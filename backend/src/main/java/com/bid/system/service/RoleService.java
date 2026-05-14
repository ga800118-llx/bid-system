package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.Role;
import com.bid.system.entity.RoleDataScope;
import com.bid.system.entity.RoleFeature;
import com.bid.system.entity.RoleField;
import com.bid.system.entity.User;
import com.bid.system.entity.UserRole;
import com.bid.system.mapper.RoleDataScopeMapper;
import com.bid.system.mapper.RoleFeatureMapper;
import com.bid.system.mapper.RoleFieldMapper;
import com.bid.system.mapper.RoleMapper;
import com.bid.system.mapper.UserMapper;
import com.bid.system.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    private final RoleFeatureMapper roleFeatureMapper;
    private final RoleDataScopeMapper roleDataScopeMapper;
    private final RoleFieldMapper roleFieldMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;

    public RoleService(RoleFeatureMapper roleFeatureMapper,
                       RoleDataScopeMapper roleDataScopeMapper,
                       RoleFieldMapper roleFieldMapper,
                       UserRoleMapper userRoleMapper,
                       UserMapper userMapper) {
        this.roleFeatureMapper = roleFeatureMapper;
        this.roleDataScopeMapper = roleDataScopeMapper;
        this.roleFieldMapper = roleFieldMapper;
        this.userRoleMapper = userRoleMapper;
        this.userMapper = userMapper;
    }

    public List<Map<String, Object>> listRoles() {
        List<Role> roles = list(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Role role : roles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", role.getId());
            item.put("roleCode", role.getRoleCode());
            item.put("roleName", role.getRoleName());
            item.put("description", role.getDescription());
            item.put("status", role.getStatus());
            item.put("builtIn", isBuiltInRole(role) ? 1 : 0);
            item.put("featureCount", roleFeatureMapper.selectCount(new LambdaQueryWrapper<RoleFeature>().eq(RoleFeature::getRoleId, role.getId())));
            item.put("userCount", userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, role.getId())));
            item.put("createdAt", role.getCreatedAt());
            result.add(item);
        }
        return result;
    }

    public Role addRole(String roleCode, String roleName, String description, Integer status) {
        String code = normalizeCode(roleCode);
        String name = normalizeName(roleName);
        if (code.isEmpty()) throw new RuntimeException("角色编码不能为空");
        if (name.isEmpty()) throw new RuntimeException("角色名称不能为空");
        if (existsRoleCode(code, null)) throw new RuntimeException("角色编码已存在");
        if (existsRoleName(name, null)) throw new RuntimeException("角色名称已存在");
        Role r = new Role();
        r.setRoleCode(code);
        r.setRoleName(name);
        r.setDescription(description);
        r.setStatus(status != null ? status : 1);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        save(r);
        return r;
    }

    public void addRole(String roleName, String description) {
        addRole(roleName, roleName, description, 1);
    }

    public void updateRole(Long id, String roleName, String description, Integer status) {
        Role r = getById(id);
        if (r == null) throw new RuntimeException("角色不存在");
        if (roleName != null) {
            String name = normalizeName(roleName);
            if (name.isEmpty()) throw new RuntimeException("角色名称不能为空");
            if (existsRoleName(name, id)) throw new RuntimeException("角色名称已存在");
            r.setRoleName(name);
        }
        if (description != null) r.setDescription(description);
        if (status != null) {
            if (status != 0 && status != 1) throw new RuntimeException("状态不正确");
            if (status == 0) {
                if (isBuiltInRole(r)) throw new RuntimeException("内置角色不允许禁用");
                if (isAdminRole(r) && activeAdminRoleCount() <= 1) {
                    throw new RuntimeException("至少保留一个启用的管理员角色");
                }
            }
            r.setStatus(status);
        }
        r.setUpdatedAt(LocalDateTime.now());
        updateById(r);
    }

    public void updateRole(Long id, String description, Integer status) {
        updateRole(id, null, description, status);
    }

    public int deleteRole(Long id) {
        Role role = getById(id);
        if (role == null) throw new RuntimeException("角色不存在");
        if (isBuiltInRole(role)) throw new RuntimeException("内置角色不允许删除");
        long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        if (userCount > 0) throw new RuntimeException("角色已分配给用户，请先调整用户角色");
        roleFeatureMapper.delete(new LambdaQueryWrapper<RoleFeature>().eq(RoleFeature::getRoleId, id));
        roleDataScopeMapper.delete(new LambdaQueryWrapper<RoleDataScope>().eq(RoleDataScope::getRoleId, id));
        roleFieldMapper.delete(new LambdaQueryWrapper<RoleField>().eq(RoleField::getRoleId, id));
        removeById(id);
        return 1;
    }

    public Map<String, Object> getRoleDisableImpact(Long id) {
        Role role = getById(id);
        if (role == null) throw new RuntimeException("角色不存在");
        Map<String, Object> impact = roleImpact(role);
        boolean builtIn = isBuiltInRole(role);
        boolean lastAdmin = isAdminRole(role) && activeAdminRoleCount() <= 1;
        impact.put("canDisable", !builtIn && !lastAdmin);
        List<String> warnings = new ArrayList<>();
        if (builtIn) warnings.add("内置角色不允许禁用");
        if (lastAdmin) warnings.add("至少保留一个启用的管理员角色");
        if (((Number) impact.get("userCount")).longValue() > 0) warnings.add("禁用后已分配该角色的用户将失去该角色权限");
        impact.put("warnings", warnings);
        return impact;
    }

    public Map<String, Object> getRoleDeleteImpact(Long id) {
        Role role = getById(id);
        if (role == null) throw new RuntimeException("角色不存在");
        Map<String, Object> impact = roleImpact(role);
        boolean builtIn = isBuiltInRole(role);
        long userCount = ((Number) impact.get("userCount")).longValue();
        impact.put("canDelete", !builtIn && userCount == 0);
        List<String> warnings = new ArrayList<>();
        if (builtIn) warnings.add("内置角色不允许删除");
        if (userCount > 0) warnings.add("角色已分配给用户，请先调整用户角色");
        if (((Number) impact.get("featureCount")).longValue() > 0) warnings.add("删除后该角色的功能权限配置会一并移除");
        if (((Number) impact.get("dataScopeCount")).longValue() > 0) warnings.add("删除后该角色的数据范围配置会一并移除");
        if (((Number) impact.get("fieldCount")).longValue() > 0) warnings.add("删除后该角色的字段权限配置会一并移除");
        impact.put("warnings", warnings);
        return impact;
    }

    private Map<String, Object> roleImpact(Role role) {
        Map<String, Object> impact = new LinkedHashMap<>();
        impact.put("roleId", role.getId());
        impact.put("roleCode", role.getRoleCode());
        impact.put("roleName", role.getRoleName());
        impact.put("status", role.getStatus());
        impact.put("builtIn", isBuiltInRole(role) ? 1 : 0);
        long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, role.getId()));
        impact.put("userCount", userCount);
        impact.put("activeUserCount", activeUserCount(role.getId()));
        impact.put("featureCount", roleFeatureMapper.selectCount(new LambdaQueryWrapper<RoleFeature>().eq(RoleFeature::getRoleId, role.getId())));
        impact.put("dataScopeCount", roleDataScopeMapper.selectCount(new LambdaQueryWrapper<RoleDataScope>().eq(RoleDataScope::getRoleId, role.getId())));
        impact.put("fieldCount", roleFieldMapper.selectCount(new LambdaQueryWrapper<RoleField>().eq(RoleField::getRoleId, role.getId())));
        return impact;
    }

    public Map<String, Object> getPermissionCatalog() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("features", featureCatalog());
        result.put("dataScopes", dataScopeCatalog());
        result.put("fields", fieldCatalog());
        result.put("scopeRules", List.of(
                option("OWN", "本人数据"),
                option("DEPT", "本部门"),
                option("DEPT_SUB", "本部门及下级"),
                option("DEPT_UP", "本部门及上级"),
                option("ALL", "全部数据")
        ));
        return result;
    }

    public Map<String, Object> getPermissions(Long roleId) {
        assertRoleExists(roleId);
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> allowedFeatures = collectCodes(featureCatalog());
        result.put("features", roleFeatureMapper.selectList(new LambdaQueryWrapper<RoleFeature>().eq(RoleFeature::getRoleId, roleId))
                .stream()
                .map(RoleFeature::getFeatureCode)
                .filter(allowedFeatures::contains)
                .toList());
        Set<String> allowedModules = collectModuleCodes(dataScopeCatalog());
        List<Map<String, Object>> scopes = new ArrayList<>();
        for (RoleDataScope scope : roleDataScopeMapper.selectList(new LambdaQueryWrapper<RoleDataScope>().eq(RoleDataScope::getRoleId, roleId))) {
            if (!allowedModules.contains(scope.getModuleCode())) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("moduleCode", scope.getModuleCode());
            item.put("scopeRule", scope.getScopeRule());
            scopes.add(item);
        }
        result.put("dataScopes", scopes);
        Set<String> allowedFields = collectFieldKeys(fieldCatalog());
        Set<String> writableFields = collectWritableFieldKeys(fieldCatalog());
        List<Map<String, Object>> fields = new ArrayList<>();
        for (RoleField field : roleFieldMapper.selectList(new LambdaQueryWrapper<RoleField>().eq(RoleField::getRoleId, roleId))) {
            String key = field.getModuleCode() + ":" + field.getFieldCode();
            if (!allowedFields.contains(key)) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("moduleCode", field.getModuleCode());
            item.put("fieldCode", field.getFieldCode());
            item.put("canRead", field.getCanRead());
            item.put("canWrite", writableFields.contains(key) ? field.getCanWrite() : 0);
            fields.add(item);
        }
        result.put("fields", fields);
        return result;
    }

    public void saveFeatures(Long roleId, List<String> featureCodes) {
        assertRoleExists(roleId);
        Set<String> allowed = collectCodes(featureCatalog());
        roleFeatureMapper.delete(new LambdaQueryWrapper<RoleFeature>().eq(RoleFeature::getRoleId, roleId));
        LocalDateTime now = LocalDateTime.now();
        for (String code : normalizeList(featureCodes)) {
            if (!allowed.contains(code)) throw new RuntimeException("未知功能权限: " + code);
            RoleFeature row = new RoleFeature();
            row.setRoleId(roleId);
            row.setFeatureCode(code);
            row.setCreatedAt(now);
            roleFeatureMapper.insert(row);
        }
    }

    public void saveDataScopes(Long roleId, List<Map<String, Object>> scopes) {
        assertRoleExists(roleId);
        Set<String> modules = collectModuleCodes(dataScopeCatalog());
        Set<String> rules = Set.of("OWN", "DEPT", "DEPT_SUB", "DEPT_UP", "ALL");
        roleDataScopeMapper.delete(new LambdaQueryWrapper<RoleDataScope>().eq(RoleDataScope::getRoleId, roleId));
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> item : scopes == null ? List.<Map<String, Object>>of() : scopes) {
            String moduleCode = item.get("moduleCode") == null ? "" : String.valueOf(item.get("moduleCode")).trim();
            String scopeRule = item.get("scopeRule") == null ? "" : String.valueOf(item.get("scopeRule")).trim();
            if (!modules.contains(moduleCode)) throw new RuntimeException("未知数据模块: " + moduleCode);
            if (!rules.contains(scopeRule)) throw new RuntimeException("未知数据范围: " + scopeRule);
            RoleDataScope row = new RoleDataScope();
            row.setRoleId(roleId);
            row.setModuleCode(moduleCode);
            row.setScopeRule(scopeRule);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            roleDataScopeMapper.insert(row);
        }
    }

    public void saveFields(Long roleId, List<Map<String, Object>> fields) {
        assertRoleExists(roleId);
        Set<String> allowed = collectFieldKeys(fieldCatalog());
        Set<String> writableFields = collectWritableFieldKeys(fieldCatalog());
        roleFieldMapper.delete(new LambdaQueryWrapper<RoleField>().eq(RoleField::getRoleId, roleId));
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> item : fields == null ? List.<Map<String, Object>>of() : fields) {
            String moduleCode = item.get("moduleCode") == null ? "" : String.valueOf(item.get("moduleCode")).trim();
            String fieldCode = item.get("fieldCode") == null ? "" : String.valueOf(item.get("fieldCode")).trim();
            String key = moduleCode + ":" + fieldCode;
            if (!allowed.contains(key)) throw new RuntimeException("未知字段权限: " + fieldCode);
            int canWrite = writableFields.contains(key) ? boolToInt(item.get("canWrite")) : 0;
            int canRead = canWrite == 1 ? 1 : boolToInt(item.get("canRead"));
            RoleField row = new RoleField();
            row.setRoleId(roleId);
            row.setModuleCode(moduleCode);
            row.setFieldCode(fieldCode);
            row.setCanRead(canRead);
            row.setCanWrite(canWrite);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            roleFieldMapper.insert(row);
        }
    }

    public List<Map<String, Object>> getUserRoles(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserRole userRole : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))) {
            Role role = getById(userRole.getRoleId());
            if (role == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", role.getId());
            item.put("roleCode", role.getRoleCode());
            item.put("roleName", role.getRoleName());
            result.add(item);
        }
        return result;
    }

    public void assignUserRoles(Long userId, List<Long> roleIds) {
        assignUserRoles(userId, roleIds, null);
    }

    public void assignUserRoles(Long userId, List<Long> roleIds, String operatorUsername) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        List<Long> ids = roleIds == null ? List.of() : roleIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) throw new RuntimeException("至少选择一个角色");
        List<Role> roles = new ArrayList<>();
        for (Long roleId : ids) {
            Role role = getById(roleId);
            if (role == null) throw new RuntimeException("角色不存在: " + roleId);
            if (role.getStatus() != null && role.getStatus() == 0) throw new RuntimeException("角色已禁用: " + role.getRoleName());
            roles.add(role);
        }
        assertCanAssignRoleEntities(userId, roles, operatorUsername);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        LocalDateTime now = LocalDateTime.now();
        for (Long roleId : ids) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setCreatedAt(now);
            userRoleMapper.insert(userRole);
        }
        boolean admin = roles.stream().anyMatch(r -> "admin".equals(r.getRoleCode()) || "admin".equals(r.getRoleName()));
        user.setRole(admin ? "admin" : "user");
        user.setUpdatedAt(now);
        userMapper.updateById(user);
    }

    public void assertCanAssignUserRoles(Long userId, List<Long> roleIds, String operatorUsername) {
        List<Long> ids = roleIds == null ? List.of() : roleIds.stream().filter(Objects::nonNull).distinct().toList();
        List<Role> roles = new ArrayList<>();
        for (Long roleId : ids) {
            Role role = getById(roleId);
            if (role == null) throw new RuntimeException("角色不存在: " + roleId);
            if (role.getStatus() != null && role.getStatus() == 0) throw new RuntimeException("角色已禁用: " + role.getRoleName());
            roles.add(role);
        }
        assertCanAssignRoleEntities(userId, roles, operatorUsername);
    }

    public List<Long> getUserRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).toList();
    }

    public List<String> getUserRoleNames(Long userId) {
        List<String> names = new ArrayList<>();
        for (UserRole userRole : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))) {
            Role role = getById(userRole.getRoleId());
            if (role != null) names.add(role.getRoleName());
        }
        return names;
    }

    public List<String> getFeatureCodesForUser(String username) {
        User user = findUserByUsername(username);
        if (user == null) return List.of();
        if ("admin".equals(user.getRole())) return allFeatureCodes();

        Set<String> allowed = collectCodes(featureCatalog());
        Set<String> codes = new LinkedHashSet<>();
        for (UserRole userRole : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()))) {
            Role role = getById(userRole.getRoleId());
            if (role == null || (role.getStatus() != null && role.getStatus() == 0)) continue;
            for (RoleFeature feature : roleFeatureMapper.selectList(new LambdaQueryWrapper<RoleFeature>().eq(RoleFeature::getRoleId, role.getId()))) {
                String code = feature.getFeatureCode();
                if (code != null && allowed.contains(code)) {
                    codes.add(code);
                }
            }
        }
        return new ArrayList<>(codes);
    }

    public String getDataScopeRuleForUser(String username, String moduleCode) {
        User user = findUserByUsername(username);
        if (user == null) return "OWN";
        if ("admin".equals(user.getRole())) return "ALL";
        String result = null;
        for (UserRole userRole : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()))) {
            Role role = getById(userRole.getRoleId());
            if (role == null || (role.getStatus() != null && role.getStatus() == 0)) continue;
            List<RoleDataScope> scopes = roleDataScopeMapper.selectList(new LambdaQueryWrapper<RoleDataScope>()
                    .eq(RoleDataScope::getRoleId, role.getId())
                    .eq(RoleDataScope::getModuleCode, moduleCode));
            for (RoleDataScope scope : scopes) {
                result = morePermissiveScope(result, scope.getScopeRule());
            }
        }
        return result == null ? "DEPT" : result;
    }

    public Set<String> getReadableFieldsForUser(String username, String moduleCode) {
        return getFieldsForUser(username, moduleCode, true);
    }

    public Set<String> getWritableFieldsForUser(String username, String moduleCode) {
        return getFieldsForUser(username, moduleCode, false);
    }

    public boolean hasWritableField(String username, String moduleCode, String fieldCode) {
        if (fieldCode == null || fieldCode.isBlank()) return true;
        return getWritableFieldsForUser(username, moduleCode).contains(fieldCode);
    }

    private Set<String> getFieldsForUser(String username, String moduleCode, boolean read) {
        Set<String> allFields = read ? allFieldCodesForModule(moduleCode) : allWritableFieldCodesForModule(moduleCode);
        User user = findUserByUsername(username);
        if (user == null) return Set.of();
        if ("admin".equals(user.getRole())) return allFields;

        Set<String> allowed = new LinkedHashSet<>();
        boolean configured = false;
        for (UserRole userRole : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()))) {
            Role role = getById(userRole.getRoleId());
            if (role == null || (role.getStatus() != null && role.getStatus() == 0)) continue;
            List<RoleField> fields = roleFieldMapper.selectList(new LambdaQueryWrapper<RoleField>()
                    .eq(RoleField::getRoleId, role.getId())
                    .eq(RoleField::getModuleCode, moduleCode));
            if (!fields.isEmpty()) configured = true;
            for (RoleField field : fields) {
                if (field.getFieldCode() == null) continue;
                boolean granted = read
                        ? (field.getCanRead() == null || field.getCanRead() == 1)
                        : (field.getCanWrite() != null && field.getCanWrite() == 1);
                if (granted) {
                    allowed.add(field.getFieldCode());
                }
            }
        }
        if (!configured) return allFields;
        Set<String> ordered = new LinkedHashSet<>();
        for (String fieldCode : allFields) {
            if (allowed.contains(fieldCode)) ordered.add(fieldCode);
        }
        return ordered;
    }

    public boolean hasFeature(String username, String legacyRole, String featureCode) {
        if (featureCode == null || featureCode.isBlank()) return true;
        return getFeatureCodesForUser(username).contains(featureCode);
    }

    public List<String> allFeatureCodes() {
        return new ArrayList<>(collectCodes(featureCatalog()));
    }

    private User findUserByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private void assertCanAssignRoleEntities(Long userId, List<Role> roles, String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) return;
        User target = userMapper.selectById(userId);
        User operator = findUserByUsername(operatorUsername);
        if (target == null || operator == null) return;
        if (!Objects.equals(target.getId(), operator.getId())) return;
        if (!isAdminUser(operator)) return;
        boolean keepAdmin = roles.stream().anyMatch(this::isAdminRole);
        if (!keepAdmin) throw new RuntimeException("不能移除当前登录管理员自己的管理员角色");
    }

    private boolean isAdminUser(User user) {
        return user != null && "admin".equals(user.getRole());
    }

    private boolean isBuiltInRole(Role role) {
        if (role == null) return false;
        return "admin".equals(role.getRoleCode())
                || "user".equals(role.getRoleCode())
                || "admin".equals(role.getRoleName())
                || "user".equals(role.getRoleName())
                || "系统管理员".equals(role.getRoleName())
                || "普通用户".equals(role.getRoleName());
    }

    private boolean isAdminRole(Role role) {
        if (role == null) return false;
        return "admin".equals(role.getRoleCode())
                || "admin".equals(role.getRoleName())
                || "系统管理员".equals(role.getRoleName());
    }

    private long activeAdminRoleCount() {
        return list(new LambdaQueryWrapper<Role>().eq(Role::getStatus, 1))
                .stream()
                .filter(this::isAdminRole)
                .count();
    }

    private long activeUserCount(Long roleId) {
        long count = 0;
        for (UserRole userRole : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId))) {
            User user = userMapper.selectById(userRole.getUserId());
            if (user != null && (user.getStatus() == null || user.getStatus() == 1)) {
                count++;
            }
        }
        return count;
    }

    private String morePermissiveScope(String current, String next) {
        if (next == null || next.isBlank()) return current;
        if (current == null || scopeRank(next) > scopeRank(current)) return next;
        return current;
    }

    private int scopeRank(String scopeRule) {
        return switch (scopeRule) {
            case "ALL" -> 5;
            case "DEPT_SUB" -> 4;
            case "DEPT_UP" -> 3;
            case "DEPT" -> 2;
            case "OWN" -> 1;
            default -> 0;
        };
    }

    private void assertRoleExists(Long roleId) {
        if (getById(roleId) == null) throw new RuntimeException("角色不存在");
    }

    private boolean existsRoleCode(String code, Long excludeId) {
        LambdaQueryWrapper<Role> q = new LambdaQueryWrapper<>();
        q.eq(Role::getRoleCode, code);
        if (excludeId != null) q.ne(Role::getId, excludeId);
        return count(q) > 0;
    }

    private boolean existsRoleName(String name, Long excludeId) {
        LambdaQueryWrapper<Role> q = new LambdaQueryWrapper<>();
        q.eq(Role::getRoleName, name);
        if (excludeId != null) q.ne(Role::getId, excludeId);
        return count(q) > 0;
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim();
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isEmpty()).distinct().toList();
    }

    private Integer boolToInt(Object value) {
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        if (value instanceof Number) return ((Number) value).intValue() == 0 ? 0 : 1;
        return "true".equals(String.valueOf(value)) || "1".equals(String.valueOf(value)) ? 1 : 0;
    }

    private Set<String> collectCodes(List<Map<String, Object>> groups) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> group : groups) {
            Object children = group.get("children");
            if (!(children instanceof List<?>)) continue;
            for (Object child : (List<?>) children) {
                if (child instanceof Map<?, ?> map && map.get("code") != null) {
                    result.add(String.valueOf(map.get("code")));
                }
            }
        }
        return result;
    }

    private Set<String> collectModuleCodes(List<Map<String, Object>> modules) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> module : modules) {
            if (module.get("moduleCode") != null) result.add(String.valueOf(module.get("moduleCode")));
        }
        return result;
    }

    private Set<String> collectFieldKeys(List<Map<String, Object>> groups) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> group : groups) {
            String moduleCode = String.valueOf(group.get("moduleCode"));
            Object fields = group.get("fields");
            if (!(fields instanceof List<?>)) continue;
            for (Object field : (List<?>) fields) {
                if (field instanceof Map<?, ?> map && map.get("fieldCode") != null) {
                    result.add(moduleCode + ":" + map.get("fieldCode"));
                }
            }
        }
        return result;
    }

    private Set<String> collectWritableFieldKeys(List<Map<String, Object>> groups) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> group : groups) {
            String moduleCode = String.valueOf(group.get("moduleCode"));
            Object fields = group.get("fields");
            if (!(fields instanceof List<?>)) continue;
            for (Object field : (List<?>) fields) {
                if (field instanceof Map<?, ?> map && map.get("fieldCode") != null && !Boolean.FALSE.equals(map.get("writable"))) {
                    result.add(moduleCode + ":" + map.get("fieldCode"));
                }
            }
        }
        return result;
    }

    private Set<String> allFieldCodesForModule(String moduleCode) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> group : fieldCatalog()) {
            if (!Objects.equals(moduleCode, String.valueOf(group.get("moduleCode")))) continue;
            Object fields = group.get("fields");
            if (!(fields instanceof List<?>)) continue;
            for (Object field : (List<?>) fields) {
                if (field instanceof Map<?, ?> map && map.get("fieldCode") != null) {
                    result.add(String.valueOf(map.get("fieldCode")));
                }
            }
        }
        return result;
    }

    private Set<String> allWritableFieldCodesForModule(String moduleCode) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> group : fieldCatalog()) {
            if (!Objects.equals(moduleCode, String.valueOf(group.get("moduleCode")))) continue;
            Object fields = group.get("fields");
            if (!(fields instanceof List<?>)) continue;
            for (Object field : (List<?>) fields) {
                if (field instanceof Map<?, ?> map && map.get("fieldCode") != null && !Boolean.FALSE.equals(map.get("writable"))) {
                    result.add(String.valueOf(map.get("fieldCode")));
                }
            }
        }
        return result;
    }

    private Map<String, Object> option(String value, String label) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("value", value);
        item.put("label", label);
        return item;
    }

    private List<Map<String, Object>> featureCatalog() {
        return List.of(
                featureGroup("department", "部门管理",
                        feature("system_department:read", "查看部门"),
                        feature("system_department:create", "新增部门"),
                        feature("system_department:import", "导入部门"),
                        feature("system_department:update", "编辑部门"),
                        feature("system_department:delete", "删除部门"),
                        feature("system_department:export", "导出部门"),
                        feature("system_department:manager", "设置负责人")),
                featureGroup("user", "用户管理",
                        feature("system_user:read", "查看用户"),
                        feature("system_user:create", "新增用户"),
                        feature("system_user:import", "导入用户"),
                        feature("system_user:update", "编辑用户"),
                        feature("system_user:export", "导出用户"),
                        feature("system_user:status", "启用禁用"),
                        feature("system_user:password", "重置密码"),
                        feature("system_user:dept", "调整部门"),
                        feature("system_user:roles", "分配角色")),
                featureGroup("role", "角色权限",
                        feature("system_role:read", "查看角色"),
                        feature("system_role:create", "新增角色"),
                        feature("system_role:update", "编辑角色"),
                        feature("system_role:delete", "删除角色"),
                        feature("system_role:export", "导出角色"),
                        feature("system_role:permission", "分配权限")),
                featureGroup("operation_log", "操作日志",
                        feature("system_log:read", "查看日志"),
                        feature("system_log:export", "导出日志")),
                featureGroup("system_config", "系统配置",
                        feature("system_config:read", "查看配置"),
                        feature("system_config:import", "导入配置"),
                        feature("system_config:update", "编辑配置"),
                        feature("system_config:export", "导出配置")),
                featureGroup("system_dict", "字典管理",
                        feature("system_dict:read", "查看字典"),
                        feature("system_dict:create", "新增字典"),
                        feature("system_dict:import", "导入字典"),
                        feature("system_dict:update", "编辑/启停字典"),
                        feature("system_dict:export", "导出字典")),
                featureGroup("system_file", "文件中心",
                        feature("system_file:read", "查看文件"),
                        feature("system_file:upload", "上传文件"),
                        feature("system_file:download", "下载文件"),
                        feature("system_file:export", "导出文件"),
                        feature("system_file:delete", "删除/禁用文件"),
                        feature("system_file:link", "维护文件关联")),
                featureGroup("system_message", "消息中心",
                        feature("system_message:send", "发送站内消息"),
                        feature("system_message:export", "导出站内消息")),
                featureGroup("system_todo", "待办中心",
                        feature("system_todo:read", "查看待办"),
                        feature("system_todo:create", "派发待办"),
                        feature("system_todo:export", "导出待办"))
        );
    }

    private List<Map<String, Object>> dataScopeCatalog() {
        return List.of(
                dataModule("system_user", "部门与用户数据")
        );
    }

    private List<Map<String, Object>> fieldCatalog() {
        return List.of(
                fieldGroup("system_user", "用户管理",
                        field("username", "账号"),
                        field("realName", "姓名"),
                        field("phone", "手机号"),
                        field("idCard", "身份证号"),
                        field("email", "邮箱"),
                        field("dept", "所属部门"),
                        field("roles", "角色"),
                        field("status", "状态"),
                        field("forcePasswordChange", "强制改密"),
                        field("loginSecurity", "登录安全", false),
                        field("lastLoginIp", "最近登录 IP", false),
                        field("passwordUpdatedAt", "密码更新时间", false)),
                fieldGroup("system_log", "操作日志",
                        field("operator", "操作人", false),
                        field("ip", "IP 地址", false),
                        field("request", "请求信息", false),
                        field("content", "操作内容", false))
        );
    }

    @SafeVarargs
    private Map<String, Object> featureGroup(String moduleCode, String moduleName, Map<String, Object>... features) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("moduleCode", moduleCode);
        group.put("moduleName", moduleName);
        group.put("children", List.of(features));
        return group;
    }

    private Map<String, Object> feature(String code, String name) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("code", code);
        item.put("name", name);
        return item;
    }

    private Map<String, Object> dataModule(String moduleCode, String moduleName) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("moduleCode", moduleCode);
        item.put("moduleName", moduleName);
        return item;
    }

    @SafeVarargs
    private Map<String, Object> fieldGroup(String moduleCode, String moduleName, Map<String, Object>... fields) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("moduleCode", moduleCode);
        group.put("moduleName", moduleName);
        group.put("fields", List.of(fields));
        return group;
    }

    private Map<String, Object> field(String fieldCode, String fieldName) {
        return field(fieldCode, fieldName, true);
    }

    private Map<String, Object> field(String fieldCode, String fieldName, boolean writable) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fieldCode", fieldCode);
        item.put("fieldName", fieldName);
        item.put("writable", writable);
        return item;
    }
}
