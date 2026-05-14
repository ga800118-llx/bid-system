package com.bid.system.controller;

import com.bid.system.config.RequirePermission;
import com.bid.system.dto.ApiResponse;
import com.bid.system.entity.User;
import com.bid.system.exception.PermissionDeniedException;
import com.bid.system.service.OperationLogService;
import com.bid.system.service.RoleService;
import com.bid.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private OperationLogService operationLogService;

    @PostMapping("/register")
    public ApiResponse register(@RequestBody User user) {
        try {
            User u = userService.register(user.getUsername(), user.getPassword());
            return ApiResponse.success(u);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody User user, HttpServletRequest request) {
        String username = safeUsername(user.getUsername());
        request.setAttribute("username", username.isEmpty() ? "anonymous" : username);
        User loginUser = userService.loginUser(username, user.getPassword(), clientIp(request));
        if (loginUser != null) {
            int sessionTimeoutMinutes = userService.sessionTimeoutMinutes();
            String token = userService.issueToken(loginUser);
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("token", token);
            data.put("userId", loginUser.getId());
            data.put("username", loginUser.getUsername());
            data.put("role", loginUser.getRole());
            data.put("forcePasswordChange", loginUser.getForcePasswordChange() == null ? 0 : loginUser.getForcePasswordChange());
            data.put("sessionTimeoutMinutes", sessionTimeoutMinutes);
            data.put("expiresInSeconds", sessionTimeoutMinutes * 60);
            operationLogService.record(request, "AUTH", "LOGIN", "登录成功：" + loginUser.getUsername());
            return ApiResponse.success(data);
        }
        String reason = userService.loginFailureReason(username, user.getPassword());
        boolean locked = userService.isAccountLockedByFailedAttempts(username);
        operationLogService.record(request, "AUTH", locked ? "ACCOUNT_LOCKED" : "LOGIN_FAILED",
                (locked ? "账号锁定：" : "登录失败：") + (username.isEmpty() ? "-" : username), false, reason);
        return ApiResponse.error(401, "用户名或密码错误");
    }

    @PostMapping("/logout")
    public ApiResponse logout(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        operationLogService.record(request, "AUTH", "LOGOUT", "退出登录：" + (username == null || username.isBlank() ? "-" : username));
        return ApiResponse.success();
    }

    @Deprecated
    @GetMapping("/list")
    @RequirePermission("system_user:read")
    public ApiResponse list(HttpServletRequest request) {
        java.util.Map<String, Object> page = userService.getSystemUsers(1, 1000, null, null, false, null, null, (String) request.getAttribute("username"));
        return ApiResponse.success(page.get("records"));
    }

    @Deprecated
    @PutMapping("/role/{id}")
    @RequirePermission("system_user:roles")
    public ApiResponse updateRole(@PathVariable("id") Long id, @RequestParam("role") String role, HttpServletRequest request) {
        try {
            User target = userService.getById(id);
            String username = (String) request.getAttribute("username");
            if (target != null && username != null && username.equals(target.getUsername()) && "admin".equals(target.getRole()) && !"admin".equals(role)) {
                return ApiResponse.error(400, "不能移除当前登录管理员自己的管理员角色");
            }
            assertCanWriteField(request, "roles", "无角色字段写入权限");
            userService.updateRole(id, role, (String) request.getAttribute("username"));
            operationLogService.record(request, "USER", "ASSIGN_ROLES", "旧接口调整用户角色：" + id);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "ASSIGN_ROLES", "旧接口调整用户角色失败：" + id, false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "ASSIGN_ROLES", "旧接口调整用户角色失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @Deprecated
    @PutMapping("/password/{id}")
    @RequirePermission("system_user:password")
    public ApiResponse updatePassword(@PathVariable("id") Long id, @RequestParam("password") String password, HttpServletRequest request) {
        try {
            userService.updatePassword(id, password, (String) request.getAttribute("username"));
            operationLogService.record(request, "USER", "RESET_PASSWORD", "旧接口重置用户密码：" + id);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "RESET_PASSWORD", "旧接口重置用户密码失败：" + id, false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "RESET_PASSWORD", "旧接口重置用户密码失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/info")
    public ApiResponse info(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        User user = userService.findByUsername(username);
        if (user != null) {
            user.setPassword(null);
            java.util.List<Long> roleIds = roleService.getUserRoleIds(user.getId());
            java.util.List<String> roleNames = roleService.getUserRoleNames(user.getId());
            java.util.List<String> permissions = roleService.getFeatureCodesForUser(username);
            java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("role", user.getRole());
            data.put("deptId", user.getDeptId());
            data.put("deptName", userService.deptNameOf(user.getDeptId()));
            data.put("status", user.getStatus());
            data.put("roleIds", roleIds);
            data.put("roleNames", roleNames);
            data.put("permissions", permissions);
            data.put("user", user);
            return ApiResponse.success(data);
        }
        return ApiResponse.error(404, "用户不存在");
    }

    @GetMapping("/profile")
    public ApiResponse profile(HttpServletRequest request) {
        try {
            return ApiResponse.success(userService.currentProfile((String) request.getAttribute("username")));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/profile/password")
    public ApiResponse changeOwnPassword(@RequestBody java.util.Map<String, Object> map, HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        try {
            String oldPassword = map.get("oldPassword") == null ? null : String.valueOf(map.get("oldPassword"));
            String newPassword = map.get("newPassword") == null ? null : String.valueOf(map.get("newPassword"));
            userService.changeOwnPassword(username, oldPassword, newPassword);
            operationLogService.record(request, "AUTH", "CHANGE_PASSWORD", "修改个人密码：" + (username == null ? "-" : username));
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "AUTH", "CHANGE_PASSWORD", "修改个人密码失败：" + (username == null ? "-" : username), false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/permissions")
    public ApiResponse permissions(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return ApiResponse.success(roleService.getFeatureCodesForUser(username));
    }

    private String safeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    private void assertCanWriteField(HttpServletRequest request, String fieldCode, String message) {
        String username = (String) request.getAttribute("username");
        if (!roleService.hasWritableField(username, "system_user", fieldCode)) {
            throw new PermissionDeniedException(message);
        }
    }
}
