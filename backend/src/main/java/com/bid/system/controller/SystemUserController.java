package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.config.RequirePermission;
import com.bid.system.exception.PermissionDeniedException;
import com.bid.system.service.ExcelExportService;
import com.bid.system.service.ExcelImportService;
import com.bid.system.service.OperationLogService;
import com.bid.system.service.RoleService;
import com.bid.system.service.UserService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/users")
public class SystemUserController {

    private final UserService userService;
    private final RoleService roleService;
    private final OperationLogService operationLogService;
    private final ExcelExportService excelExportService;
    private final ExcelImportService excelImportService;

    public SystemUserController(UserService userService,
                                RoleService roleService,
                                OperationLogService operationLogService,
                                ExcelExportService excelExportService,
                                ExcelImportService excelImportService) {
        this.userService = userService;
        this.roleService = roleService;
        this.operationLogService = operationLogService;
        this.excelExportService = excelExportService;
        this.excelImportService = excelImportService;
    }

    @GetMapping
    @RequirePermission("system_user:read")
    public ApiResponse list(@RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "20") int size,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "deptId", required = false) String deptId,
                            @RequestParam(value = "status", required = false) Integer status,
                            @RequestParam(value = "role", required = false) String role,
                            @RequestParam(value = "roleId", required = false) Long roleId,
                            HttpServletRequest request) {
        try {
            boolean unassigned = "unassigned".equals(deptId);
            Long parsedDeptId = null;
            if (deptId != null && !deptId.isBlank() && !unassigned) {
                parsedDeptId = Long.valueOf(deptId);
            }
            return ApiResponse.success(userService.getSystemUsers(page, size, keyword, parsedDeptId, unassigned, status, role, roleId, (String) request.getAttribute("username")));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/export")
    @RequirePermission("system_user:export")
    public ResponseEntity<byte[]> export(@RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "deptId", required = false) String deptId,
                                         @RequestParam(value = "status", required = false) Integer status,
                                         @RequestParam(value = "role", required = false) String role,
                                         @RequestParam(value = "roleId", required = false) Long roleId,
                                         HttpServletRequest request) {
        boolean unassigned = "unassigned".equals(deptId);
        Long parsedDeptId = null;
        try {
            if (deptId != null && !deptId.isBlank() && !unassigned) {
                parsedDeptId = Long.valueOf(deptId);
            }
            String operatorUsername = (String) request.getAttribute("username");
            Map<String, Object> exportData = userService.exportSystemUsers(keyword, parsedDeptId, unassigned, status, role, roleId, operatorUsername);
            byte[] content = excelExportService.export("用户列表", buildExportHeaders(exportData), buildExportRows(exportData));
            String fileName = "用户列表_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "USER", "EXPORT", "导出用户列表");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "EXPORT", "导出用户列表失败", false, e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "EXPORT", "导出用户列表失败", false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/template")
    @RequirePermission("system_user:import")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] content = excelImportService.template("用户导入模板", buildImportHeaders(), buildImportSampleRows());
        String fileName = "用户导入模板.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping("/import")
    @RequirePermission("system_user:import")
    public ApiResponse importUsers(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            Map<String, Object> result = userService.importSystemUsers(
                    excelImportService.readRows(file, buildImportHeaders().size()),
                    (String) request.getAttribute("username")
            );
            operationLogService.record(request, "USER", "IMPORT", "导入用户：" + importSummary(result));
            return ApiResponse.success(result);
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "IMPORT", "导入用户失败", false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "IMPORT", "导入用户失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping
    @RequirePermission("system_user:create")
    public ApiResponse add(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String username = (String) params.get("username");
            String password = (String) params.get("password");
            String realName = (String) params.get("realName");
            String phone = optionalStringParam(params, "phone");
            String idCard = optionalStringParam(params, "idCard");
            String email = optionalStringParam(params, "email");
            String role = params.get("role") != null ? (String) params.get("role") : "user";
            Long deptId = parseLong(params.get("deptId"));
            Integer status = params.get("status") != null ? ((Number) params.get("status")).intValue() : 1;
            Integer forcePasswordChange = parseBinaryFlag(params.get("forcePasswordChange"));
            assertCanWriteField(request, "username", "无账号字段写入权限");
            assertCanWriteField(request, "realName", "无姓名字段写入权限");
            assertOptionalWriteField(request, params, "phone", "phone", "无手机号字段写入权限");
            assertOptionalWriteField(request, params, "idCard", "idCard", "无身份证号字段写入权限");
            assertOptionalWriteField(request, params, "email", "email", "无邮箱字段写入权限");
            assertOptionalWriteField(request, params, "forcePasswordChange", "forcePasswordChange", "无强制改密字段写入权限");
            if ((params.get("roleIds") instanceof List<?>) || "admin".equals(role)) {
                assertHasFeature(request, "system_user:roles", "无分配角色权限");
                assertCanWriteField(request, "roles", "无角色字段写入权限");
            }
            if (params.containsKey("deptId") && deptId != null) {
                assertHasFeature(request, "system_user:dept", "无调整部门权限");
                assertCanWriteField(request, "dept", "无部门字段写入权限");
            }
            if (params.containsKey("status")) {
                assertHasFeature(request, "system_user:status", "无启停用户权限");
                assertCanWriteField(request, "status", "无状态字段写入权限");
            }
            String operatorUsername = (String) request.getAttribute("username");
            var user = userService.createSystemUser(username, password, realName, phone, idCard, email, role, deptId, status, forcePasswordChange, operatorUsername);
            if (params.get("roleIds") instanceof List<?>) {
                roleService.assignUserRoles(user.getId(), parseLongList(params.get("roleIds")), operatorUsername);
            }
            operationLogService.record(request, "USER", "CREATE", "新增用户：" + username);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "CREATE", "新增用户失败", false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "CREATE", "新增用户失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequirePermission("system_user:update")
    public ApiResponse update(@PathVariable("id") Long id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String realName = (String) params.get("realName");
            String phone = optionalStringParam(params, "phone");
            String idCard = optionalStringParam(params, "idCard");
            String email = optionalStringParam(params, "email");
            String role = (String) params.get("role");
            Integer status = params.get("status") != null ? ((Number) params.get("status")).intValue() : null;
            Integer forcePasswordChange = parseBinaryFlag(params.get("forcePasswordChange"));
            Long deptId = parseLong(params.get("deptId"));
            if (params.containsKey("realName")) {
                assertCanWriteField(request, "realName", "无姓名字段写入权限");
            }
            assertOptionalWriteField(request, params, "phone", "phone", "无手机号字段写入权限");
            assertOptionalWriteField(request, params, "idCard", "idCard", "无身份证号字段写入权限");
            assertOptionalWriteField(request, params, "email", "email", "无邮箱字段写入权限");
            assertOptionalWriteField(request, params, "forcePasswordChange", "forcePasswordChange", "无强制改密字段写入权限");
            if (params.get("roleIds") instanceof List<?>) {
                assertHasFeature(request, "system_user:roles", "无分配角色权限");
                assertCanWriteField(request, "roles", "无角色字段写入权限");
                roleService.assertCanAssignUserRoles(id, parseLongList(params.get("roleIds")), (String) request.getAttribute("username"));
            } else {
                if (role != null) {
                    assertHasFeature(request, "system_user:roles", "无分配角色权限");
                    assertCanWriteField(request, "roles", "无角色字段写入权限");
                }
                assertCanUpdateLegacyRole(id, role, request);
            }
            if (params.containsKey("deptId")) {
                assertHasFeature(request, "system_user:dept", "无调整部门权限");
                assertCanWriteField(request, "dept", "无部门字段写入权限");
            }
            if (params.containsKey("status")) {
                assertHasFeature(request, "system_user:status", "无启停用户权限");
                assertCanWriteField(request, "status", "无状态字段写入权限");
            }
            userService.updateSystemUserDetails(id, realName, phone, idCard, email, forcePasswordChange, role, status, null, deptId, params.containsKey("deptId"), (String) request.getAttribute("username"));
            if (params.get("roleIds") instanceof List<?>) {
                roleService.assignUserRoles(id, parseLongList(params.get("roleIds")), (String) request.getAttribute("username"));
            }
            operationLogService.record(request, "USER", "UPDATE", "编辑用户：" + id);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "UPDATE", "编辑用户失败：" + id, false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "UPDATE", "编辑用户失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/roles")
    @RequirePermission("system_user:read")
    public ApiResponse userRoles(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            userService.assertUserReadable(id, (String) request.getAttribute("username"));
            return ApiResponse.success(roleService.getUserRoles(id));
        } catch (PermissionDeniedException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/roles")
    @RequirePermission("system_user:roles")
    public ApiResponse assignRoles(@PathVariable("id") Long id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Object rawIds = params.get("roleIds");
            if (!(rawIds instanceof List<?>)) return ApiResponse.error(400, "请选择角色");
            assertCanWriteField(request, "roles", "无角色字段写入权限");
            userService.assertUserWritable(id, (String) request.getAttribute("username"));
            roleService.assignUserRoles(id, parseLongList(rawIds), (String) request.getAttribute("username"));
            operationLogService.record(request, "USER", "ASSIGN_ROLES", "分配用户角色：" + id);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "ASSIGN_ROLES", "分配用户角色失败：" + id, false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "ASSIGN_ROLES", "分配用户角色失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/password")
    @RequirePermission("system_user:password")
    public ApiResponse updatePassword(@PathVariable("id") Long id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String password = (String) params.get("password");
            Integer forcePasswordChange = parseBinaryFlag(params.get("forcePasswordChange"));
            if (params.containsKey("forcePasswordChange")) {
                assertCanWriteField(request, "forcePasswordChange", "无强制改密字段写入权限");
            }
            userService.updatePassword(id, password, forcePasswordChange, (String) request.getAttribute("username"));
            operationLogService.record(request, "USER", "RESET_PASSWORD", "重置用户密码：" + id);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "RESET_PASSWORD", "重置用户密码失败：" + id, false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "RESET_PASSWORD", "重置用户密码失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @RequirePermission("system_user:status")
    public ApiResponse updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Integer status = params.get("status") != null ? ((Number) params.get("status")).intValue() : null;
            assertCanWriteField(request, "status", "无状态字段写入权限");
            userService.updateUsersStatus(List.of(id), status, (String) request.getAttribute("username"));
            operationLogService.record(request, "USER", status != null && status == 1 ? "ENABLE" : "DISABLE", "启停用户：" + id);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "STATUS", "启停用户失败：" + id, false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "STATUS", "启停用户失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/status")
    @RequirePermission("system_user:status")
    public ApiResponse updateUsersStatus(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Object rawIds = params.get("userIds");
            if (!(rawIds instanceof List<?>)) {
                return ApiResponse.error(400, "请选择用户");
            }
            List<Long> userIds = ((List<?>) rawIds).stream()
                    .map(v -> ((Number) v).longValue())
                    .toList();
            Integer status = params.get("status") != null ? ((Number) params.get("status")).intValue() : null;
            assertCanWriteField(request, "status", "无状态字段写入权限");
            userService.updateUsersStatus(userIds, status, (String) request.getAttribute("username"));
            operationLogService.record(request, "USER", status != null && status == 1 ? "ENABLE" : "DISABLE", "批量启停用户，用户数：" + userIds.size());
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "STATUS", "批量启停用户失败", false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "STATUS", "批量启停用户失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/dept")
    @RequirePermission("system_user:dept")
    public ApiResponse moveUsers(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            Object rawIds = params.get("userIds");
            if (!(rawIds instanceof List<?>)) {
                return ApiResponse.error(400, "请选择用户");
            }
            List<Long> userIds = ((List<?>) rawIds).stream()
                    .map(v -> ((Number) v).longValue())
                    .toList();
            assertCanWriteField(request, "dept", "无部门字段写入权限");
            userService.moveUsersToDept(userIds, parseLong(params.get("deptId")), (String) request.getAttribute("username"));
            operationLogService.record(request, "USER", "MOVE_DEPT", "调整用户部门，用户数：" + userIds.size() + "，目标部门：" + parseLong(params.get("deptId")));
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "MOVE_DEPT", "调整用户部门失败", false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "MOVE_DEPT", "调整用户部门失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equals(text) || "unassigned".equals(text)) return null;
        return Long.valueOf(text);
    }

    private String optionalStringParam(Map<String, Object> params, String key) {
        if (!params.containsKey(key)) return null;
        Object value = params.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer parseBinaryFlag(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        if (value instanceof Number) return ((Number) value).intValue() == 1 ? 1 : 0;
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        if ("1".equals(text) || "true".equalsIgnoreCase(text) || "是".equals(text)) return 1;
        if ("0".equals(text) || "false".equalsIgnoreCase(text) || "否".equals(text)) return 0;
        throw new RuntimeException("布尔字段仅支持：是/否/1/0");
    }

    private List<Long> parseLongList(Object rawIds) {
        return ((List<?>) rawIds).stream()
                .filter(v -> v != null)
                .map(v -> ((Number) v).longValue())
                .toList();
    }

    private void assertCanUpdateLegacyRole(Long id, String role, HttpServletRequest request) {
        if (role == null || "admin".equals(role)) return;
        var target = userService.getById(id);
        String username = (String) request.getAttribute("username");
        if (target != null && username != null && username.equals(target.getUsername()) && "admin".equals(target.getRole())) {
            throw new RuntimeException("不能移除当前登录管理员自己的管理员角色");
        }
    }

    private void assertHasFeature(HttpServletRequest request, String featureCode, String message) {
        String username = (String) request.getAttribute("username");
        String role = (String) request.getAttribute("role");
        if (!roleService.hasFeature(username, role, featureCode)) {
            throw new PermissionDeniedException(message);
        }
    }

    private void assertCanWriteField(HttpServletRequest request, String fieldCode, String message) {
        String username = (String) request.getAttribute("username");
        if (!roleService.hasWritableField(username, "system_user", fieldCode)) {
            throw new PermissionDeniedException(message);
        }
    }

    private void assertOptionalWriteField(HttpServletRequest request, Map<String, Object> params, String paramKey, String fieldCode, String message) {
        if (params.containsKey(paramKey)) {
            assertCanWriteField(request, fieldCode, message);
        }
    }

    private List<String> buildExportHeaders(Map<String, Object> exportData) {
        List<String> headers = new ArrayList<>();
        Map<String, String> fieldLabels = fieldLabels();
        List<String> readableFields = castStringList(exportData.get("readableFields"));
        for (String fieldCode : List.of("realName", "username", "phone", "idCard", "email", "dept", "roles", "status", "forcePasswordChange", "loginSecurity", "lastLoginIp", "passwordUpdatedAt")) {
            if (readableFields.contains(fieldCode)) {
                headers.add(fieldLabels.get(fieldCode));
            }
        }
        headers.add("最后登录时间");
        headers.add("创建时间");
        return headers;
    }

    private List<String> buildImportHeaders() {
        return List.of("账号", "姓名", "初始密码", "手机号", "身份证号", "邮箱", "所属部门", "状态(启用/禁用)", "强制改密(是/否)");
    }

    private List<List<String>> buildImportSampleRows() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("zhangsan@bid.com", "张三", "abc123", "13800000001", "110101199001010011", "zhangsan@bid.com", "财务部", "启用", "否"));
        rows.add(List.of("lisi@bid.com", "李四", "abc123", "13800000002", "", "lisi@bid.com", "未分配", "启用", "是"));
        return rows;
    }

    private List<List<String>> buildExportRows(Map<String, Object> exportData) {
        List<List<String>> rows = new ArrayList<>();
        List<?> records = exportData.get("records") instanceof List<?> list ? list : List.of();
        List<String> readableFields = castStringList(exportData.get("readableFields"));
        for (Object recordObj : records) {
            if (!(recordObj instanceof Map<?, ?> rawRecord)) continue;
            Map<String, Object> record = new LinkedHashMap<>();
            rawRecord.forEach((key, value) -> record.put(String.valueOf(key), value));
            List<String> row = new ArrayList<>();
            if (readableFields.contains("realName")) row.add(stringValue(record.get("realName")));
            if (readableFields.contains("username")) row.add(stringValue(record.get("username")));
            if (readableFields.contains("phone")) row.add(stringValue(record.get("phone")));
            if (readableFields.contains("idCard")) row.add(stringValue(record.get("idCard")));
            if (readableFields.contains("email")) row.add(stringValue(record.get("email")));
            if (readableFields.contains("dept")) row.add(stringValue(record.get("deptName"), "未分配"));
            if (readableFields.contains("roles")) row.add(joinNames(record.get("roleNames")));
            if (readableFields.contains("status")) row.add(Integer.valueOf(1).equals(record.get("status")) ? "启用" : "禁用");
            if (readableFields.contains("forcePasswordChange")) row.add(Integer.valueOf(1).equals(record.get("forcePasswordChange")) ? "是" : "否");
            if (readableFields.contains("loginSecurity")) {
                row.add("失败 " + stringValue(record.get("failedLoginCount"), "0") + " 次 / " + stringValue(record.get("lastFailedLoginAt"), "无失败记录"));
            }
            if (readableFields.contains("lastLoginIp")) row.add(stringValue(record.get("lastLoginIp")));
            if (readableFields.contains("passwordUpdatedAt")) row.add(formatDateTime(record.get("passwordUpdatedAt")));
            row.add(formatDateTime(record.get("lastLoginAt")));
            row.add(formatDateTime(record.get("createdAt")));
            rows.add(row);
        }
        return rows;
    }

    private List<String> castStringList(Object value) {
        if (!(value instanceof List<?> raw)) return List.of();
        return raw.stream().map(String::valueOf).toList();
    }

    private Map<String, String> fieldLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("realName", "姓名");
        labels.put("username", "账号");
        labels.put("phone", "手机号");
        labels.put("idCard", "身份证号");
        labels.put("email", "邮箱");
        labels.put("dept", "所属部门");
        labels.put("roles", "角色");
        labels.put("status", "状态");
        labels.put("forcePasswordChange", "强制改密");
        labels.put("loginSecurity", "登录安全");
        labels.put("lastLoginIp", "最近登录 IP");
        labels.put("passwordUpdatedAt", "密码更新时间");
        return labels;
    }

    private String joinNames(Object value) {
        if (value instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(String::valueOf).toList().stream().reduce((a, b) -> a + "、" + b).orElse("-");
        }
        return "-";
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

    private String importSummary(Map<String, Object> result) {
        return "成功 " + stringValue(result.get("successCount"), "0")
                + "，跳过 " + stringValue(result.get("skippedCount"), "0")
                + "，失败 " + stringValue(result.get("failureCount"), "0");
    }
}
