package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.config.RequirePermission;
import com.bid.system.entity.SystemConfig;
import com.bid.system.service.ExcelExportService;
import com.bid.system.service.ExcelImportService;
import com.bid.system.service.OperationLogService;
import com.bid.system.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/configs")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;
    private final OperationLogService operationLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public SystemConfigController(SystemConfigService systemConfigService,
                                  OperationLogService operationLogService,
                                  ExcelExportService excelExportService,
                                  ExcelImportService excelImportService) {
        this.systemConfigService = systemConfigService;
        this.operationLogService = operationLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    @GetMapping
    @RequirePermission("system_config:read")
    public ApiResponse list(@RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "20") int size,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "groupCode", required = false) String groupCode,
                            @RequestParam(value = "status", required = false) Integer status,
                            @RequestParam(value = "builtIn", required = false) Boolean builtIn) {
        try {
            return ApiResponse.success(systemConfigService.listConfigs(page, size, keyword, groupCode, status, builtIn));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/export")
    @RequirePermission("system_config:export")
    public ResponseEntity<byte[]> export(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "groupCode", required = false) String groupCode,
                                         @RequestParam(value = "status", required = false) Integer status,
                                         @RequestParam(value = "builtIn", required = false) Boolean builtIn,
                                         HttpServletRequest request) {
        try {
            List<SystemConfig> records = systemConfigService.exportConfigs(keyword, groupCode, status, builtIn);
            byte[] content = excelExportService.export("系统配置", buildExportHeaders(), buildExportRows(records));
            String fileName = "系统配置_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "CONFIG", "EXPORT", "导出系统配置");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (RuntimeException e) {
            operationLogService.record(request, "CONFIG", "EXPORT", "导出系统配置失败", false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/template")
    @RequirePermission("system_config:import")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] content = excelImportService.template("系统配置导入模板", buildImportHeaders(), buildImportSampleRows());
        String fileName = "系统配置导入模板.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping("/import")
    @RequirePermission("system_config:import")
    public ApiResponse importConfigs(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            Map<String, Object> result = systemConfigService.importConfigs(excelImportService.readRows(file, buildImportHeaders().size()));
            operationLogService.record(request, "CONFIG", "IMPORT", "导入系统配置：" + importSummary(result));
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            operationLogService.record(request, "CONFIG", "IMPORT", "导入系统配置失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/public/basic")
    public ApiResponse publicBasic() {
        return ApiResponse.success(systemConfigService.publicBasic());
    }

    @PostMapping
    @RequirePermission("system_config:update")
    public ApiResponse add(@RequestBody Map<String, Object> map, HttpServletRequest request) {
        try {
            String configKey = stringValue(map.get("configKey"));
            String configValue = stringValue(map.get("configValue"));
            String configName = stringValue(map.get("configName"));
            String description = stringValue(map.get("description"));
            String valueType = stringValue(map.get("valueType"));
            String groupCode = stringValue(map.get("groupCode"));
            Integer status = intValue(map.get("status"), 1);
            systemConfigService.addConfig(configKey, configValue, configName, description, valueType, groupCode, status);
            operationLogService.record(request, "CONFIG", "CREATE", "新增系统配置：" + configKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "CONFIG", "CREATE", "新增系统配置失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequirePermission("system_config:update")
    public ApiResponse update(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        String logKey = configLogKey(id);
        try {
            String configKey = map.containsKey("configKey") ? stringValue(map.get("configKey")) : null;
            String configValue = map.containsKey("configValue") ? stringValue(map.get("configValue")) : null;
            String configName = map.containsKey("configName") ? stringValue(map.get("configName")) : null;
            String description = map.containsKey("description") ? stringValue(map.get("description")) : null;
            String valueType = map.containsKey("valueType") ? stringValue(map.get("valueType")) : null;
            String groupCode = map.containsKey("groupCode") ? stringValue(map.get("groupCode")) : null;
            Integer status = map.containsKey("status") ? intValue(map.get("status"), null) : null;
            systemConfigService.updateConfig(id, configKey, configValue, configName, description, valueType, groupCode, status);
            operationLogService.record(request, "CONFIG", "UPDATE", "编辑系统配置：" + (configKey != null && !configKey.isBlank() ? configKey : logKey));
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "CONFIG", "UPDATE", "编辑系统配置失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @RequirePermission("system_config:update")
    public ApiResponse updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        String logKey = configLogKey(id);
        try {
            Integer status = intValue(map.get("status"), null);
            if (status == null) throw new RuntimeException("状态不能为空");
            systemConfigService.updateConfig(id, null, null, null, null, null, null, status);
            String action = status != null && status == 1 ? "ENABLE" : "DISABLE";
            operationLogService.record(request, "CONFIG", action, (status != null && status == 1 ? "启用" : "禁用") + "系统配置：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "CONFIG", "STATUS", "更新系统配置状态失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system_config:update")
    public ApiResponse delete(@PathVariable("id") Long id, HttpServletRequest request) {
        String logKey = configLogKey(id);
        try {
            systemConfigService.disableConfig(id);
            operationLogService.record(request, "CONFIG", "DISABLE", "禁用系统配置：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "CONFIG", "DISABLE", "禁用系统配置失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private String configLogKey(Long id) {
        if (id == null) return "";
        SystemConfig config = systemConfigService.getById(id);
        if (config == null || config.getConfigKey() == null || config.getConfigKey().isBlank()) return String.valueOf(id);
        return config.getConfigKey();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private List<String> buildExportHeaders() {
        return List.of("配置键", "名称", "配置值", "分组", "类型", "状态", "说明", "更新时间");
    }

    private List<String> buildImportHeaders() {
        return List.of("配置键", "名称", "配置值", "分组", "类型(TEXT/NUMBER/BOOLEAN/JSON)", "状态(启用/禁用)", "说明");
    }

    private List<List<String>> buildImportSampleRows() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("project.default_owner", "项目默认负责人", "admin@bid.com", "basic", "TEXT", "启用", "用于项目默认负责人演示"));
        rows.add(List.of("report.export.max_rows", "导出最大行数", "5000", "security", "NUMBER", "启用", "控制单次导出上限"));
        return rows;
    }

    private List<List<String>> buildExportRows(List<SystemConfig> records) {
        List<List<String>> rows = new ArrayList<>();
        for (SystemConfig record : records) {
            rows.add(List.of(
                    stringValue(record.getConfigKey()),
                    stringValue(record.getConfigName()),
                    stringValue(record.getConfigValue()),
                    stringValue(record.getGroupCode()),
                    stringValue(record.getValueType()),
                    Integer.valueOf(1).equals(record.getStatus()) ? "启用" : "禁用",
                    stringValue(record.getDescription()),
                    formatDateTime(record.getUpdatedAt())
            ));
        }
        return rows;
    }

    private String formatDateTime(Object value) {
        String text = stringValue(value);
        return text == null || text.isBlank() ? "-" : text.replace('T', ' ');
    }

    private String importSummary(Map<String, Object> result) {
        return "成功 " + stringValue(result.get("successCount"))
                + "，跳过 " + stringValue(result.get("skippedCount"))
                + "，失败 " + stringValue(result.get("failureCount"));
    }
}
