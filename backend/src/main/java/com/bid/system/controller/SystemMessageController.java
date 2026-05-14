package com.bid.system.controller;

import com.bid.system.config.RequirePermission;
import com.bid.system.dto.ApiResponse;
import com.bid.system.service.ExcelExportService;
import com.bid.system.service.OperationLogService;
import com.bid.system.service.SystemMessageService;
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
@RequestMapping("/api/system/messages")
public class SystemMessageController {

    private final SystemMessageService systemMessageService;
    private final OperationLogService operationLogService;
    private final ExcelExportService excelExportService;

    public SystemMessageController(SystemMessageService systemMessageService,
                                   OperationLogService operationLogService,
                                   ExcelExportService excelExportService) {
        this.systemMessageService = systemMessageService;
        this.operationLogService = operationLogService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/my")
    public ApiResponse myMessages(@RequestParam(value = "page", defaultValue = "1") int page,
                                  @RequestParam(value = "size", defaultValue = "20") int size,
                                  @RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "messageType", required = false) String messageType,
                                  @RequestParam(value = "readStatus", required = false) Integer readStatus,
                                  HttpServletRequest request) {
        try {
            return ApiResponse.success(systemMessageService.listMyMessages(page, size, keyword, messageType, readStatus, longValue(request.getAttribute("userId"))));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/export")
    @RequirePermission("system_message:export")
    public ResponseEntity<byte[]> export(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "messageType", required = false) String messageType,
                                         @RequestParam(value = "readStatus", required = false) Integer readStatus,
                                         HttpServletRequest request) {
        try {
            List<Map<String, Object>> records = systemMessageService.exportMyMessages(keyword, messageType, readStatus, longValue(request.getAttribute("userId")));
            byte[] content = excelExportService.export("站内消息", exportHeaders(), exportRows(records));
            String fileName = "站内消息_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "MESSAGE", "EXPORT", "导出站内消息");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (RuntimeException e) {
            operationLogService.record(request, "MESSAGE", "EXPORT", "导出站内消息失败", false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/unread-count")
    public ApiResponse unreadCount(HttpServletRequest request) {
        return ApiResponse.success(Map.of("count", systemMessageService.unreadCount(longValue(request.getAttribute("userId")))));
    }

    @PutMapping("/{id}/read")
    public ApiResponse markRead(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            systemMessageService.markRead(id, longValue(request.getAttribute("userId")));
            operationLogService.record(request, "MESSAGE", "READ", "阅读站内消息：" + id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "MESSAGE", "READ", "阅读站内消息失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/read-all")
    public ApiResponse markAllRead(HttpServletRequest request) {
        try {
            int count = systemMessageService.markAllRead(longValue(request.getAttribute("userId")));
            operationLogService.record(request, "MESSAGE", "READ_ALL", "全部标记已读：" + count + " 条");
            return ApiResponse.success(Map.of("count", count));
        } catch (RuntimeException e) {
            operationLogService.record(request, "MESSAGE", "READ_ALL", "全部标记已读失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            systemMessageService.deleteMessage(id, longValue(request.getAttribute("userId")));
            operationLogService.record(request, "MESSAGE", "DELETE", "删除站内消息：" + id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "MESSAGE", "DELETE", "删除站内消息失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping
    @RequirePermission("system_message:send")
    public ApiResponse send(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Map<String, Object> result = systemMessageService.sendMessage(
                    stringValue(body.get("title")),
                    stringValue(body.get("content")),
                    stringValue(body.get("messageType")),
                    stringValue(body.get("targetType")),
                    longList(body.get("userIds")),
                    longList(body.get("ccUserIds")),
                    longList(body.get("attachmentFileIds")),
                    stringValue(body.get("contentType")),
                    longValue(request.getAttribute("userId")),
                    stringValue(request.getAttribute("realName")) == null || stringValue(request.getAttribute("realName")).isBlank()
                            ? stringValue(request.getAttribute("username"))
                            : stringValue(request.getAttribute("realName")),
                    stringValue(body.get("bizType")),
                    longValue(body.get("bizId")),
                    stringValue(body.get("relatedPath"))
            );
            operationLogService.record(request, "MESSAGE", "SEND", "发送站内消息：" + stringValue(body.get("title")));
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            operationLogService.record(request, "MESSAGE", "SEND", "发送站内消息失败", false, e.getMessage());
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

    private List<Long> longList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().map(this::longValue).filter(v -> v != null).toList();
    }

    private List<String> exportHeaders() {
        return List.of("标题", "类型", "内容", "接收类型", "发送人", "状态", "阅读时间", "发送时间");
    }

    private List<List<String>> exportRows(List<Map<String, Object>> records) {
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> record : records) {
            rows.add(List.of(
                    safeText(record.get("title")),
                    safeText(record.get("messageType")),
                    safeText(record.get("contentText")),
                    "CC".equals(safeText(record.get("receiverType"))) ? "抄送" : "收件",
                    safeText(record.get("senderName")),
                    Integer.valueOf(1).equals(record.get("readStatus")) ? "已读" : "未读",
                    formatDateTime(record.get("readAt")),
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
}
