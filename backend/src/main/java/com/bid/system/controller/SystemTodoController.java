package com.bid.system.controller;

import com.bid.system.config.RequirePermission;
import com.bid.system.dto.ApiResponse;
import com.bid.system.service.ExcelExportService;
import com.bid.system.service.OperationLogService;
import com.bid.system.service.SystemTodoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/todos")
public class SystemTodoController {

    private final SystemTodoService systemTodoService;
    private final OperationLogService operationLogService;
    private final ExcelExportService excelExportService;

    public SystemTodoController(SystemTodoService systemTodoService,
                                OperationLogService operationLogService,
                                ExcelExportService excelExportService) {
        this.systemTodoService = systemTodoService;
        this.operationLogService = operationLogService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/my")
    @RequirePermission("system_todo:read")
    public ApiResponse myTodos(@RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "20") int size,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "priority", required = false) String priority,
                               @RequestParam(value = "todoType", required = false) String todoType,
                               @RequestParam(value = "overdueOnly", required = false) Integer overdueOnly,
                               @RequestParam(value = "dateFrom", required = false) String dateFrom,
                               @RequestParam(value = "dateTo", required = false) String dateTo,
                               HttpServletRequest request) {
        try {
            return ApiResponse.success(systemTodoService.listMyTodos(page, size, keyword, status, priority, todoType, overdueOnly, dateFrom, dateTo, longValue(request.getAttribute("userId"))));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @RequirePermission("system_todo:read")
    public ApiResponse detail(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            return ApiResponse.success(systemTodoService.getMyTodoDetail(id, longValue(request.getAttribute("userId"))));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/export")
    @RequirePermission("system_todo:export")
    public ResponseEntity<byte[]> export(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "status", required = false) String status,
                                         @RequestParam(value = "priority", required = false) String priority,
                                         @RequestParam(value = "todoType", required = false) String todoType,
                                         @RequestParam(value = "overdueOnly", required = false) Integer overdueOnly,
                                         @RequestParam(value = "dateFrom", required = false) String dateFrom,
                                         @RequestParam(value = "dateTo", required = false) String dateTo,
                                         HttpServletRequest request) {
        try {
            List<Map<String, Object>> records = systemTodoService.exportMyTodos(keyword, status, priority, todoType, overdueOnly, dateFrom, dateTo, longValue(request.getAttribute("userId")));
            byte[] content = excelExportService.export("我的待办", exportHeaders(), exportRows(records));
            String fileName = "我的待办_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "TODO", "EXPORT", "导出我的待办");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (RuntimeException e) {
            operationLogService.record(request, "TODO", "EXPORT", "导出我的待办失败", false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @RequirePermission("system_todo:create")
    public ApiResponse create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Map<String, Object> result = systemTodoService.createTodo(
                    stringValue(body.get("title")),
                    stringValue(body.get("content")),
                    stringValue(body.get("todoType")),
                    stringValue(body.get("priority")),
                    longList(body.get("assigneeIds"), body.get("assigneeId")),
                    stringValue(body.get("dueAt")),
                    stringValue(body.get("bizType")),
                    longValue(body.get("bizId")),
                    longList(body.get("attachmentFileIds"), null),
                    stringValue(body.get("contentType")),
                    longValue(request.getAttribute("userId")),
                    stringValue(request.getAttribute("realName")) == null || stringValue(request.getAttribute("realName")).isBlank()
                            ? stringValue(request.getAttribute("username"))
                            : stringValue(request.getAttribute("realName"))
            );
            operationLogService.record(request, "TODO", "CREATE", "新增待办：" + stringValue(body.get("title")));
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            operationLogService.record(request, "TODO", "CREATE", "新增待办失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @RequirePermission("system_todo:read")
    public ApiResponse updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            String status = stringValue(body.get("status"));
            systemTodoService.updateMyTodoStatus(id, status, longValue(request.getAttribute("userId")));
            operationLogService.record(request, "TODO", "STATUS", "更新待办状态：" + id + " -> " + status);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "TODO", "STATUS", "更新待办状态失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private List<Long> longList(Object value, Object fallback) {
        List<Long> result = new ArrayList<>();
        appendLongs(result, value);
        if (result.isEmpty()) appendLongs(result, fallback);
        return result;
    }

    private void appendLongs(List<Long> result, Object value) {
        if (value == null) return;
        if (value instanceof List<?> list) {
            for (Object item : list) {
                Long parsed = longValue(item);
                if (parsed != null) result.add(parsed);
            }
            return;
        }
        Long parsed = longValue(value);
        if (parsed != null) result.add(parsed);
    }

    private List<String> exportHeaders() {
        return List.of("标题", "类型", "优先级", "状态", "处理人", "创建人", "截止时间", "完成时间", "创建时间");
    }

    private List<List<String>> exportRows(List<Map<String, Object>> records) {
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> record : records) {
            rows.add(List.of(
                    safeText(record.get("title")),
                    safeText(record.get("todoType")),
                    safeText(record.get("priority")),
                    statusLabel(record.get("status")),
                    safeText(record.get("assigneeName")),
                    safeText(record.get("creatorName")),
                    formatDateTime(record.get("dueAt")),
                    formatDateTime(record.get("processedAt")),
                    formatDateTime(record.get("createdAt"))
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

    private String statusLabel(Object value) {
        String text = safeText(value);
        return switch (text) {
            case "PENDING" -> "待处理";
            case "PROCESSING" -> "处理中";
            case "DONE" -> "已完成";
            default -> text;
        };
    }
}
