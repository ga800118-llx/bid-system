package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.Department;
import com.bid.system.entity.User;
import com.bid.system.mapper.DepartmentMapper;
import com.bid.system.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DepartmentService extends ServiceImpl<DepartmentMapper, Department> {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private RoleService roleService;

    public List<Map<String, Object>> getTree() {
        return getTree(null);
    }

    public List<Map<String, Object>> getTree(String operatorUsername) {
        List<Department> all = visibleDepartments(operatorUsername);
        return buildTree(all, null);
    }

    public List<Map<String, Object>> exportDepartments(String operatorUsername) {
        List<Department> visibleDepartments = visibleDepartments(operatorUsername);
        Map<Long, Department> departmentMap = new LinkedHashMap<>();
        for (Department department : visibleDepartments) {
            if (department.getId() != null) {
                departmentMap.put(department.getId(), department);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        appendExportRows(visibleDepartments, departmentMap, null, 0, rows);
        return rows;
    }

    private List<Department> visibleDepartments(String operatorUsername) {
        LambdaQueryWrapper<Department> q = new LambdaQueryWrapper<>();
        q.orderByAsc(Department::getSortOrder);
        List<Department> all = list(q);
        Set<Long> visibleDeptIds = visibleDeptIds(operatorUsername);
        if (visibleDeptIds != null) {
            Set<Long> displayDeptIds = new LinkedHashSet<>(visibleDeptIds);
            for (Long deptId : visibleDeptIds) collectAncestorDeptIds(deptId, displayDeptIds);
            all = all.stream()
                    .filter(dept -> dept.getId() != null && displayDeptIds.contains(dept.getId()))
                    .toList();
        }
        return all;
    }

    private List<Map<String, Object>> buildTree(List<Department> all, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Department d : all) {
            boolean match = (parentId == null && d.getParentId() == null) || (parentId != null && parentId.equals(d.getParentId()));
            if (match) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", d.getId());
                node.put("name", displayName(d));
                node.put("parentId", d.getParentId());
                node.put("managerUserId", d.getManagerUserId());
                node.put("managerName", getManagerName(d.getManagerUserId()));
                node.put("sortOrder", d.getSortOrder());
                node.put("status", d.getStatus());
                node.put("children", buildTree(all, d.getId()));
                result.add(node);
            }
        }
        return result;
    }

    private void appendExportRows(List<Department> all,
                                  Map<Long, Department> departmentMap,
                                  Long parentId,
                                  int level,
                                  List<Map<String, Object>> rows) {
        for (Department department : all) {
            boolean match = (parentId == null && department.getParentId() == null)
                    || (parentId != null && parentId.equals(department.getParentId()));
            if (!match) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", displayName(department));
            row.put("level", level + 1);
            row.put("parentName", parentDisplayName(department, departmentMap));
            row.put("managerName", getManagerName(department.getManagerUserId()));
            row.put("status", department.getStatus());
            row.put("sortOrder", department.getSortOrder());
            row.put("createdAt", department.getCreatedAt());
            rows.add(row);
            appendExportRows(all, departmentMap, department.getId(), level + 1, rows);
        }
    }

    private String parentDisplayName(Department department, Map<Long, Department> departmentMap) {
        if (department == null || department.getParentId() == null) return "-";
        Department parent = departmentMap.get(department.getParentId());
        if (parent == null) return String.valueOf(department.getParentId());
        return displayName(parent);
    }

    private String getManagerName(Long managerUserId) {
        if (managerUserId == null) return null;
        User manager = userMapper.selectById(managerUserId);
        if (manager == null) return null;
        if (manager.getRealName() != null && !manager.getRealName().isBlank()) return manager.getRealName();
        return manager.getUsername();
    }

    private String displayName(Department department) {
        if (department.getId() != null && department.getId() == 1L && department.getParentId() == null) {
            return systemConfigService.getString("enterprise.name", "我的公司");
        }
        return department.getName();
    }

    public Set<Long> visibleDeptIds(String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) return null;
        User operator = findUser(operatorUsername);
        if (operator == null) return Set.of();
        String rule = roleService.getDataScopeRuleForUser(operatorUsername, "system_user");
        if ("ALL".equals(rule)) return null;
        if (operator.getDeptId() == null) return Set.of();
        return switch (rule) {
            case "DEPT_SUB" -> descendantDeptIds(operator.getDeptId());
            case "DEPT_UP" -> ancestorDeptIds(operator.getDeptId());
            case "OWN", "DEPT" -> new LinkedHashSet<>(List.of(operator.getDeptId()));
            default -> Set.of();
        };
    }

    public String dataScopeRule(String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) return "ALL";
        User operator = findUser(operatorUsername);
        if (operator == null) return "NONE";
        return roleService.getDataScopeRuleForUser(operatorUsername, "system_user");
    }

    private User findUser(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private Set<Long> descendantDeptIds(Long deptId) {
        Set<Long> ids = new LinkedHashSet<>();
        collectDeptIds(deptId, ids);
        return ids;
    }

    private Set<Long> ancestorDeptIds(Long deptId) {
        Set<Long> ids = new LinkedHashSet<>();
        collectAncestorDeptIds(deptId, ids);
        return ids;
    }

    private void collectAncestorDeptIds(Long deptId, Set<Long> deptIds) {
        Long currentId = deptId;
        Set<Long> visited = new LinkedHashSet<>();
        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId);
            Department dept = getById(currentId);
            if (dept == null) break;
            deptIds.add(dept.getId());
            currentId = dept.getParentId();
        }
    }

    public void addDept(String name, Long parentId, Integer sortOrder) {
        String deptName = normalizeName(name);
        if (deptName.isEmpty()) throw new RuntimeException("部门名称不能为空");
        if (parentId != null && getById(parentId) == null) throw new RuntimeException("父部门不存在");
        if (existsSameName(parentId, deptName, null)) throw new RuntimeException("同级部门名称不能重复");
        Department d = new Department();
        d.setName(deptName);
        d.setParentId(parentId);
        d.setSortOrder(sortOrder != null ? sortOrder : 0);
        d.setStatus(1);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        save(d);
    }

    public Map<String, Object> importDepartments(List<List<String>> rows) {
        List<List<String>> safeRows = rows == null ? List.of() : rows;
        int successCount = 0;
        int skippedCount = 0;
        int failureCount = 0;
        List<String> details = new ArrayList<>();
        for (int index = 0; index < safeRows.size(); index++) {
            int displayRow = index + 2;
            List<String> values = safeRows.get(index);
            String name = valueAt(values, 0);
            String parentName = valueAt(values, 1);
            try {
                if (name.isEmpty() && parentName.isEmpty()) {
                    skippedCount++;
                    details.add("第" + displayRow + "行：空行已跳过");
                    continue;
                }
                if (name.isEmpty()) throw new RuntimeException("部门名称不能为空");
                Long parentId = resolveImportParentId(parentName);
                if (existsSameName(parentId, name, null)) {
                    skippedCount++;
                    details.add("第" + displayRow + "行：同级部门名称已存在，已跳过：" + name);
                    continue;
                }
                Department department = new Department();
                department.setName(name);
                department.setParentId(parentId);
                department.setSortOrder(parseInteger(valueAt(values, 2), 0));
                department.setStatus(parseStatus(valueAt(values, 3)));
                department.setCreatedAt(LocalDateTime.now());
                department.setUpdatedAt(LocalDateTime.now());
                save(department);
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

    public void updateDept(Long id, String name, Integer sortOrder, Integer status, Long managerUserId) {
        Department d = getById(id);
        if (d == null) throw new RuntimeException("DEPT_NOT_FOUND");
        if (name != null) {
            String deptName = normalizeName(name);
            if (deptName.isEmpty()) throw new RuntimeException("部门名称不能为空");
            if (existsSameName(d.getParentId(), deptName, id)) throw new RuntimeException("同级部门名称不能重复");
            d.setName(deptName);
        }
        if (sortOrder != null) d.setSortOrder(sortOrder);
        if (status != null) d.setStatus(status);
        if (managerUserId != null) {
            validateManager(id, managerUserId);
            d.setManagerUserId(managerUserId);
        }
        d.setUpdatedAt(LocalDateTime.now());
        updateById(d);
    }

    public void clearManager(Long id) {
        Department d = getById(id);
        if (d == null) throw new RuntimeException("DEPT_NOT_FOUND");
        d.setManagerUserId(null);
        d.setUpdatedAt(LocalDateTime.now());
        updateById(d);
    }

    private void validateManager(Long deptId, Long managerUserId) {
        User user = userMapper.selectById(managerUserId);
        if (user == null) throw new RuntimeException("负责人不存在");
        if (user.getDeptId() == null || !user.getDeptId().equals(deptId)) {
            throw new RuntimeException("负责人必须是本部门用户");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("负责人不能是禁用用户");
        }
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private boolean existsSameName(Long parentId, String name, Long excludeId) {
        LambdaQueryWrapper<Department> q = new LambdaQueryWrapper<>();
        q.eq(Department::getName, name);
        if (parentId == null) {
            q.isNull(Department::getParentId);
        } else {
            q.eq(Department::getParentId, parentId);
        }
        if (excludeId != null) {
            q.ne(Department::getId, excludeId);
        }
        return count(q) > 0;
    }

    public int deleteDept(Long id) {
        long childCount = count(new LambdaQueryWrapper<Department>().eq(Department::getParentId, id));
        if (childCount > 0) throw new RuntimeException("存在子部门，请先删除或调整子部门");
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeptId, id));
        if (userCount > 0) throw new RuntimeException("部门下存在用户，请先调整用户所属部门");
        removeById(id);
        return (int) childCount;
    }

    public Map<String, Object> getDeleteImpact(Long id) {
        Department d = getById(id);
        if (d == null) throw new RuntimeException("DEPT_NOT_FOUND");
        Set<Long> deptIds = new LinkedHashSet<>();
        collectDeptIds(id, deptIds);
        long childCount = Math.max(0, deptIds.size() - 1);
        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.in(User::getDeptId, deptIds);
        long userCount = userMapper.selectCount(userQuery);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("childCount", childCount);
        result.put("userCount", userCount);
        result.put("managerUserId", d.getManagerUserId());
        result.put("managerName", getManagerName(d.getManagerUserId()));
        result.put("canDelete", childCount == 0 && userCount == 0);
        return result;
    }

    private void collectDeptIds(Long id, Set<Long> deptIds) {
        deptIds.add(id);
        List<Department> children = list(new LambdaQueryWrapper<Department>().eq(Department::getParentId, id));
        for (Department child : children) collectDeptIds(child.getId(), deptIds);
    }

    private String valueAt(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) return "";
        return values.get(index) == null ? "" : values.get(index).trim();
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("排序必须是数字");
        }
    }

    private Integer parseStatus(String value) {
        if (value == null || value.isBlank()) return 1;
        String text = value.trim();
        if ("1".equals(text) || "启用".equals(text) || "是".equals(text) || "true".equalsIgnoreCase(text)) return 1;
        if ("0".equals(text) || "禁用".equals(text) || "否".equals(text) || "false".equalsIgnoreCase(text)) return 0;
        throw new RuntimeException("状态仅支持：启用/禁用/1/0");
    }

    private Long resolveImportParentId(String parentName) {
        String text = parentName == null ? "" : parentName.trim();
        if (text.isEmpty() || "-".equals(text) || "无".equals(text)) return null;
        List<Department> all = list(new LambdaQueryWrapper<Department>().orderByAsc(Department::getId));
        List<Department> matches = new ArrayList<>();
        String enterpriseName = systemConfigService.getString("enterprise.name", "我的公司");
        for (Department department : all) {
            String rawName = department.getName() == null ? "" : department.getName().trim();
            String displayName = displayName(department).trim();
            if (text.equals(rawName) || text.equals(displayName) || (department.getId() != null && department.getId() == 1L && text.equals(enterpriseName))) {
                matches.add(department);
            }
        }
        if (matches.isEmpty()) throw new RuntimeException("父部门不存在：" + text);
        if (matches.size() > 1) throw new RuntimeException("父部门名称重复，请先整理部门：" + text);
        return matches.get(0).getId();
    }
}
