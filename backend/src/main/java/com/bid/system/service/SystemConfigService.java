package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.SystemConfig;
import com.bid.system.mapper.SystemConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class SystemConfigService extends ServiceImpl<SystemConfigMapper, SystemConfig> {

    private static final Set<String> VALUE_TYPES = Set.of("TEXT", "NUMBER", "BOOLEAN", "JSON");
    private static final Set<String> BUILT_IN_KEYS = Set.of(
            "enterprise.name",
            "system.title",
            "security.password.min_length",
            "security.password.require_strong",
            "security.login.max_failed_attempts",
            "security.session.timeout_minutes",
            "ui.default.page_size",
            "ui.page_size_options",
            "ui.sidebar.collapsed_default",
            "ui.watermark.enabled",
            "ui.watermark.text_template",
            "ui.watermark.opacity",
            "ui.watermark.font_size",
            "ui.watermark.rotate",
            "file.storage.type",
            "file.local.base_path",
            "file.upload.max_size_mb",
            "file.allowed_extensions",
            "file.previewable_extensions",
            "message.default_page_size"
    );
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public Map<String, Object> listConfigs(int page, int size, String keyword, String groupCode, Integer status, Boolean builtIn) {
        List<SystemConfig> all = queryConfigs(keyword, groupCode, status, builtIn);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int from = Math.min((safePage - 1) * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", all.subList(from, to));
        result.put("total", all.size());
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    public List<SystemConfig> exportConfigs(String keyword, String groupCode, Integer status, Boolean builtIn) {
        return queryConfigs(keyword, groupCode, status, builtIn);
    }

    private List<SystemConfig> queryConfigs(String keyword, String groupCode, Integer status, Boolean builtIn) {
        LambdaQueryWrapper<SystemConfig> q = new LambdaQueryWrapper<>();
        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) {
            q.and(w -> w.like(SystemConfig::getConfigKey, kw)
                    .or().like(SystemConfig::getConfigName, kw)
                    .or().like(SystemConfig::getConfigValue, kw));
        }
        if (groupCode != null && !groupCode.isBlank()) q.eq(SystemConfig::getGroupCode, groupCode.trim());
        if (status != null) q.eq(SystemConfig::getStatus, status);
        if (Boolean.TRUE.equals(builtIn)) q.in(SystemConfig::getConfigKey, BUILT_IN_KEYS);
        List<SystemConfig> all = new ArrayList<>(list(q));
        all.sort(Comparator
                .comparing((SystemConfig c) -> normalize(c.getGroupCode()))
                .thenComparing(c -> isBuiltInConfig(c) ? 0 : 1)
                .thenComparing(c -> normalize(c.getConfigKey())));
        return all;
    }

    public SystemConfig addConfig(String configKey, String configValue, String configName, String description,
                                  String valueType, String groupCode, Integer status) {
        String key = normalize(configKey);
        String name = normalize(configName);
        if (key.isEmpty()) throw new RuntimeException("配置键不能为空");
        if (name.isEmpty()) throw new RuntimeException("配置名称不能为空");
        if (existsKey(key, null)) throw new RuntimeException("配置键已存在");
        String type = normalizeValueType(valueType);
        validateConfigValue(configValue, type);
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(configValue);
        config.setConfigName(name);
        config.setDescription(description);
        config.setValueType(type);
        config.setGroupCode(normalizeGroup(groupCode));
        config.setStatus(normalizeStatus(status));
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        save(config);
        return config;
    }

    public Map<String, Object> importConfigs(List<List<String>> rows) {
        List<List<String>> safeRows = rows == null ? List.of() : rows;
        int successCount = 0;
        int skippedCount = 0;
        int failureCount = 0;
        List<String> details = new ArrayList<>();
        for (int index = 0; index < safeRows.size(); index++) {
            int displayRow = index + 2;
            List<String> values = safeRows.get(index);
            String configKey = valueAt(values, 0);
            String configName = valueAt(values, 1);
            try {
                if (configKey.isEmpty() && configName.isEmpty()) {
                    skippedCount++;
                    details.add("第" + displayRow + "行：空行已跳过");
                    continue;
                }
                if (configKey.isEmpty()) throw new RuntimeException("配置键不能为空");
                if (configName.isEmpty()) throw new RuntimeException("配置名称不能为空");
                if (existsKey(configKey, null)) {
                    skippedCount++;
                    details.add("第" + displayRow + "行：配置键已存在，已跳过：" + configKey);
                    continue;
                }
                addConfig(
                        configKey,
                        emptyToNull(valueAt(values, 2)),
                        configName,
                        emptyToNull(valueAt(values, 6)),
                        valueAt(values, 4),
                        valueAt(values, 3),
                        parseStatusText(valueAt(values, 5))
                );
                successCount++;
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

    public void updateConfig(Long id, String configKey, String configValue, String configName, String description,
                             String valueType, String groupCode, Integer status) {
        SystemConfig config = getById(id);
        if (config == null) throw new RuntimeException("配置不存在");
        String nextValue = configValue != null ? configValue : config.getConfigValue();
        String nextType = valueType != null ? normalizeValueType(valueType) : normalizeValueType(config.getValueType());
        validateConfigValue(nextValue, nextType);
        if (configKey != null) {
            String key = normalize(configKey);
            if (key.isEmpty()) throw new RuntimeException("配置键不能为空");
            if (isBuiltInConfig(config) && !key.equals(config.getConfigKey())) {
                throw new RuntimeException("内置配置键不允许修改");
            }
            if (!key.equals(config.getConfigKey()) && isBuiltInKey(key)) {
                throw new RuntimeException("内置配置键不允许占用");
            }
            if (existsKey(key, id)) throw new RuntimeException("配置键已存在");
            config.setConfigKey(key);
        }
        if (configName != null) {
            String name = normalize(configName);
            if (name.isEmpty()) throw new RuntimeException("配置名称不能为空");
            config.setConfigName(name);
        }
        if (configValue != null) config.setConfigValue(configValue);
        if (description != null) config.setDescription(description);
        if (valueType != null) config.setValueType(nextType);
        if (groupCode != null) config.setGroupCode(normalizeGroup(groupCode));
        if (status != null) {
            Integer nextStatus = normalizeStatus(status);
            if (nextStatus == 0 && isBuiltInConfig(config)) {
                throw new RuntimeException("内置配置不允许禁用");
            }
            config.setStatus(nextStatus);
        }
        config.setUpdatedAt(LocalDateTime.now());
        updateById(config);
    }

    public void disableConfig(Long id) {
        SystemConfig config = getById(id);
        if (config == null) throw new RuntimeException("配置不存在");
        if (isBuiltInConfig(config)) throw new RuntimeException("内置配置不允许禁用");
        config.setStatus(0);
        config.setUpdatedAt(LocalDateTime.now());
        updateById(config);
    }

    public String getValue(String key, String defaultValue) {
        return getString(key, defaultValue);
    }

    public String getString(String key, String defaultValue) {
        String value = getActiveConfigValue(key);
        if (value == null || value.isBlank()) return defaultValue;
        return value;
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = getActiveConfigValue(key);
        if (value == null || value.isBlank()) return defaultValue;
        String text = value.trim();
        if ("true".equalsIgnoreCase(text)) return true;
        if ("false".equalsIgnoreCase(text)) return false;
        return defaultValue;
    }

    public BigDecimal getNumber(String key, BigDecimal defaultValue) {
        String value = getActiveConfigValue(key);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public JsonNode getJson(String key, JsonNode defaultValue) {
        String value = getActiveConfigValue(key);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return JSON_MAPPER.readTree(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public <T> T getJson(String key, Class<T> type, T defaultValue) {
        JsonNode node = getJson(key, null);
        if (node == null) return defaultValue;
        try {
            return JSON_MAPPER.treeToValue(node, type);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getActiveConfigValue(String key) {
        if (key == null || key.isBlank()) return null;
        SystemConfig config = getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key.trim())
                .eq(SystemConfig::getStatus, 1)
                .last("LIMIT 1"));
        if (config == null) return null;
        return config.getConfigValue();
    }

    public Map<String, Object> publicBasic() {
        int defaultPageSize = Math.max(1, getNumber("ui.default.page_size", BigDecimal.valueOf(20)).intValue());
        int passwordMinLength = Math.max(1, getNumber("security.password.min_length", BigDecimal.valueOf(6)).intValue());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enterpriseName", getString("enterprise.name", "我的公司"));
        result.put("systemTitle", getString("system.title", "投标管理系统"));
        result.put("defaultPageSize", defaultPageSize);
        result.put("pageSizeOptions", getJson("ui.page_size_options", List.class, List.of(10, 20, 50, 100)));
        result.put("passwordMinLength", passwordMinLength);
        result.put("passwordRequireStrong", getBoolean("security.password.require_strong", false));
        result.put("sidebarCollapsedDefault", getBoolean("ui.sidebar.collapsed_default", false));
        result.put("watermarkEnabled", getBoolean("ui.watermark.enabled", false));
        result.put("watermarkTextTemplate", getString("ui.watermark.text_template", "{realName} {username}"));
        result.put("watermarkOpacity", getNumber("ui.watermark.opacity", new BigDecimal("0.12")));
        result.put("watermarkFontSize", getNumber("ui.watermark.font_size", new BigDecimal("16")));
        result.put("watermarkRotate", getNumber("ui.watermark.rotate", new BigDecimal("-24")));
        return result;
    }

    private boolean existsKey(String key, Long excludeId) {
        LambdaQueryWrapper<SystemConfig> q = new LambdaQueryWrapper<>();
        q.eq(SystemConfig::getConfigKey, key);
        if (excludeId != null) q.ne(SystemConfig::getId, excludeId);
        return count(q) > 0;
    }

    private boolean isBuiltInConfig(SystemConfig config) {
        return config != null && isBuiltInKey(config.getConfigKey());
    }

    private boolean isBuiltInKey(String key) {
        return BUILT_IN_KEYS.contains(normalize(key));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeGroup(String value) {
        String group = normalize(value);
        return group.isEmpty() ? "basic" : group;
    }

    private String normalizeValueType(String value) {
        String type = normalize(value).toUpperCase();
        if (type.isEmpty()) return "TEXT";
        if (!VALUE_TYPES.contains(type)) throw new RuntimeException("配置值类型不正确");
        return type;
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) return 1;
        if (status != 0 && status != 1) throw new RuntimeException("状态不正确");
        return status;
    }

    private void validateConfigValue(String value, String valueType) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || "TEXT".equals(valueType)) return;
        if ("NUMBER".equals(valueType)) {
            try {
                new BigDecimal(text);
                return;
            } catch (NumberFormatException e) {
                throw new RuntimeException("NUMBER 类型配置值必须是数字");
            }
        }
        if ("BOOLEAN".equals(valueType)) {
            if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) return;
            throw new RuntimeException("BOOLEAN 类型配置值必须是 true 或 false");
        }
        if ("JSON".equals(valueType)) {
            try {
                JSON_MAPPER.readTree(text);
                return;
            } catch (Exception e) {
                throw new RuntimeException("JSON 类型配置值格式不正确");
            }
        }
    }

    private String valueAt(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) return "";
        return normalize(values.get(index));
    }

    private String emptyToNull(String value) {
        String text = normalize(value);
        return text.isEmpty() ? null : text;
    }

    private Integer parseStatusText(String value) {
        String text = normalize(value);
        if (text.isEmpty()) return 1;
        if ("1".equals(text) || "启用".equals(text) || "是".equals(text) || "true".equalsIgnoreCase(text)) return 1;
        if ("0".equals(text) || "禁用".equals(text) || "否".equals(text) || "false".equalsIgnoreCase(text)) return 0;
        throw new RuntimeException("状态仅支持：启用/禁用/1/0");
    }
}
