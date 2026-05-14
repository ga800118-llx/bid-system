package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.Department;
import com.bid.system.entity.Role;
import com.bid.system.entity.User;
import com.bid.system.entity.UserRole;
import com.bid.system.exception.PermissionDeniedException;
import com.bid.system.mapper.DepartmentMapper;
import com.bid.system.mapper.RoleMapper;
import com.bid.system.mapper.UserMapper;
import com.bid.system.mapper.UserRoleMapper;
import com.bid.system.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    private static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 120;
    private static final int MAX_SESSION_TIMEOUT_MINUTES = 43200;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User register(String username, String password) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, username);
        if (userMapper.selectCount(q) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        validatePassword(password);
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("user");
        user.setStatus(1);
        user.setForcePasswordChange(0);
        user.setFailedLoginCount(0);
        LocalDateTime now = LocalDateTime.now();
        user.setPasswordUpdatedAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        syncLegacyUserRoles(user.getId(), user.getRole());
        return user;
    }

    public String login(String username, String password) {
        User user = loginUser(username, password);
        return user == null ? null : issueToken(user);
    }

    public String issueToken(User user) {
        if (user == null) return null;
        return JwtUtil.generateToken(user.getId(), user.getUsername(), sessionTimeoutMillis());
    }

    public int sessionTimeoutMinutes() {
        int minutes = systemConfigService.getNumber("security.session.timeout_minutes",
                BigDecimal.valueOf(DEFAULT_SESSION_TIMEOUT_MINUTES)).intValue();
        if (minutes < 1) return DEFAULT_SESSION_TIMEOUT_MINUTES;
        return Math.min(minutes, MAX_SESSION_TIMEOUT_MINUTES);
    }

    private long sessionTimeoutMillis() {
        return sessionTimeoutMinutes() * 60L * 1000L;
    }

    public User loginUser(String username, String password) {
        return loginUser(username, password, null);
    }

    public User loginUser(String username, String password, String ipAddress) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, username);
        User user = userMapper.selectOne(q);
        if (user == null) { return null; }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordLoginFailure(user);
            return null;
        }
        if (user.getStatus() != null && user.getStatus() == 0) { return null; }
        if (!"admin".equals(user.getRole())) {
            if (user.getDeptId() == null) { return null; }
            Department dept = departmentMapper.selectById(user.getDeptId());
            if (dept == null || (dept.getStatus() != null && dept.getStatus() == 0)) { return null; }
        }
        resetLoginFailures(user);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(normalizeText(ipAddress));
        userMapper.updateById(user);
        return user;
    }

    public String loginFailureReason(String username, String password) {
        String account = username == null ? "" : username.trim();
        if (account.isEmpty()) return "账号不能为空";
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, account);
        User user = userMapper.selectOne(q);
        if (user == null) return "账号不存在";
        int maxFailed = loginMaxFailedAttempts();
        int failedCount = user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount();
        if (user.getStatus() != null && user.getStatus() == 0 && maxFailed > 0 && failedCount >= maxFailed) {
            return "密码错误次数达到上限，账号已禁用";
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            if (maxFailed > 0) {
                return "密码错误，连续失败 " + failedCount + "/" + maxFailed;
            }
            return "密码错误";
        }
        if (user.getStatus() != null && user.getStatus() == 0) return "账号已禁用";
        if (!"admin".equals(user.getRole())) {
            if (user.getDeptId() == null) return "用户未分配部门";
            Department dept = departmentMapper.selectById(user.getDeptId());
            if (dept == null) return "所属部门不存在";
            if (dept.getStatus() != null && dept.getStatus() == 0) return "所属部门已禁用";
        }
        return "登录失败";
    }

    public boolean isAccountLockedByFailedAttempts(String username) {
        String account = username == null ? "" : username.trim();
        if (account.isEmpty()) return false;
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, account);
        User user = userMapper.selectOne(q);
        if (user == null) return false;
        int maxFailed = loginMaxFailedAttempts();
        int failedCount = user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount();
        return user.getStatus() != null && user.getStatus() == 0 && maxFailed > 0 && failedCount >= maxFailed;
    }

    private void recordLoginFailure(User user) {
        if (user == null) return;
        int failedCount = (user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount()) + 1;
        user.setFailedLoginCount(failedCount);
        user.setLastFailedLoginAt(LocalDateTime.now());
        if (!"admin".equals(user.getRole())) {
            int maxFailed = loginMaxFailedAttempts();
            if (maxFailed > 0 && failedCount >= maxFailed) {
                user.setStatus(0);
            }
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private void resetLoginFailures(User user) {
        if (user == null) return;
        user.setFailedLoginCount(0);
        user.setLastFailedLoginAt(null);
    }

    private int loginMaxFailedAttempts() {
        BigDecimal value = systemConfigService.getNumber("security.login.max_failed_attempts", BigDecimal.valueOf(5));
        int max = value == null ? 5 : value.intValue();
        return Math.max(0, max);
    }

    public List<User> list() {
        return userMapper.selectList(null);
    }

    public Map<String, Object> getSystemUsers(int page, int size, String keyword, Long deptId, boolean unassigned, Integer status, String role) {
        return getSystemUsers(page, size, keyword, deptId, unassigned, status, role, null, null);
    }

    public Map<String, Object> getSystemUsers(int page, int size, String keyword, Long deptId, boolean unassigned, Integer status, String role, String operatorUsername) {
        return getSystemUsers(page, size, keyword, deptId, unassigned, status, role, null, operatorUsername);
    }

    public Map<String, Object> getSystemUsers(int page, int size, String keyword, Long deptId, boolean unassigned, Integer status, String role, Long roleId, String operatorUsername) {
        LambdaQueryWrapper<User> q = buildSystemUserQuery(keyword, deptId, unassigned, status, role, roleId);
        String appliedDataScope = applyUserDataScope(q, operatorUsername);
        q.orderByDesc(User::getCreatedAt);
        Page<User> p = page(new Page<>(page, size), q);
        Set<String> readableFields = readableUserFields(operatorUsername);
        Set<String> writableFields = writableUserFields(operatorUsername);
        var records = p.getRecords().stream().map(u -> buildUserRecord(u, readableFields, true)).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", p.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("dataScope", appliedDataScope);
        result.put("readableFields", new ArrayList<>(readableFields));
        result.put("writableFields", new ArrayList<>(writableFields));
        return result;
    }

    public Map<String, Object> exportSystemUsers(String keyword, Long deptId, boolean unassigned, Integer status, String role, String operatorUsername) {
        return exportSystemUsers(keyword, deptId, unassigned, status, role, null, operatorUsername);
    }

    public Map<String, Object> exportSystemUsers(String keyword, Long deptId, boolean unassigned, Integer status, String role, Long roleId, String operatorUsername) {
        LambdaQueryWrapper<User> q = buildSystemUserQuery(keyword, deptId, unassigned, status, role, roleId);
        String appliedDataScope = applyUserDataScope(q, operatorUsername);
        q.orderByDesc(User::getCreatedAt);
        List<User> users = userMapper.selectList(q);
        Set<String> readableFields = readableUserFields(operatorUsername);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", users.stream().map(u -> buildUserRecord(u, readableFields, true)).toList());
        result.put("dataScope", appliedDataScope);
        result.put("readableFields", new ArrayList<>(readableFields));
        return result;
    }

    private LambdaQueryWrapper<User> buildSystemUserQuery(String keyword, Long deptId, boolean unassigned, Integer status, String role) {
        return buildSystemUserQuery(keyword, deptId, unassigned, status, role, null);
    }

    private LambdaQueryWrapper<User> buildSystemUserQuery(String keyword, Long deptId, boolean unassigned, Integer status, String role, Long roleId) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) {
            q.and(w -> w.like(User::getUsername, kw)
                    .or().like(User::getRealName, kw)
                    .or().like(User::getPhone, kw)
                    .or().like(User::getEmail, kw)
                    .or().like(User::getIdCard, kw));
        }
        if (unassigned) {
            q.isNull(User::getDeptId);
        } else if (deptId != null) {
            q.eq(User::getDeptId, deptId);
        }
        if (status != null) {
            if (status != 0 && status != 1) throw new RuntimeException("状态不正确");
            q.eq(User::getStatus, status);
        }
        if (role != null && !role.isBlank()) {
            q.eq(User::getRole, "admin".equals(role) ? "admin" : "user");
        }
        if (roleId != null) {
            List<Long> userIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleId))
                    .stream().map(UserRole::getUserId).distinct().toList();
            if (userIds.isEmpty()) q.eq(User::getId, -1L);
            else q.in(User::getId, userIds);
        }
        return q;
    }

    private String applyUserDataScope(LambdaQueryWrapper<User> q, String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) return "ALL";
        User operator = findByUsername(operatorUsername);
        if (operator == null) {
            q.eq(User::getId, -1L);
            return "NONE";
        }
        String rule = roleService.getDataScopeRuleForUser(operatorUsername, "system_user");
        if ("ALL".equals(rule)) return rule;
        if ("OWN".equals(rule)) {
            q.eq(User::getId, operator.getId());
            return rule;
        }
        if (operator.getDeptId() == null) {
            q.eq(User::getId, operator.getId());
            return rule + "_NO_DEPT";
        }
        Set<Long> deptIds = switch (rule) {
            case "DEPT_SUB" -> collectDescendantDeptIds(operator.getDeptId());
            case "DEPT_UP" -> collectAncestorDeptIds(operator.getDeptId());
            case "DEPT" -> new LinkedHashSet<>(List.of(operator.getDeptId()));
            default -> new LinkedHashSet<>();
        };
        if (deptIds.isEmpty()) {
            q.eq(User::getId, operator.getId());
        } else {
            q.in(User::getDeptId, deptIds);
        }
        return rule;
    }

    private Set<Long> collectDescendantDeptIds(Long deptId) {
        Set<Long> ids = new LinkedHashSet<>();
        collectDescendantDeptIds(deptId, ids);
        return ids;
    }

    private void collectDescendantDeptIds(Long deptId, Set<Long> ids) {
        if (deptId == null || ids.contains(deptId)) return;
        ids.add(deptId);
        List<Department> children = departmentMapper.selectList(new LambdaQueryWrapper<Department>().eq(Department::getParentId, deptId));
        for (Department child : children) {
            collectDescendantDeptIds(child.getId(), ids);
        }
    }

    private Set<Long> collectAncestorDeptIds(Long deptId) {
        Set<Long> ids = new LinkedHashSet<>();
        Long currentId = deptId;
        while (currentId != null && !ids.contains(currentId)) {
            Department dept = departmentMapper.selectById(currentId);
            if (dept == null) break;
            ids.add(dept.getId());
            currentId = dept.getParentId();
        }
        return ids;
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) return null;
        Department dept = departmentMapper.selectById(deptId);
        return dept == null ? null : dept.getName();
    }

    public String deptNameOf(Long deptId) {
        return getDeptName(deptId);
    }

    public void updateRole(Long id, String role) {
        updateRole(id, role, null);
    }

    public void updateRole(Long id, String role, String operatorUsername) {
        assertUserWritable(id, operatorUsername);
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setRole("admin".equals(role) ? "admin" : "user");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        syncLegacyUserRoles(id, user.getRole());
    }

    public void updatePassword(Long id, String password) {
        updatePassword(id, password, null);
    }

    public void updatePassword(Long id, String password, String operatorUsername) {
        updatePassword(id, password, 0, operatorUsername);
    }

    public void updatePassword(Long id, String password, Integer forcePasswordChange, String operatorUsername) {
        assertUserWritable(id, operatorUsername);
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        validatePassword(password);
        user.setPassword(passwordEncoder.encode(password));
        resetLoginFailures(user);
        LocalDateTime now = LocalDateTime.now();
        user.setPasswordUpdatedAt(now);
        user.setForcePasswordChange(forcePasswordChange != null && forcePasswordChange == 1 ? 1 : 0);
        user.setUpdatedAt(now);
        userMapper.updateById(user);
    }

    public User findByUsername(String username) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, username);
        return userMapper.selectOne(q);
    }

    public Map<String, Object> getByDept(Long deptId, int page, int size, String keyword) {
        return getByDept(deptId, page, size, keyword, null);
    }

    public Map<String, Object> getByDept(Long deptId, int page, int size, String keyword, String operatorUsername) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        if (deptId == null) {
            q.isNull(User::getDeptId);
        } else {
            q.eq(User::getDeptId, deptId);
        }
        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) {
            q.and(w -> w.like(User::getUsername, kw)
                    .or().like(User::getRealName, kw)
                    .or().like(User::getPhone, kw)
                    .or().like(User::getEmail, kw)
                    .or().like(User::getIdCard, kw));
        }
        String appliedDataScope = applyDeptUserDataScope(q, deptId, operatorUsername);
        q.orderByDesc(User::getCreatedAt);
        Page<User> p = page(new Page<>(page, size), q);
        Set<String> readableFields = readableUserFields(operatorUsername);
        Set<String> writableFields = writableUserFields(operatorUsername);
        var records = p.getRecords().stream().map(u -> buildUserRecord(u, readableFields, true)).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", p.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("dataScope", appliedDataScope);
        result.put("readableFields", new ArrayList<>(readableFields));
        result.put("writableFields", new ArrayList<>(writableFields));
        return result;
    }

    public Map<String, Object> currentProfile(String username) {
        User user = findByUsername(username);
        if (user == null) throw new RuntimeException("用户不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("phone", maskPhone(user.getPhone()));
        result.put("email", user.getEmail());
        result.put("role", user.getRole());
        result.put("deptId", user.getDeptId());
        result.put("deptName", deptNameOf(user.getDeptId()));
        result.put("status", user.getStatus());
        result.put("roleIds", roleService.getUserRoleIds(user.getId()));
        result.put("roleNames", roleService.getUserRoleNames(user.getId()));
        result.put("lastLoginAt", user.getLastLoginAt());
        result.put("lastLoginIp", user.getLastLoginIp());
        result.put("passwordUpdatedAt", user.getPasswordUpdatedAt());
        result.put("forcePasswordChange", user.getForcePasswordChange() == null ? 0 : user.getForcePasswordChange());
        result.put("failedLoginCount", user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount());
        result.put("lastFailedLoginAt", user.getLastFailedLoginAt());
        result.put("createdAt", user.getCreatedAt());
        result.put("sessionTimeoutMinutes", sessionTimeoutMinutes());
        result.put("loginMaxFailedAttempts", loginMaxFailedAttempts());
        result.put("passwordMinLength", passwordMinLength());
        result.put("passwordRequireStrong", passwordRequireStrong());
        return result;
    }

    public void changeOwnPassword(String username, String oldPassword, String newPassword) {
        User user = findByUsername(username);
        if (user == null) throw new RuntimeException("用户不存在");
        if (oldPassword == null || oldPassword.isBlank()) throw new RuntimeException("原密码不能为空");
        if (newPassword == null || newPassword.isBlank()) throw new RuntimeException("新密码不能为空");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) throw new RuntimeException("原密码不正确");
        if (passwordEncoder.matches(newPassword, user.getPassword())) throw new RuntimeException("新密码不能与原密码相同");
        validatePassword(newPassword);
        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(newPassword));
        resetLoginFailures(user);
        user.setPasswordUpdatedAt(now);
        user.setForcePasswordChange(0);
        user.setUpdatedAt(now);
        userMapper.updateById(user);
    }

    private Set<String> readableUserFields(String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) {
            return new LinkedHashSet<>(List.of("username", "realName", "phone", "idCard", "email", "dept", "roles", "status", "forcePasswordChange", "loginSecurity", "lastLoginIp", "passwordUpdatedAt"));
        }
        return roleService.getReadableFieldsForUser(operatorUsername, "system_user");
    }

    private Set<String> writableUserFields(String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) {
            return new LinkedHashSet<>(List.of("username", "realName", "phone", "idCard", "email", "dept", "roles", "status", "forcePasswordChange"));
        }
        return roleService.getWritableFieldsForUser(operatorUsername, "system_user");
    }

    private Map<String, Object> buildUserRecord(User u, Set<String> readableFields, boolean includeRoleNames) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("lastLoginAt", u.getLastLoginAt());
        m.put("createdAt", u.getCreatedAt());
        if (readableFields.contains("loginSecurity")) {
            m.put("failedLoginCount", u.getFailedLoginCount() == null ? 0 : u.getFailedLoginCount());
            m.put("lastFailedLoginAt", u.getLastFailedLoginAt());
        }
        if (readableFields.contains("username")) m.put("username", u.getUsername());
        if (readableFields.contains("realName")) m.put("realName", u.getRealName());
        if (readableFields.contains("phone")) m.put("phone", maskPhone(u.getPhone()));
        if (readableFields.contains("idCard")) m.put("idCard", maskIdCard(u.getIdCard()));
        if (readableFields.contains("email")) m.put("email", u.getEmail());
        if (readableFields.contains("forcePasswordChange")) m.put("forcePasswordChange", u.getForcePasswordChange() == null ? 0 : u.getForcePasswordChange());
        if (readableFields.contains("lastLoginIp")) m.put("lastLoginIp", u.getLastLoginIp());
        if (readableFields.contains("passwordUpdatedAt")) m.put("passwordUpdatedAt", u.getPasswordUpdatedAt());
        if (readableFields.contains("dept")) {
            m.put("deptId", u.getDeptId());
            m.put("deptName", getDeptName(u.getDeptId()));
        }
        if (readableFields.contains("status")) m.put("status", u.getStatus());
        if (readableFields.contains("roles")) {
            m.put("role", u.getRole());
            if (includeRoleNames) {
                m.put("roleIds", getUserRoleIds(u.getId()));
                m.put("roleNames", getUserRoleNames(u.getId()));
            }
        }
        return m;
    }

    private String applyDeptUserDataScope(LambdaQueryWrapper<User> q, Long deptId, String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) return "ALL";
        User operator = findByUsername(operatorUsername);
        if (operator == null) {
            q.eq(User::getId, -1L);
            return "NONE";
        }
        String rule = roleService.getDataScopeRuleForUser(operatorUsername, "system_user");
        if ("ALL".equals(rule)) return rule;
        if ("OWN".equals(rule)) {
            q.eq(User::getId, operator.getId());
            return rule;
        }
        Set<Long> visibleDeptIds = departmentService.visibleDeptIds(operatorUsername);
        if (deptId == null || visibleDeptIds == null || !visibleDeptIds.contains(deptId)) {
            q.eq(User::getId, -1L);
            return rule;
        }
        return rule;
    }

    public List<Map<String, Object>> getManagerCandidates(Long deptId) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getDeptId, deptId);
        q.eq(User::getStatus, 1);
        q.orderByAsc(User::getRealName).orderByAsc(User::getUsername);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : userMapper.selectList(q)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            result.add(m);
        }
        return result;
    }

    public User createSystemUser(String username, String password, String realName, String role, Long deptId, Integer status) {
        return createSystemUser(username, password, realName, role, deptId, status, null);
    }

    public User createSystemUser(String username, String password, String realName, String role, Long deptId, Integer status, String operatorUsername) {
        return createSystemUser(username, password, realName, null, null, null, role, deptId, status, 0, operatorUsername);
    }

    public User createSystemUser(String username, String password, String realName, String phone, String idCard, String email,
                                 String role, Long deptId, Integer status, Integer forcePasswordChange, String operatorUsername) {
        assertDeptWritable(deptId, operatorUsername);
        if (username == null || username.trim().isEmpty()) throw new RuntimeException("账号不能为空");
        validatePassword(password);
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        q.eq(User::getUsername, username.trim());
        if (userMapper.selectCount(q) > 0) throw new RuntimeException("账号已存在");
        if (deptId != null && departmentMapper.selectById(deptId) == null) throw new RuntimeException("部门不存在");
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(normalizeText(realName));
        user.setPhone(normalizeText(phone));
        user.setIdCard(normalizeText(idCard));
        user.setEmail(normalizeText(email));
        user.setRole("admin".equals(role) ? "admin" : "user");
        user.setDeptId(deptId);
        user.setStatus(status != null ? status : 1);
        user.setForcePasswordChange(forcePasswordChange != null && forcePasswordChange == 1 ? 1 : 0);
        user.setFailedLoginCount(0);
        LocalDateTime now = LocalDateTime.now();
        user.setPasswordUpdatedAt(now);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);
        syncLegacyUserRoles(user.getId(), user.getRole());
        return user;
    }

    public Map<String, Object> importSystemUsers(List<List<String>> rows, String operatorUsername) {
        List<List<String>> safeRows = rows == null ? List.of() : rows;
        int successCount = 0;
        int skippedCount = 0;
        int failureCount = 0;
        List<String> details = new ArrayList<>();
        for (int index = 0; index < safeRows.size(); index++) {
            int displayRow = index + 2;
            List<String> values = safeRows.get(index);
            String username = valueAt(values, 0);
            String realName = valueAt(values, 1);
            try {
                if (username.isEmpty() && realName.isEmpty()) {
                    skippedCount++;
                    details.add("第" + displayRow + "行：空行已跳过");
                    continue;
                }
                if (username.isEmpty()) throw new RuntimeException("账号不能为空");
                if (realName.isEmpty()) throw new RuntimeException("姓名不能为空");
                if (findByUsername(username) != null) {
                    skippedCount++;
                    details.add("第" + displayRow + "行：账号已存在，已跳过：" + username);
                    continue;
                }
                String password = valueAt(values, 2);
                if (password.isEmpty()) throw new RuntimeException("初始密码不能为空");
                String phone = valueAt(values, 3);
                String idCard = valueAt(values, 4);
                String email = valueAt(values, 5);
                Long deptId = resolveImportDeptId(valueAt(values, 6));
                Integer status = parseImportStatus(valueAt(values, 7));
                Integer forcePasswordChange = parseImportBoolean(valueAt(values, 8), 0, "强制改密");
                createSystemUser(username, password, realName, phone, idCard, email, "user", deptId, status, forcePasswordChange, operatorUsername);
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

    public void moveUsersToDept(List<Long> userIds, Long deptId) {
        moveUsersToDept(userIds, deptId, null);
    }

    public void moveUsersToDept(List<Long> userIds, Long deptId, String operatorUsername) {
        if (userIds == null || userIds.isEmpty()) throw new RuntimeException("请选择用户");
        assertDeptWritable(deptId, operatorUsername);
        if (deptId != null && departmentMapper.selectById(deptId) == null) throw new RuntimeException("目标部门不存在");
        LocalDateTime now = LocalDateTime.now();
        for (Long userId : userIds) {
            assertUserWritable(userId, operatorUsername);
            User user = userMapper.selectById(userId);
            if (user == null) throw new RuntimeException("用户不存在: " + userId);
            Long oldDeptId = user.getDeptId();
            user.setDeptId(deptId);
            user.setUpdatedAt(now);
            userMapper.updateById(user);
            if (oldDeptId != null) {
                Department oldDept = departmentMapper.selectById(oldDeptId);
                if (oldDept != null && userId.equals(oldDept.getManagerUserId())) {
                    oldDept.setManagerUserId(null);
                    oldDept.setUpdatedAt(now);
                    departmentMapper.updateById(oldDept);
                }
            }
        }
    }

    public void updateUsersStatus(List<Long> userIds, Integer status) {
        updateUsersStatus(userIds, status, null);
    }

    public void updateUsersStatus(List<Long> userIds, Integer status, String operatorUsername) {
        if (userIds == null || userIds.isEmpty()) throw new RuntimeException("请选择用户");
        if (status == null || (status != 0 && status != 1)) throw new RuntimeException("状态不正确");
        LocalDateTime now = LocalDateTime.now();
        for (Long userId : userIds) {
            assertUserWritable(userId, operatorUsername);
            User user = userMapper.selectById(userId);
            if (user == null) throw new RuntimeException("用户不存在: " + userId);
            user.setStatus(status);
            if (status == 1) resetLoginFailures(user);
            user.setUpdatedAt(now);
            userMapper.updateById(user);
            if (status == 0) clearManagerForUser(userId, now);
        }
    }

    public void updateSystemUser(Long id, String realName, String role, Integer status, String password) {
        updateSystemUser(id, realName, role, status, password, null, false);
    }

    public void updateSystemUser(Long id, String realName, String role, Integer status, String password, Long deptId, boolean updateDept) {
        updateSystemUser(id, realName, role, status, password, deptId, updateDept, null);
    }

    public void updateSystemUser(Long id, String realName, String role, Integer status, String password, Long deptId, boolean updateDept, String operatorUsername) {
        updateSystemUserDetails(id, realName, null, null, null, null, role, status, password, deptId, updateDept, operatorUsername);
    }

    public void updateSystemUserDetails(Long id, String realName, String phone, String idCard, String email, Integer forcePasswordChange,
                                        String role, Integer status, String password, Long deptId, boolean updateDept, String operatorUsername) {
        assertUserWritable(id, operatorUsername);
        if (updateDept) assertDeptWritable(deptId, operatorUsername);
        User user = userMapper.selectById(id);
        if (user == null) throw new RuntimeException("用户不存在");
        Long oldDeptId = user.getDeptId();
        if (role != null && !role.isBlank()) {
            user.setRole("admin".equals(role) ? "admin" : "user");
        }
        if (status != null) {
            if (status != 0 && status != 1) throw new RuntimeException("状态不正确");
            user.setStatus(status);
            if (status == 1) resetLoginFailures(user);
        }
        if (realName != null) {
            user.setRealName(realName.trim());
        }
        if (phone != null) {
            user.setPhone(normalizeText(phone));
        }
        if (idCard != null) {
            user.setIdCard(normalizeText(idCard));
        }
        if (email != null) {
            user.setEmail(normalizeText(email));
        }
        if (forcePasswordChange != null) {
            user.setForcePasswordChange(forcePasswordChange == 1 ? 1 : 0);
        }
        if (password != null && !password.isBlank()) {
            validatePassword(password);
            user.setPassword(passwordEncoder.encode(password));
            resetLoginFailures(user);
            user.setPasswordUpdatedAt(LocalDateTime.now());
        }
        if (updateDept) {
            if (deptId != null && departmentMapper.selectById(deptId) == null) throw new RuntimeException("部门不存在");
            user.setDeptId(deptId);
        }
        LocalDateTime now = LocalDateTime.now();
        user.setUpdatedAt(now);
        userMapper.updateById(user);
        if (role != null && !role.isBlank()) syncLegacyUserRoles(id, user.getRole());
        if (status != null && status == 0) clearManagerForUser(id, now);
        if (updateDept && oldDeptId != null && !oldDeptId.equals(deptId)) {
            Department oldDept = departmentMapper.selectById(oldDeptId);
            if (oldDept != null && id.equals(oldDept.getManagerUserId())) {
                oldDept.setManagerUserId(null);
                oldDept.setUpdatedAt(now);
                departmentMapper.updateById(oldDept);
            }
        }
    }

    public void assertUserReadable(Long userId, String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) return;
        User operator = findByUsername(operatorUsername);
        if (operator == null) throw new RuntimeException("当前用户不存在");
        String rule = roleService.getDataScopeRuleForUser(operatorUsername, "system_user");
        if ("ALL".equals(rule)) return;
        User target = userMapper.selectById(userId);
        if (target == null) throw new RuntimeException("用户不存在: " + userId);
        if ("OWN".equals(rule)) {
            if (!operator.getId().equals(target.getId())) throw new PermissionDeniedException("无权查看当前数据范围外的用户");
            return;
        }
        Set<Long> visibleDeptIds = departmentService.visibleDeptIds(operatorUsername);
        if (target.getDeptId() == null || visibleDeptIds == null || !visibleDeptIds.contains(target.getDeptId())) {
            throw new PermissionDeniedException("无权查看当前数据范围外的用户");
        }
    }

    public void assertUserWritable(Long userId, String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) return;
        User operator = findByUsername(operatorUsername);
        if (operator == null) throw new RuntimeException("当前用户不存在");
        String rule = roleService.getDataScopeRuleForUser(operatorUsername, "system_user");
        if ("ALL".equals(rule)) return;
        User target = userMapper.selectById(userId);
        if (target == null) throw new RuntimeException("用户不存在: " + userId);
        if ("OWN".equals(rule)) {
            if (!operator.getId().equals(target.getId())) throw new PermissionDeniedException("无权操作当前数据范围外的用户");
            return;
        }
        Set<Long> visibleDeptIds = departmentService.visibleDeptIds(operatorUsername);
        if (target.getDeptId() == null || visibleDeptIds == null || !visibleDeptIds.contains(target.getDeptId())) {
            throw new PermissionDeniedException("无权操作当前数据范围外的用户");
        }
    }

    private void assertDeptWritable(Long deptId, String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) return;
        String rule = roleService.getDataScopeRuleForUser(operatorUsername, "system_user");
        if ("ALL".equals(rule)) return;
        Set<Long> visibleDeptIds = departmentService.visibleDeptIds(operatorUsername);
        if (deptId == null || visibleDeptIds == null || !visibleDeptIds.contains(deptId)) {
            throw new PermissionDeniedException("无权操作当前数据范围外的部门");
        }
    }

    private void clearManagerForUser(Long userId, LocalDateTime now) {
        LambdaQueryWrapper<Department> q = new LambdaQueryWrapper<>();
        q.eq(Department::getManagerUserId, userId);
        for (Department dept : departmentMapper.selectList(q)) {
            dept.setManagerUserId(null);
            dept.setUpdatedAt(now);
            departmentMapper.updateById(dept);
        }
    }

    private void validatePassword(String password) {
        int minLength = passwordMinLength();
        if (password == null || password.length() < minLength) {
            throw new RuntimeException("密码至少" + minLength + "位");
        }
        if (systemConfigService.getBoolean("security.password.require_strong", false) && !isStrongPassword(password)) {
            throw new RuntimeException("密码需包含大小写字母、数字或特殊字符中的至少三类");
        }
    }

    private int passwordMinLength() {
        BigDecimal value = systemConfigService.getNumber("security.password.min_length", BigDecimal.valueOf(6));
        int minLength = value == null ? 6 : value.intValue();
        return Math.max(1, minLength);
    }

    private boolean passwordRequireStrong() {
        return systemConfigService.getBoolean("security.password.require_strong", false);
    }

    private boolean isStrongPassword(String password) {
        int categories = 0;
        if (password.matches(".*[a-z].*")) categories++;
        if (password.matches(".*[A-Z].*")) categories++;
        if (password.matches(".*\\d.*")) categories++;
        if (password.matches(".*[^a-zA-Z0-9].*")) categories++;
        return categories >= 3;
    }

    private void syncLegacyUserRoles(Long userId, String role) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        Role target = findRoleByCode("admin".equals(role) ? "admin" : "user");
        if (target == null) return;
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(target.getId());
        userRole.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(userRole);
    }

    private Role findRoleByCode(String code) {
        LambdaQueryWrapper<Role> q = new LambdaQueryWrapper<>();
        q.eq(Role::getRoleCode, code).or().eq(Role::getRoleName, code);
        return roleMapper.selectOne(q);
    }

    private List<Long> getUserRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))
                .stream().map(UserRole::getRoleId).toList();
    }

    private List<String> getUserRoleNames(Long userId) {
        List<String> names = new ArrayList<>();
        for (UserRole userRole : userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId))) {
            Role role = roleMapper.selectById(userRole.getRoleId());
            if (role != null) names.add(role.getRoleName());
        }
        return names;
    }

    private String valueAt(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) return "";
        return values.get(index) == null ? "" : values.get(index).trim();
    }

    private String normalizeText(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String maskPhone(String value) {
        String text = normalizeText(value);
        if (text == null) return null;
        if (text.length() < 7) return text.charAt(0) + "****";
        return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
    }

    private String maskIdCard(String value) {
        String text = normalizeText(value);
        if (text == null) return null;
        if (text.length() <= 8) return text.substring(0, Math.min(2, text.length())) + "********";
        return text.substring(0, 6) + "********" + text.substring(text.length() - 4);
    }

    private Long resolveImportDeptId(String deptName) {
        String name = deptName == null ? "" : deptName.trim();
        if (name.isEmpty() || "未分配".equals(name)) return null;
        List<Department> departments = departmentMapper.selectList(new LambdaQueryWrapper<Department>().eq(Department::getName, name));
        if (departments.isEmpty()) throw new RuntimeException("所属部门不存在：" + name);
        if (departments.size() > 1) throw new RuntimeException("所属部门名称重复，请先整理部门：" + name);
        Department department = departments.get(0);
        if (department.getStatus() != null && department.getStatus() == 0) {
            throw new RuntimeException("所属部门已禁用：" + name);
        }
        return department.getId();
    }

    private Integer parseImportStatus(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return 1;
        if ("1".equals(text) || "启用".equals(text) || "是".equals(text) || "true".equalsIgnoreCase(text)) return 1;
        if ("0".equals(text) || "禁用".equals(text) || "否".equals(text) || "false".equalsIgnoreCase(text)) return 0;
        throw new RuntimeException("状态仅支持：启用/禁用/1/0");
    }

    private Integer parseImportBoolean(String value, Integer defaultValue, String fieldName) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) return defaultValue;
        if ("1".equals(text) || "是".equals(text) || "true".equalsIgnoreCase(text) || "Y".equalsIgnoreCase(text)) return 1;
        if ("0".equals(text) || "否".equals(text) || "false".equalsIgnoreCase(text) || "N".equalsIgnoreCase(text)) return 0;
        throw new RuntimeException(fieldName + "仅支持：是/否/1/0");
    }

}
