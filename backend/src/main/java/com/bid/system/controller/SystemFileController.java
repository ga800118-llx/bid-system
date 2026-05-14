package com.bid.system.controller;

import com.bid.system.config.RequirePermission;
import com.bid.system.dto.ApiResponse;
import com.bid.system.entity.SystemFile;
import com.bid.system.entity.SystemFileLink;
import com.bid.system.service.ExcelExportService;
import com.bid.system.service.OperationLogService;
import com.bid.system.service.SystemFileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system/files")
public class SystemFileController {

    private final SystemFileService systemFileService;
    private final OperationLogService operationLogService;
    private final ExcelExportService excelExportService;

    public SystemFileController(SystemFileService systemFileService,
                                OperationLogService operationLogService,
                                ExcelExportService excelExportService) {
        this.systemFileService = systemFileService;
        this.operationLogService = operationLogService;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    @RequirePermission("system_file:read")
    public ApiResponse list(@RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "20") int size,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "extension", required = false) String extension,
                            @RequestParam(value = "status", required = false) Integer status,
                            @RequestParam(value = "uploaderId", required = false) Long uploaderId,
                            @RequestParam(value = "dateFrom", required = false) String dateFrom,
                            @RequestParam(value = "dateTo", required = false) String dateTo,
                            @RequestParam(value = "previewable", required = false) Boolean previewable) {
        try {
            return ApiResponse.success(systemFileService.listFiles(page, size, keyword, extension, status, uploaderId, dateFrom, dateTo, previewable));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/export")
    @RequirePermission("system_file:export")
    public ResponseEntity<byte[]> export(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "extension", required = false) String extension,
                                         @RequestParam(value = "status", required = false) Integer status,
                                         @RequestParam(value = "uploaderId", required = false) Long uploaderId,
                                         @RequestParam(value = "dateFrom", required = false) String dateFrom,
                                         @RequestParam(value = "dateTo", required = false) String dateTo,
                                         @RequestParam(value = "previewable", required = false) Boolean previewable,
                                         HttpServletRequest request) {
        try {
            List<Map<String, Object>> records = systemFileService.exportFiles(keyword, extension, status, uploaderId, dateFrom, dateTo, previewable);
            byte[] content = excelExportService.export("文件列表", exportHeaders(), exportRows(records));
            String fileName = "文件列表_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "FILE", "EXPORT", "导出文件列表");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (RuntimeException e) {
            operationLogService.record(request, "FILE", "EXPORT", "导出文件列表失败", false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @RequirePermission("system_file:read")
    public ApiResponse detail(@PathVariable("id") Long id) {
        try {
            return ApiResponse.success(systemFileService.getFileDetail(id));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/rules")
    @RequirePermission("system_file:read")
    public ApiResponse rules() {
        try {
            return ApiResponse.success(systemFileService.getRules());
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/upload")
    @RequirePermission("system_file:upload")
    public ApiResponse upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String originalName = file == null ? "-" : String.valueOf(file.getOriginalFilename());
        try {
            Long userId = longValue(request.getAttribute("userId"));
            SystemFile saved = systemFileService.uploadFile(file, userId);
            operationLogService.record(request, "FILE", "UPLOAD", "上传文件：" + saved.getOriginalName());
            return ApiResponse.success(saved);
        } catch (RuntimeException e) {
            operationLogService.record(request, "FILE", "UPLOAD", "上传文件失败：" + originalName, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    @RequirePermission("system_file:download")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            SystemFile file = systemFileService.requireFile(id);
            Path path = systemFileService.resolveDownloadPath(id);
            operationLogService.record(request, "FILE", "DOWNLOAD", "下载文件：" + file.getOriginalName());
            Resource resource = new FileSystemResource(path);
            String fileName = file.getOriginalName() == null || file.getOriginalName().isBlank() ? path.getFileName().toString() : file.getOriginalName();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (RuntimeException e) {
            operationLogService.record(request, "FILE", "DOWNLOAD", "下载文件失败：" + id, false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/preview")
    @RequirePermission("system_file:download")
    public ResponseEntity<Resource> preview(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            SystemFile file = systemFileService.requireFile(id);
            systemFileService.assertPreviewable(id);
            Path path = systemFileService.resolveDownloadPath(id);
            operationLogService.record(request, "FILE", "PREVIEW", "预览文件：" + file.getOriginalName());
            Resource resource = new FileSystemResource(path);
            String fileName = file.getOriginalName() == null || file.getOriginalName().isBlank() ? path.getFileName().toString() : file.getOriginalName();
            MediaType mediaType = resolveMediaType(file, resource);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(mediaType)
                    .body(resource);
        } catch (RuntimeException e) {
            operationLogService.record(request, "FILE", "PREVIEW", "预览文件失败：" + id, false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/status")
    @RequirePermission("system_file:delete")
    public ApiResponse updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        String logKey = fileLogKey(id);
        try {
            Integer status = intValue(map.get("status"), null);
            if (status == null) throw new RuntimeException("状态不能为空");
            systemFileService.updateFileStatus(id, status);
            operationLogService.record(request, "FILE", "STATUS", (status == 1 ? "启用" : "禁用") + "文件：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "FILE", "STATUS", "更新文件状态失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system_file:delete")
    public ApiResponse delete(@PathVariable("id") Long id, HttpServletRequest request) {
        String logKey = fileLogKey(id);
        try {
            systemFileService.disableFile(id);
            operationLogService.record(request, "FILE", "DELETE", "删除文件：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "FILE", "DELETE", "删除文件失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/links")
    @RequirePermission("system_file:read")
    public ApiResponse links(@RequestParam("bizType") String bizType, @RequestParam("bizId") Long bizId) {
        try {
            return ApiResponse.success(systemFileService.listLinks(bizType, bizId));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/links")
    @RequirePermission("system_file:link")
    public ApiResponse addLink(@RequestBody Map<String, Object> map, HttpServletRequest request) {
        try {
            Long fileId = longValue(map.get("fileId"));
            String bizType = stringValue(map.get("bizType"));
            Long bizId = longValue(map.get("bizId"));
            String categoryCode = stringValue(map.get("categoryCode"));
            Integer sortOrder = intValue(map.get("sortOrder"), 0);
            Long createdBy = longValue(request.getAttribute("userId"));
            SystemFileLink link = systemFileService.addLink(fileId, bizType, bizId, categoryCode, sortOrder, createdBy);
            operationLogService.record(request, "FILE", "LINK", "关联文件：" + fileLogKey(fileId) + " -> " + bizType + "/" + bizId);
            return ApiResponse.success(link);
        } catch (RuntimeException e) {
            operationLogService.record(request, "FILE", "LINK", "关联文件失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/links/{id}")
    @RequirePermission("system_file:link")
    public ApiResponse removeLink(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            systemFileService.removeLink(id);
            operationLogService.record(request, "FILE", "UNLINK", "解除文件关联：" + id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "FILE", "UNLINK", "解除文件关联失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private String fileLogKey(Long id) {
        if (id == null) return "-";
        SystemFile file = systemFileService.getById(id);
        if (file == null || file.getOriginalName() == null || file.getOriginalName().isBlank()) return String.valueOf(id);
        return file.getOriginalName();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private MediaType resolveMediaType(SystemFile file, Resource resource) {
        String contentType = file == null ? null : file.getContentType();
        if (contentType != null && !contentType.isBlank() && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(contentType)) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private List<String> exportHeaders() {
        return List.of("文件名", "扩展名", "文件大小", "上传人", "状态", "支持预览", "对象键", "上传时间");
    }

    private List<List<String>> exportRows(List<Map<String, Object>> records) {
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> record : records) {
            Map<String, Object> item = new LinkedHashMap<>(record);
            rows.add(List.of(
                    safeText(item.get("originalName")),
                    safeText(item.get("extension")),
                    safeText(item.get("fileSize")),
                    safeText(item.get("uploaderName")),
                    Integer.valueOf(1).equals(item.get("status")) ? "启用" : "禁用",
                    Boolean.TRUE.equals(item.get("previewable")) ? "是" : "否",
                    safeText(item.get("objectKey")),
                    formatDateTime(item.get("createdAt"))
            ));
        }
        return rows;
    }

    private String safeText(Object value) {
        if (value == null) return "-";
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "-" : text;
    }

    private String formatDateTime(Object value) {
        String text = safeText(value);
        return "-".equals(text) ? text : text.replace('T', ' ');
    }
}
