package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.DictItem;
import com.bid.system.entity.DictType;
import com.bid.system.mapper.DictItemMapper;
import com.bid.system.mapper.DictTypeMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DictService extends ServiceImpl<DictTypeMapper, DictType> {

    private static final Set<String> BUILT_IN_TYPES = Set.of(
            "common_status",
            "success_status",
            "user_role_type",
            "config_group",
            "config_value_type",
            "message_type",
            "todo_type",
            "todo_priority",
            "todo_status",
            "tag_color",
            "file_storage_type",
            "file_link_category",
            "operation_log_module",
            "operation_log_action"
    );

    private static final Map<String, Set<String>> BUILT_IN_ITEMS = Map.ofEntries(
            Map.entry("common_status", Set.of("enabled", "disabled")),
            Map.entry("success_status", Set.of("success", "failed")),
            Map.entry("user_role_type", Set.of("admin", "user")),
            Map.entry("config_group", Set.of("basic", "security", "ui")),
            Map.entry("config_value_type", Set.of("TEXT", "NUMBER", "BOOLEAN", "JSON")),
            Map.entry("message_type", Set.of("SYSTEM", "NOTICE", "TODO", "WARNING")),
            Map.entry("todo_type", Set.of("GENERAL", "SYSTEM", "FOLLOW_UP")),
            Map.entry("todo_priority", Set.of("LOW", "MEDIUM", "HIGH", "URGENT")),
            Map.entry("todo_status", Set.of("PENDING", "PROCESSING", "DONE")),
            Map.entry("tag_color", Set.of("arcoblue", "green", "orangered", "red", "gold", "purple", "gray")),
            Map.entry("file_storage_type", Set.of("LOCAL")),
            Map.entry("file_link_category", Set.of("primary", "attachment", "archive")),
            Map.entry("operation_log_module", Set.of("DEPARTMENT", "USER", "ROLE", "CONFIG", "DICT", "AUTH", "FILE", "MESSAGE", "TODO", "LOG")),
            Map.entry("operation_log_action", Set.of(
                    "CREATE", "UPDATE", "DELETE", "ENABLE", "DISABLE", "STATUS",
                    "EXPORT",
                    "IMPORT",
                    "RESET_PASSWORD", "MOVE_DEPT", "ASSIGN_ROLES", "SAVE_FEATURES",
                    "SAVE_DATA_SCOPE", "SAVE_FIELDS", "LOGIN", "LOGIN_FAILED", "ACCOUNT_LOCKED", "LOGOUT", "CREATE_TYPE",
                    "CHANGE_PASSWORD",
                    "UPDATE_TYPE", "ENABLE_TYPE", "DISABLE_TYPE", "STATUS_TYPE",
                    "CREATE_ITEM", "UPDATE_ITEM", "ENABLE_ITEM", "DISABLE_ITEM", "STATUS_ITEM",
                    "PERMISSION_DENIED", "UPLOAD", "DOWNLOAD", "PREVIEW", "LINK", "UNLINK",
                    "SEND", "READ", "READ_ALL"
            ))
    );

    private static final Map<String, List<String>> BUILT_IN_USAGE = Map.ofEntries(
            Map.entry("common_status", List.of("用户管理状态筛选与标签", "部门管理状态展示", "角色管理状态展示", "系统配置状态展示", "字典管理状态展示")),
            Map.entry("success_status", List.of("操作日志结果筛选与标签")),
            Map.entry("user_role_type", List.of("用户管理角色筛选", "部门管理用户角色展示")),
            Map.entry("config_group", List.of("系统配置分组筛选与标签")),
            Map.entry("config_value_type", List.of("系统配置值类型下拉与标签")),
            Map.entry("message_type", List.of("消息中心消息类型筛选与标签")),
            Map.entry("todo_type", List.of("待办中心待办类型筛选与标签")),
            Map.entry("todo_priority", List.of("待办中心优先级筛选与标签")),
            Map.entry("todo_status", List.of("待办中心状态筛选与标签")),
            Map.entry("tag_color", List.of("字典项标签颜色下拉")),
            Map.entry("file_storage_type", List.of("文件中心存储类型展示")),
            Map.entry("file_link_category", List.of("文件中心关联分类下拉与展示")),
            Map.entry("operation_log_module", List.of("操作日志模块筛选与标签")),
            Map.entry("operation_log_action", List.of("操作日志动作筛选与标签"))
    );

    private final DictItemMapper dictItemMapper;

    public DictService(DictItemMapper dictItemMapper) {
        this.dictItemMapper = dictItemMapper;
    }

    public Map<String, Object> listTypes(int page, int size, String keyword, Integer status) {
        List<DictType> records = queryTypes(keyword, status);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int from = Math.min((safePage - 1) * safeSize, records.size());
        int to = Math.min(from + safeSize, records.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records.subList(from, to));
        result.put("total", records.size());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public List<DictType> exportTypes(String keyword, Integer status) {
        return queryTypes(keyword, status);
    }

    private List<DictType> queryTypes(String keyword, Integer status) {
        LambdaQueryWrapper<DictType> q = new LambdaQueryWrapper<>();
        String kw = normalize(keyword);
        if (!kw.isEmpty()) {
            q.and(w -> w.like(DictType::getTypeCode, kw)
                    .or().like(DictType::getTypeName, kw)
                    .or().like(DictType::getDescription, kw));
        }
        if (status != null) q.eq(DictType::getStatus, normalizeStatus(status));
        q.orderByAsc(DictType::getSortOrder).orderByAsc(DictType::getTypeCode);
        return list(q);
    }

    public DictType addType(String typeCode, String typeName, String description, Integer sortOrder, Integer status) {
        String code = normalize(typeCode);
        String name = normalize(typeName);
        if (code.isEmpty()) throw new RuntimeException("字典编码不能为空");
        if (name.isEmpty()) throw new RuntimeException("字典名称不能为空");
        if (existsTypeCode(code, null)) throw new RuntimeException("字典编码已存在");
        DictType type = new DictType();
        type.setTypeCode(code);
        type.setTypeName(name);
        type.setDescription(description);
        type.setSortOrder(sortOrder == null ? 0 : sortOrder);
        type.setStatus(normalizeStatus(status));
        LocalDateTime now = LocalDateTime.now();
        type.setCreatedAt(now);
        type.setUpdatedAt(now);
        save(type);
        return type;
    }

    public Map<String, Object> importTypes(List<List<String>> rows) {
        return importRows(rows, values -> {
            String typeCode = valueAt(values, 0);
            String typeName = valueAt(values, 1);
            if (typeCode.isEmpty() && typeName.isEmpty()) {
                return ImportRowResult.skip("空行已跳过");
            }
            if (typeCode.isEmpty()) throw new RuntimeException("字典编码不能为空");
            if (typeName.isEmpty()) throw new RuntimeException("字典名称不能为空");
            if (existsTypeCode(typeCode, null)) {
                return ImportRowResult.skip("字典编码已存在，已跳过：" + typeCode);
            }
            addType(typeCode, typeName, emptyToNull(valueAt(values, 4)), parseInteger(valueAt(values, 3), 0), parseStatusText(valueAt(values, 2)));
            return ImportRowResult.success();
        });
    }

    public void updateType(Long id, String typeCode, String typeName, String description, Integer sortOrder, Integer status) {
        DictType type = getById(id);
        if (type == null) throw new RuntimeException("字典类型不存在");
        boolean builtIn = isBuiltInType(type);
        if (typeCode != null) {
            String code = normalize(typeCode);
            if (code.isEmpty()) throw new RuntimeException("字典编码不能为空");
            if (builtIn && !code.equals(type.getTypeCode())) throw new RuntimeException("内置字典类型编码不允许修改");
            if (existsTypeCode(code, id)) throw new RuntimeException("字典编码已存在");
            type.setTypeCode(code);
        }
        if (typeName != null) {
            String name = normalize(typeName);
            if (name.isEmpty()) throw new RuntimeException("字典名称不能为空");
            type.setTypeName(name);
        }
        if (description != null) type.setDescription(description);
        if (sortOrder != null) type.setSortOrder(sortOrder);
        if (status != null) {
            Integer nextStatus = normalizeStatus(status);
            if (builtIn && nextStatus == 0) throw new RuntimeException("内置字典类型不允许禁用");
            type.setStatus(nextStatus);
        }
        type.setUpdatedAt(LocalDateTime.now());
        updateById(type);
    }

    public void disableType(Long id) {
        DictType type = getById(id);
        if (type == null) throw new RuntimeException("字典类型不存在");
        if (isBuiltInType(type)) throw new RuntimeException("内置字典类型不允许禁用");
        type.setStatus(0);
        type.setUpdatedAt(LocalDateTime.now());
        updateById(type);
    }

    public Map<String, Object> listItems(Long typeId, int page, int size, String keyword, Integer status) {
        List<DictItem> records = queryItems(typeId, keyword, status);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int from = Math.min((safePage - 1) * safeSize, records.size());
        int to = Math.min(from + safeSize, records.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records.subList(from, to));
        result.put("total", records.size());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public List<DictItem> exportItems(Long typeId, String keyword, Integer status) {
        return queryItems(typeId, keyword, status);
    }

    private List<DictItem> queryItems(Long typeId, String keyword, Integer status) {
        assertTypeExists(typeId);
        LambdaQueryWrapper<DictItem> q = new LambdaQueryWrapper<>();
        q.eq(DictItem::getTypeId, typeId);
        String kw = normalize(keyword);
        if (!kw.isEmpty()) {
            q.and(w -> w.like(DictItem::getItemLabel, kw)
                    .or().like(DictItem::getItemValue, kw)
                    .or().like(DictItem::getDescription, kw));
        }
        if (status != null) q.eq(DictItem::getStatus, normalizeStatus(status));
        q.orderByDesc(DictItem::getIsDefault).orderByAsc(DictItem::getSortOrder).orderByAsc(DictItem::getItemValue);
        return dictItemMapper.selectList(q);
    }

    public DictItem addItem(Long typeId, String itemLabel, String itemValue, String tagColor, Integer sortOrder,
                            Integer status, Integer isDefault, String description) {
        assertTypeExists(typeId);
        String label = normalize(itemLabel);
        String value = normalize(itemValue);
        if (label.isEmpty()) throw new RuntimeException("字典项标签不能为空");
        if (value.isEmpty()) throw new RuntimeException("字典项值不能为空");
        if (existsItemValue(typeId, value, null)) throw new RuntimeException("字典项值已存在");
        DictItem item = new DictItem();
        item.setTypeId(typeId);
        item.setItemLabel(label);
        item.setItemValue(value);
        item.setTagColor(normalize(tagColor));
        item.setSortOrder(sortOrder == null ? 0 : sortOrder);
        Integer nextStatus = normalizeStatus(status);
        item.setStatus(nextStatus);
        item.setIsDefault(nextStatus == 0 ? 0 : normalizeFlag(isDefault));
        item.setDescription(description);
        LocalDateTime now = LocalDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        if (item.getIsDefault() == 1) clearDefault(typeId, null);
        dictItemMapper.insert(item);
        return item;
    }

    public Map<String, Object> importItems(Long typeId, List<List<String>> rows) {
        assertTypeExists(typeId);
        return importRows(rows, values -> {
            String itemLabel = valueAt(values, 0);
            String itemValue = valueAt(values, 1);
            if (itemLabel.isEmpty() && itemValue.isEmpty()) {
                return ImportRowResult.skip("空行已跳过");
            }
            if (itemLabel.isEmpty()) throw new RuntimeException("字典项标签不能为空");
            if (itemValue.isEmpty()) throw new RuntimeException("字典项值不能为空");
            if (existsItemValue(typeId, itemValue, null)) {
                return ImportRowResult.skip("字典项值已存在，已跳过：" + itemValue);
            }
            addItem(
                    typeId,
                    itemLabel,
                    itemValue,
                    emptyToNull(valueAt(values, 2)),
                    parseInteger(valueAt(values, 5), 0),
                    parseStatusText(valueAt(values, 3)),
                    parseFlagText(valueAt(values, 4)),
                    emptyToNull(valueAt(values, 6))
            );
            return ImportRowResult.success();
        });
    }

    public void updateItem(Long id, String itemLabel, String itemValue, String tagColor, Integer sortOrder,
                           Integer status, Integer isDefault, String description) {
        DictItem item = dictItemMapper.selectById(id);
        if (item == null) throw new RuntimeException("字典项不存在");
        DictType type = getById(item.getTypeId());
        boolean builtIn = isBuiltInItem(type, item);
        if (itemLabel != null) {
            String label = normalize(itemLabel);
            if (label.isEmpty()) throw new RuntimeException("字典项标签不能为空");
            item.setItemLabel(label);
        }
        if (itemValue != null) {
            String value = normalize(itemValue);
            if (value.isEmpty()) throw new RuntimeException("字典项值不能为空");
            if (builtIn && !value.equals(item.getItemValue())) throw new RuntimeException("内置字典项值不允许修改");
            if (existsItemValue(item.getTypeId(), value, id)) throw new RuntimeException("字典项值已存在");
            item.setItemValue(value);
        }
        if (tagColor != null) item.setTagColor(normalize(tagColor));
        if (sortOrder != null) item.setSortOrder(sortOrder);
        if (status != null) {
            Integer nextStatus = normalizeStatus(status);
            if (builtIn && nextStatus == 0) throw new RuntimeException("内置字典项不允许禁用");
            item.setStatus(nextStatus);
        }
        if (isDefault != null) {
            item.setIsDefault(normalizeFlag(isDefault));
        }
        if (item.getStatus() != null && item.getStatus() == 0) {
            item.setIsDefault(0);
        } else if (item.getIsDefault() != null && item.getIsDefault() == 1) {
            clearDefault(item.getTypeId(), id);
        }
        if (description != null) item.setDescription(description);
        item.setUpdatedAt(LocalDateTime.now());
        dictItemMapper.updateById(item);
    }

    public void disableItem(Long id) {
        DictItem item = dictItemMapper.selectById(id);
        if (item == null) throw new RuntimeException("字典项不存在");
        DictType type = getById(item.getTypeId());
        if (isBuiltInItem(type, item)) throw new RuntimeException("内置字典项不允许禁用");
        item.setStatus(0);
        item.setIsDefault(0);
        item.setUpdatedAt(LocalDateTime.now());
        dictItemMapper.updateById(item);
    }

    public List<DictItem> publicItems(String typeCode) {
        DictType type = getOne(new LambdaQueryWrapper<DictType>()
                .eq(DictType::getTypeCode, normalize(typeCode))
                .eq(DictType::getStatus, 1)
                .last("LIMIT 1"));
        if (type == null) return List.of();
        return dictItemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getTypeId, type.getId())
                .eq(DictItem::getStatus, 1)
                .orderByDesc(DictItem::getIsDefault)
                .orderByAsc(DictItem::getSortOrder)
                .orderByAsc(DictItem::getItemValue));
    }

    public DictItem getItemById(Long id) {
        return dictItemMapper.selectById(id);
    }

    public Map<String, Object> typeDisableImpact(Long id) {
        DictType type = getById(id);
        if (type == null) throw new RuntimeException("字典类型不存在");
        String typeCode = normalize(type.getTypeCode());
        boolean builtIn = isBuiltInType(type);
        long itemCount = dictItemMapper.selectCount(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getTypeId, id));
        long enabledItemCount = dictItemMapper.selectCount(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getTypeId, id)
                .eq(DictItem::getStatus, 1));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", type.getId());
        result.put("typeCode", type.getTypeCode());
        result.put("typeName", type.getTypeName());
        result.put("status", type.getStatus());
        result.put("builtIn", builtIn);
        result.put("itemCount", itemCount);
        result.put("enabledItemCount", enabledItemCount);
        result.put("publicAvailable", type.getStatus() != null && type.getStatus() == 1 && enabledItemCount > 0);
        result.put("usage", usageForType(typeCode));
        result.put("canDisable", !builtIn);
        result.put("message", builtIn ? "内置字典类型用于系统运行，不允许禁用。" : "禁用后，该字典类型的公共字典接口将返回空列表。");
        return result;
    }

    public Map<String, Object> itemDisableImpact(Long id) {
        DictItem item = dictItemMapper.selectById(id);
        if (item == null) throw new RuntimeException("字典项不存在");
        DictType type = getById(item.getTypeId());
        if (type == null) throw new RuntimeException("字典类型不存在");
        String typeCode = normalize(type.getTypeCode());
        boolean builtIn = isBuiltInItem(type, item);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.getId());
        result.put("typeId", type.getId());
        result.put("typeCode", type.getTypeCode());
        result.put("typeName", type.getTypeName());
        result.put("itemLabel", item.getItemLabel());
        result.put("itemValue", item.getItemValue());
        result.put("status", item.getStatus());
        result.put("isDefault", item.getIsDefault());
        result.put("builtIn", builtIn);
        result.put("usage", usageForType(typeCode));
        result.put("willRemoveFromPublic", type.getStatus() != null && type.getStatus() == 1 && item.getStatus() != null && item.getStatus() == 1);
        result.put("canDisable", !builtIn);
        result.put("message", builtIn ? "内置字典项用于系统运行，不允许禁用。" : "禁用后，该字典项将从公共下拉和标签展示中移除。");
        return result;
    }

    private void clearDefault(Long typeId, Long excludeId) {
        List<DictItem> items = dictItemMapper.selectList(new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getTypeId, typeId)
                .eq(DictItem::getIsDefault, 1));
        LocalDateTime now = LocalDateTime.now();
        for (DictItem item : items) {
            if (excludeId != null && excludeId.equals(item.getId())) continue;
            item.setIsDefault(0);
            item.setUpdatedAt(now);
            dictItemMapper.updateById(item);
        }
    }

    private void assertTypeExists(Long typeId) {
        if (typeId == null || getById(typeId) == null) throw new RuntimeException("字典类型不存在");
    }

    private boolean existsTypeCode(String code, Long excludeId) {
        LambdaQueryWrapper<DictType> q = new LambdaQueryWrapper<>();
        q.eq(DictType::getTypeCode, code);
        if (excludeId != null) q.ne(DictType::getId, excludeId);
        return count(q) > 0;
    }

    private boolean existsItemValue(Long typeId, String value, Long excludeId) {
        LambdaQueryWrapper<DictItem> q = new LambdaQueryWrapper<>();
        q.eq(DictItem::getTypeId, typeId).eq(DictItem::getItemValue, value);
        if (excludeId != null) q.ne(DictItem::getId, excludeId);
        return dictItemMapper.selectCount(q) > 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) return 1;
        if (status != 0 && status != 1) throw new RuntimeException("状态不正确");
        return status;
    }

    private Integer normalizeFlag(Integer value) {
        if (value == null) return 0;
        if (value != 0 && value != 1) throw new RuntimeException("默认标记不正确");
        return value;
    }

    private boolean isBuiltInType(DictType type) {
        return type != null && BUILT_IN_TYPES.contains(normalize(type.getTypeCode()));
    }

    private boolean isBuiltInItem(DictType type, DictItem item) {
        if (type == null || item == null) return false;
        Set<String> values = BUILT_IN_ITEMS.get(normalize(type.getTypeCode()));
        return values != null && values.contains(normalize(item.getItemValue()));
    }

    private List<String> usageForType(String typeCode) {
        return BUILT_IN_USAGE.getOrDefault(normalize(typeCode), List.of("公共字典接口", "使用该字典的业务下拉或标签展示"));
    }

    private Map<String, Object> importRows(List<List<String>> rows, ImportRowHandler handler) {
        List<List<String>> safeRows = rows == null ? List.of() : rows;
        int successCount = 0;
        int skippedCount = 0;
        int failureCount = 0;
        List<String> details = new java.util.ArrayList<>();
        for (int index = 0; index < safeRows.size(); index++) {
            int displayRow = index + 2;
            try {
                ImportRowResult result = handler.handle(safeRows.get(index));
                if (result == null || result.status == ImportRowStatus.SUCCESS) {
                    successCount++;
                    continue;
                }
                if (result.status == ImportRowStatus.SKIPPED) {
                    skippedCount++;
                    details.add("第" + displayRow + "行：" + result.message);
                }
            } catch (RuntimeException e) {
                failureCount++;
                details.add("第" + displayRow + "行：" + e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", safeRows.size());
        result.put("successCount", successCount);
        result.put("skippedCount", skippedCount);
        result.put("failureCount", failureCount);
        result.put("details", details);
        result.put("message", "导入完成：成功 " + successCount + "，跳过 " + skippedCount + "，失败 " + failureCount);
        return result;
    }

    private String valueAt(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) return "";
        return normalize(values.get(index));
    }

    private String emptyToNull(String value) {
        String text = normalize(value);
        return text.isEmpty() ? null : text;
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        String text = normalize(value);
        if (text.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new RuntimeException("数字格式不正确");
        }
    }

    private Integer parseStatusText(String value) {
        String text = normalize(value);
        if (text.isEmpty()) return 1;
        if ("1".equals(text) || "启用".equals(text) || "是".equals(text) || "true".equalsIgnoreCase(text)) return 1;
        if ("0".equals(text) || "禁用".equals(text) || "否".equals(text) || "false".equalsIgnoreCase(text)) return 0;
        throw new RuntimeException("状态仅支持：启用/禁用/1/0");
    }

    private Integer parseFlagText(String value) {
        String text = normalize(value);
        if (text.isEmpty()) return 0;
        if ("1".equals(text) || "是".equals(text) || "true".equalsIgnoreCase(text)) return 1;
        if ("0".equals(text) || "否".equals(text) || "false".equalsIgnoreCase(text)) return 0;
        throw new RuntimeException("默认项仅支持：是/否/1/0");
    }

    @FunctionalInterface
    private interface ImportRowHandler {
        ImportRowResult handle(List<String> values);
    }

    private enum ImportRowStatus {
        SUCCESS, SKIPPED
    }

    private static class ImportRowResult {
        private final ImportRowStatus status;
        private final String message;

        private ImportRowResult(ImportRowStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        private static ImportRowResult success() {
            return new ImportRowResult(ImportRowStatus.SUCCESS, null);
        }

        private static ImportRowResult skip(String message) {
            return new ImportRowResult(ImportRowStatus.SKIPPED, message);
        }
    }
}
