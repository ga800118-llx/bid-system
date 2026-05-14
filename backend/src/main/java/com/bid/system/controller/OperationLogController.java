package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.config.RequirePermission;
import com.bid.system.service.ExcelExportService;
import com.bid.system.service.OperationLogService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;
    private final ExcelExportService excelExportService;

    public OperationLogController(OperationLogService operationLogService, ExcelExportService excelExportService) {
        this.operationLogService = operationLogService;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    @RequirePermission("system_log:read")
    public ApiResponse list(@RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "20") int size,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "module", required = false) String module,
                            @RequestParam(value = "action", required = false) String action,
                            @RequestParam(value = "success", required = false) Integer success,
                            @RequestParam(value = "startDate", required = false) String startDate,
                            @RequestParam(value = "endDate", required = false) String endDate,
                            HttpServletRequest request) {
        try {
            String username = request.getAttribute("username") == null ? null : String.valueOf(request.getAttribute("username"));
            return ApiResponse.success(operationLogService.listLogs(page, size, keyword, module, action, success, startDate, endDate, username));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/export")
    @RequirePermission("system_log:export")
    public ResponseEntity<byte[]> export(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "module", required = false) String module,
                                         @RequestParam(value = "action", required = false) String action,
                                         @RequestParam(value = "success", required = false) Integer success,
                                         @RequestParam(value = "startDate", required = false) String startDate,
                                         @RequestParam(value = "endDate", required = false) String endDate,
                                         HttpServletRequest request) {
        try {
            String username = request.getAttribute("username") == null ? null : String.valueOf(request.getAttribute("username"));
            Map<String, Object> exportData = operationLogService.exportLogs(keyword, module, action, success, startDate, endDate, username);
            byte[] content = excelExportService.export("操作日志", buildExportHeaders(exportData), buildExportRows(exportData));
            String fileName = "操作日志_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "LOG", "EXPORT", "导出操作日志");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (RuntimeException e) {
            operationLogService.record(request, "LOG", "EXPORT", "导出操作日志失败", false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private List<String> buildExportHeaders(Map<String, Object> exportData) {
        List<String> headers = new ArrayList<>();
        List<String> readableFields = castStringList(exportData.get("readableFields"));
        headers.add("时间");
        if (readableFields.contains("operator")) headers.add("操作人");
        headers.add("模块");
        headers.add("动作");
        headers.add("结果");
        if (readableFields.contains("content")) headers.add("内容");
        if (readableFields.contains("request")) headers.add("请求");
        if (readableFields.contains("ip")) headers.add("IP");
        if (readableFields.contains("content")) headers.add("错误信息");
        return headers;
    }

    private List<List<String>> buildExportRows(Map<String, Object> exportData) {
        List<List<String>> rows = new ArrayList<>();
        List<String> readableFields = castStringList(exportData.get("readableFields"));
        List<?> records = exportData.get("records") instanceof List<?> list ? list : List.of();
        for (Object item : records) {
            if (!(item instanceof Map<?, ?> rawRecord)) continue;
            Map<String, Object> record = new LinkedHashMap<>();
            rawRecord.forEach((key, value) -> record.put(String.valueOf(key), value));
            List<String> row = new ArrayList<>();
            row.add(formatDateTime(record.get("createdAt")));
            if (readableFields.contains("operator")) row.add(stringValue(record.get("operatorUsername")));
            row.add(stringValue(record.get("module")));
            row.add(stringValue(record.get("action")));
            row.add(Integer.valueOf(1).equals(record.get("success")) ? "成功" : "失败");
            if (readableFields.contains("content")) row.add(stringValue(record.get("content")));
            if (readableFields.contains("request")) row.add((stringValue(record.get("requestMethod"), "-") + " " + stringValue(record.get("requestPath"), "")).trim());
            if (readableFields.contains("ip")) row.add(stringValue(record.get("ip")));
            if (readableFields.contains("content")) row.add(stringValue(record.get("errorMessage")));
            rows.add(row);
        }
        return rows;
    }

    private List<String> castStringList(Object value) {
        if (!(value instanceof List<?> raw)) return List.of();
        return raw.stream().map(String::valueOf).toList();
    }

    private String formatDateTime(Object value) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? "-" : text.replace('T', ' ');
    }

    private String stringValue(Object value) {
        return stringValue(value, "-");
    }

    private String stringValue(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }
}
