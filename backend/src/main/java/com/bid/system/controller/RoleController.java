package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.config.RequirePermission;
import com.bid.system.exception.PermissionDeniedException;
import com.bid.system.service.ExcelExportService;
import com.bid.system.service.OperationLogService;
import com.bid.system.service.RoleService;
import com.bid.system.service.UserService;
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
@RequestMapping("/api/system/roles")
public class RoleController {

    private final RoleService roleService;
    private final OperationLogService operationLogService;
    private final UserService userService;
    private final ExcelExportService excelExportService;

    public RoleController(RoleService roleService,
                          OperationLogService operationLogService,
                          UserService userService,
                          ExcelExportService excelExportService) {
        this.roleService = roleService;
        this.operationLogService = operationLogService;
        this.userService = userService;
        this.excelExportService = excelExportService;
    }

    @GetMapping
    @RequirePermission("system_role:read")
    public ApiResponse list() {
        return ApiResponse.success(roleService.listRoles());
    }

    @GetMapping("/export")
    @RequirePermission("system_role:export")
    public ResponseEntity<byte[]> export(HttpServletRequest request) {
        try {
            List<Map<String, Object>> roles = roleService.listRoles();
            byte[] content = excelExportService.export("角色列表", exportHeaders(), exportRows(roles));
            String fileName = "角色列表_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "ROLE", "EXPORT", "导出角色列表");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (RuntimeException e) {
            operationLogService.record(request, "ROLE", "EXPORT", "导出角色列表失败", false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/permission-catalog")
    @RequirePermission("system_role:read")
    public ApiResponse permissionCatalog() {
        return ApiResponse.success(roleService.getPermissionCatalog());
    }

    @PostMapping
    @RequirePermission("system_role:create")
    public ApiResponse add(@RequestBody Map<String, Object> map, HttpServletRequest request) {
        try {
            String roleCode = (String) map.get("roleCode");
            String roleName = (String) map.get("roleName");
            String description = (String) map.get("description");
            Integer status = map.get("status") != null ? ((Number) map.get("status")).intValue() : 1;
            roleService.addRole(roleCode, roleName, description, status);
            operationLogService.record(request, "ROLE", "CREATE", "新增角色：" + roleName + "（" + roleCode + "）");
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "ROLE", "CREATE", "新增角色失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequirePermission("system_role:update")
    public ApiResponse update(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        try {
            String roleName = null;
            String description = null;
            Integer status = null;
            if (map.get("roleName") != null) {
                roleName = (String) map.get("roleName");
            }
            if (map.get("description") != null) {
                description = (String) map.get("description");
            }
            if (map.get("status") != null) {
                status = ((Number) map.get("status")).intValue();
            }
            roleService.updateRole(id, roleName, description, status);
            operationLogService.record(request, "ROLE", "UPDATE", "编辑角色：" + id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "ROLE", "UPDATE", "编辑角色失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system_role:delete")
    public ApiResponse delete(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            roleService.deleteRole(id);
            operationLogService.record(request, "ROLE", "DELETE", "删除角色：" + id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "ROLE", "DELETE", "删除角色失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/disable-impact")
    @RequirePermission("system_role:update")
    public ApiResponse disableImpact(@PathVariable("id") Long id) {
        try {
            return ApiResponse.success(roleService.getRoleDisableImpact(id));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/delete-impact")
    @RequirePermission("system_role:delete")
    public ApiResponse deleteImpact(@PathVariable("id") Long id) {
        try {
            return ApiResponse.success(roleService.getRoleDeleteImpact(id));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/permissions")
    @RequirePermission("system_role:read")
    public ApiResponse permissions(@PathVariable("id") Long id) {
        try {
            return ApiResponse.success(roleService.getPermissions(id));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/features")
    @RequirePermission("system_role:permission")
    public ApiResponse updateFeatures(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        try {
            roleService.saveFeatures(id, stringList(map.get("features")));
            operationLogService.record(request, "ROLE", "SAVE_FEATURES", "保存角色功能权限：" + id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "ROLE", "SAVE_FEATURES", "保存角色功能权限失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/data-scopes")
    @RequirePermission("system_role:permission")
    public ApiResponse updateDataScopes(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        try {
            roleService.saveDataScopes(id, mapList(map.get("dataScopes")));
            operationLogService.record(request, "ROLE", "SAVE_DATA_SCOPE", "保存角色数据范围：" + id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "ROLE", "SAVE_DATA_SCOPE", "保存角色数据范围失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/fields")
    @RequirePermission("system_role:permission")
    public ApiResponse updateFields(@PathVariable("id") Long id, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        try {
            roleService.saveFields(id, mapList(map.get("fields")));
            operationLogService.record(request, "ROLE", "SAVE_FIELDS", "保存角色字段权限：" + id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "ROLE", "SAVE_FIELDS", "保存角色字段权限失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/users/{userId}")
    @RequirePermission("system_user:read")
    public ApiResponse userRoles(@PathVariable("userId") Long userId, HttpServletRequest request) {
        try {
            userService.assertUserReadable(userId, (String) request.getAttribute("username"));
            return ApiResponse.success(roleService.getUserRoles(userId));
        } catch (PermissionDeniedException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/users/{userId}")
    @RequirePermission("system_user:roles")
    public ApiResponse assignUserRoles(@PathVariable("userId") Long userId, @RequestBody Map<String, Object> map, HttpServletRequest request) {
        try {
            Object rawIds = map.get("roleIds");
            if (!(rawIds instanceof List<?>)) return ApiResponse.error(400, "请选择角色");
            List<Long> roleIds = ((List<?>) rawIds).stream().map(v -> ((Number) v).longValue()).toList();
            assertCanWriteUserField(request, "roles", "无角色字段写入权限");
            userService.assertUserWritable(userId, (String) request.getAttribute("username"));
            roleService.assignUserRoles(userId, roleIds, (String) request.getAttribute("username"));
            operationLogService.record(request, "USER", "ASSIGN_ROLES", "分配用户角色：" + userId);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "ASSIGN_ROLES", "分配用户角色失败：" + userId, false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "ASSIGN_ROLES", "分配用户角色失败：" + userId, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private List<String> stringList(Object raw) {
        if (!(raw instanceof List<?>)) return List.of();
        return ((List<?>) raw).stream().filter(v -> v != null).map(String::valueOf).toList();
    }

    private void assertCanWriteUserField(HttpServletRequest request, String fieldCode, String message) {
        String username = (String) request.getAttribute("username");
        if (!roleService.hasWritableField(username, "system_user", fieldCode)) {
            throw new PermissionDeniedException(message);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object raw) {
        if (!(raw instanceof List<?>)) return List.of();
        return ((List<?>) raw).stream()
                .filter(v -> v instanceof Map<?, ?>)
                .map(v -> (Map<String, Object>) v)
                .toList();
    }

    private List<String> exportHeaders() {
        return List.of("角色编码", "角色名称", "状态", "内置", "用户数", "功能权限数", "说明", "创建时间");
    }

    private List<List<String>> exportRows(List<Map<String, Object>> roles) {
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> role : roles) {
            rows.add(List.of(
                    safeText(role.get("roleCode")),
                    safeText(role.get("roleName")),
                    Integer.valueOf(1).equals(role.get("status")) ? "启用" : "禁用",
                    Integer.valueOf(1).equals(role.get("builtIn")) ? "是" : "否",
                    safeText(role.get("userCount")),
                    safeText(role.get("featureCount")),
                    safeText(role.get("description")),
                    formatDateTime(role.get("createdAt"))
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
