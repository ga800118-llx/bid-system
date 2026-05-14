package com.bid.system.controller;

import com.bid.system.config.RequirePermission;
import com.bid.system.dto.ApiResponse;
import com.bid.system.entity.DictItem;
import com.bid.system.entity.DictType;
import com.bid.system.service.DictService;
import com.bid.system.service.ExcelExportService;
import com.bid.system.service.ExcelImportService;
import com.bid.system.service.OperationLogService;
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
@RequestMapping("/api/system/dicts")
public class DictController {

    private final DictService dictService;
    private final OperationLogService operationLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public DictController(DictService dictService,
                          OperationLogService operationLogService,
                          ExcelExportService excelExportService,
                          ExcelImportService excelImportService) {
        this.dictService = dictService;
        this.operationLogService = operationLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    @GetMapping("/types")
    @RequirePermission("system_dict:read")
    public ApiResponse listTypes(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 @RequestParam(value = "status", required = false) Integer status) {
        try {
            return ApiResponse.success(dictService.listTypes(page, size, keyword, status));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/types/export")
    @RequirePermission("system_dict:export")
    public ResponseEntity<byte[]> exportTypes(@RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "status", required = false) Integer status,
                                              HttpServletRequest request) {
        try {
            List<DictType> records = dictService.exportTypes(keyword, status);
            byte[] content = excelExportService.export("字典类型", typeHeaders(), typeRows(records));
            String fileName = "字典类型_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "DICT", "EXPORT", "导出字典类型");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "EXPORT", "导出字典类型失败", false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/types/template")
    @RequirePermission("system_dict:import")
    public ResponseEntity<byte[]> downloadTypeTemplate() {
        byte[] content = excelImportService.template("字典类型导入模板", typeImportHeaders(), typeImportSampleRows());
        String fileName = "字典类型导入模板.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping("/types/import")
    @RequirePermission("system_dict:import")
    public ApiResponse importTypes(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            Map<String, Object> result = dictService.importTypes(excelImportService.readRows(file, typeImportHeaders().size()));
            operationLogService.record(request, "DICT", "IMPORT", "导入字典类型：" + importSummary(result));
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "IMPORT", "导入字典类型失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/types")
    @RequirePermission("system_dict:create")
    public ApiResponse addType(@RequestBody Map<String, Object> map, HttpServletRequest request) {
        String typeCode = stringValue(map.get("typeCode"));
        try {
            DictType type = dictService.addType(typeCode, stringValue(map.get("typeName")), stringValue(map.get("description")),
                    intValue(map.get("sortOrder"), 0), intValue(map.get("status"), 1));
            operationLogService.record(request, "DICT", "CREATE_TYPE", "新增字典类型：" + type.getTypeCode());
            return ApiResponse.success(type);
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "CREATE_TYPE", "新增字典类型失败：" + safeLog(typeCode), false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/types/{id}")
    @RequirePermission("system_dict:update")
    public ApiResponse updateType(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        String logKey = typeLogKey(id);
        try {
            dictService.updateType(id,
                    map.containsKey("typeCode") ? stringValue(map.get("typeCode")) : null,
                    map.containsKey("typeName") ? stringValue(map.get("typeName")) : null,
                    map.containsKey("description") ? stringValue(map.get("description")) : null,
                    map.containsKey("sortOrder") ? intValue(map.get("sortOrder"), null) : null,
                    map.containsKey("status") ? intValue(map.get("status"), null) : null);
            operationLogService.record(request, "DICT", "UPDATE_TYPE", "编辑字典类型：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "UPDATE_TYPE", "编辑字典类型失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/types/{id}/status")
    @RequirePermission("system_dict:update")
    public ApiResponse updateTypeStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        String logKey = typeLogKey(id);
        try {
            Integer status = intValue(map.get("status"), null);
            if (status == null) throw new RuntimeException("状态不能为空");
            dictService.updateType(id, null, null, null, null, status);
            operationLogService.record(request, "DICT", status == 1 ? "ENABLE_TYPE" : "DISABLE_TYPE",
                    (status == 1 ? "启用" : "禁用") + "字典类型：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "STATUS_TYPE", "更新字典类型状态失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/types/{id}")
    @RequirePermission("system_dict:update")
    public ApiResponse deleteType(@PathVariable("id") Long id, HttpServletRequest request) {
        String logKey = typeLogKey(id);
        try {
            dictService.disableType(id);
            operationLogService.record(request, "DICT", "DISABLE_TYPE", "禁用字典类型：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "DISABLE_TYPE", "禁用字典类型失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/types/{typeId}/items")
    @RequirePermission("system_dict:read")
    public ApiResponse listItems(@PathVariable("typeId") Long typeId,
                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size,
                                 @RequestParam(value = "keyword", required = false) String keyword,
                                 @RequestParam(value = "status", required = false) Integer status) {
        try {
            return ApiResponse.success(dictService.listItems(typeId, page, size, keyword, status));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/types/{typeId}/items/export")
    @RequirePermission("system_dict:export")
    public ResponseEntity<byte[]> exportItems(@PathVariable("typeId") Long typeId,
                                              @RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "status", required = false) Integer status,
                                              HttpServletRequest request) {
        String typeKey = typeLogKey(typeId);
        try {
            List<DictItem> records = dictService.exportItems(typeId, keyword, status);
            byte[] content = excelExportService.export("字典项", itemHeaders(), itemRows(records));
            String fileName = typeKey + "_字典项_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "DICT", "EXPORT", "导出字典项：" + typeKey);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "EXPORT", "导出字典项失败：" + typeKey, false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/types/{typeId}/items/template")
    @RequirePermission("system_dict:import")
    public ResponseEntity<byte[]> downloadItemTemplate(@PathVariable("typeId") Long typeId) {
        String typeKey = typeLogKey(typeId);
        byte[] content = excelImportService.template("字典项导入模板", itemImportHeaders(), itemImportSampleRows());
        String fileName = typeKey + "_字典项导入模板.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping("/types/{typeId}/items/import")
    @RequirePermission("system_dict:import")
    public ApiResponse importItems(@PathVariable("typeId") Long typeId,
                                   @RequestParam("file") MultipartFile file,
                                   HttpServletRequest request) {
        String typeKey = typeLogKey(typeId);
        try {
            Map<String, Object> result = dictService.importItems(typeId, excelImportService.readRows(file, itemImportHeaders().size()));
            operationLogService.record(request, "DICT", "IMPORT", "导入字典项：" + typeKey + "，" + importSummary(result));
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "IMPORT", "导入字典项失败：" + typeKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/types/{id}/disable-impact")
    @RequirePermission("system_dict:read")
    public ApiResponse typeDisableImpact(@PathVariable("id") Long id) {
        try {
            return ApiResponse.success(dictService.typeDisableImpact(id));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/types/{typeId}/items")
    @RequirePermission("system_dict:create")
    public ApiResponse addItem(@PathVariable("typeId") Long typeId, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        String itemValue = stringValue(map.get("itemValue"));
        String typeKey = typeLogKey(typeId);
        try {
            DictItem item = dictService.addItem(typeId, stringValue(map.get("itemLabel")), itemValue, stringValue(map.get("tagColor")),
                    intValue(map.get("sortOrder"), 0), intValue(map.get("status"), 1), intValue(map.get("isDefault"), 0),
                    stringValue(map.get("description")));
            operationLogService.record(request, "DICT", "CREATE_ITEM", "新增字典项：" + typeKey + "/" + item.getItemValue());
            return ApiResponse.success(item);
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "CREATE_ITEM", "新增字典项失败：" + typeKey + "/" + safeLog(itemValue), false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/items/{id}")
    @RequirePermission("system_dict:update")
    public ApiResponse updateItem(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        String logKey = itemLogKey(id);
        try {
            dictService.updateItem(id,
                    map.containsKey("itemLabel") ? stringValue(map.get("itemLabel")) : null,
                    map.containsKey("itemValue") ? stringValue(map.get("itemValue")) : null,
                    map.containsKey("tagColor") ? stringValue(map.get("tagColor")) : null,
                    map.containsKey("sortOrder") ? intValue(map.get("sortOrder"), null) : null,
                    map.containsKey("status") ? intValue(map.get("status"), null) : null,
                    map.containsKey("isDefault") ? intValue(map.get("isDefault"), null) : null,
                    map.containsKey("description") ? stringValue(map.get("description")) : null);
            operationLogService.record(request, "DICT", "UPDATE_ITEM", "编辑字典项：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "UPDATE_ITEM", "编辑字典项失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/items/{id}/disable-impact")
    @RequirePermission("system_dict:read")
    public ApiResponse itemDisableImpact(@PathVariable("id") Long id) {
        try {
            return ApiResponse.success(dictService.itemDisableImpact(id));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/items/{id}/status")
    @RequirePermission("system_dict:update")
    public ApiResponse updateItemStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        String logKey = itemLogKey(id);
        try {
            Integer status = intValue(map.get("status"), null);
            if (status == null) throw new RuntimeException("状态不能为空");
            dictService.updateItem(id, null, null, null, null, status, null, null);
            operationLogService.record(request, "DICT", status == 1 ? "ENABLE_ITEM" : "DISABLE_ITEM",
                    (status == 1 ? "启用" : "禁用") + "字典项：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "STATUS_ITEM", "更新字典项状态失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/items/{id}")
    @RequirePermission("system_dict:update")
    public ApiResponse deleteItem(@PathVariable("id") Long id, HttpServletRequest request) {
        String logKey = itemLogKey(id);
        try {
            dictService.disableItem(id);
            operationLogService.record(request, "DICT", "DISABLE_ITEM", "禁用字典项：" + logKey);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "DICT", "DISABLE_ITEM", "禁用字典项失败：" + logKey, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/public/{typeCode}")
    public ApiResponse publicItems(@PathVariable("typeCode") String typeCode) {
        return ApiResponse.success(dictService.publicItems(typeCode));
    }

    private String typeLogKey(Long id) {
        DictType type = id == null ? null : dictService.getById(id);
        return type == null || type.getTypeCode() == null ? String.valueOf(id) : type.getTypeCode();
    }

    private String itemLogKey(Long id) {
        DictItem item = id == null ? null : dictService.getItemById(id);
        if (item == null) return String.valueOf(id);
        return typeLogKey(item.getTypeId()) + "/" + item.getItemValue();
    }

    private String safeLog(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer intValue(Object value, Integer defaultValue) {
        if (value == null || String.valueOf(value).isBlank()) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private List<String> typeHeaders() {
        return List.of("类型编码", "类型名称", "状态", "排序", "说明", "更新时间");
    }

    private List<String> typeImportHeaders() {
        return List.of("类型编码", "类型名称", "状态(启用/禁用)", "排序", "说明");
    }

    private List<List<String>> typeImportSampleRows() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("project_status", "项目状态", "启用", "10", "用于项目状态下拉"));
        rows.add(List.of("supplier_level", "供应商等级", "启用", "20", "用于供应商等级标签"));
        return rows;
    }

    private List<List<String>> typeRows(List<DictType> records) {
        List<List<String>> rows = new ArrayList<>();
        for (DictType record : records) {
            rows.add(List.of(
                    safeText(record.getTypeCode()),
                    safeText(record.getTypeName()),
                    Integer.valueOf(1).equals(record.getStatus()) ? "启用" : "禁用",
                    String.valueOf(record.getSortOrder() == null ? 0 : record.getSortOrder()),
                    safeText(record.getDescription()),
                    formatDateTime(record.getUpdatedAt())
            ));
        }
        return rows;
    }

    private List<String> itemHeaders() {
        return List.of("标签", "值", "标签颜色", "状态", "默认项", "排序", "说明", "更新时间");
    }

    private List<String> itemImportHeaders() {
        return List.of("标签", "值", "标签颜色", "状态(启用/禁用)", "默认项(是/否)", "排序", "说明");
    }

    private List<List<String>> itemImportSampleRows() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("草稿", "draft", "gray", "启用", "是", "10", "初始状态"));
        rows.add(List.of("进行中", "processing", "arcoblue", "启用", "否", "20", "处理中状态"));
        return rows;
    }

    private List<List<String>> itemRows(List<DictItem> records) {
        List<List<String>> rows = new ArrayList<>();
        for (DictItem record : records) {
            rows.add(List.of(
                    safeText(record.getItemLabel()),
                    safeText(record.getItemValue()),
                    safeText(record.getTagColor()),
                    Integer.valueOf(1).equals(record.getStatus()) ? "启用" : "禁用",
                    Integer.valueOf(1).equals(record.getIsDefault()) ? "是" : "否",
                    String.valueOf(record.getSortOrder() == null ? 0 : record.getSortOrder()),
                    safeText(record.getDescription()),
                    formatDateTime(record.getUpdatedAt())
            ));
        }
        return rows;
    }

    private String formatDateTime(Object value) {
        String text = safeText(value);
        return text.equals("-") ? text : text.replace('T', ' ');
    }

    private String safeText(Object value) {
        if (value == null) return "-";
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "-" : text;
    }

    private String importSummary(Map<String, Object> result) {
        return "成功 " + safeText(result.get("successCount"))
                + "，跳过 " + safeText(result.get("skippedCount"))
                + "，失败 " + safeText(result.get("failureCount"));
    }
}
