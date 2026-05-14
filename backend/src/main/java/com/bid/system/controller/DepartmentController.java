package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.config.RequirePermission;
import com.bid.system.config.RequireAnyPermission;
import com.bid.system.exception.PermissionDeniedException;
import com.bid.system.service.DepartmentService;
import com.bid.system.service.ExcelExportService;
import com.bid.system.service.ExcelImportService;
import com.bid.system.service.OperationLogService;
import com.bid.system.service.RoleService;
import com.bid.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/system/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private ExcelExportService excelExportService;

    @Autowired
    private ExcelImportService excelImportService;

    @GetMapping("/tree")
    @RequirePermission("system_department:read")
    public ApiResponse tree(HttpServletRequest request) {
        return ApiResponse.success(departmentService.getTree((String) request.getAttribute("username")));
    }

    @GetMapping("/export")
    @RequirePermission("system_department:export")
    public ResponseEntity<byte[]> export(HttpServletRequest request) {
        try {
            List<Map<String, Object>> departments = departmentService.exportDepartments((String) request.getAttribute("username"));
            byte[] content = excelExportService.export("部门列表", exportHeaders(), exportRows(departments));
            String fileName = "部门列表_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";
            operationLogService.record(request, "DEPARTMENT", "EXPORT", "导出部门列表");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(content);
        } catch (RuntimeException e) {
            operationLogService.record(request, "DEPARTMENT", "EXPORT", "导出部门列表失败", false, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/template")
    @RequirePermission("system_department:import")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] content = excelImportService.template("部门导入模板", importHeaders(), importSampleRows());
        String fileName = "部门导入模板.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @PostMapping("/import")
    @RequirePermission("system_department:import")
    public ApiResponse importDepartments(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        try {
            Map<String, Object> result = departmentService.importDepartments(excelImportService.readRows(file, importHeaders().size()));
            operationLogService.record(request, "DEPARTMENT", "IMPORT", "导入部门：" + importSummary(result));
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            operationLogService.record(request, "DEPARTMENT", "IMPORT", "导入部门失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/users")
    @RequirePermission("system_department:read")
    public ApiResponse users(@PathVariable("id") Long id,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             @RequestParam(value = "size", defaultValue = "20") int size,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             HttpServletRequest request) {
        return ApiResponse.success(userService.getByDept(id, page, size, keyword, (String) request.getAttribute("username")));
    }

    @GetMapping("/unassigned/users")
    @RequirePermission("system_department:read")
    public ApiResponse unassignedUsers(@RequestParam(value = "page", defaultValue = "1") int page,
                                       @RequestParam(value = "size", defaultValue = "20") int size,
                                       @RequestParam(value = "keyword", required = false) String keyword,
                                       HttpServletRequest request) {
        return ApiResponse.success(userService.getByDept(null, page, size, keyword, (String) request.getAttribute("username")));
    }

    @GetMapping("/{id}/manager-candidates")
    @RequirePermission("system_department:manager")
    public ApiResponse managerCandidates(@PathVariable("id") Long id) {
        return ApiResponse.success(userService.getManagerCandidates(id));
    }

    @GetMapping("/{id}/delete-impact")
    @RequirePermission("system_department:read")
    public ApiResponse deleteImpact(@PathVariable("id") Long id) {
        try {
            return ApiResponse.success(departmentService.getDeleteImpact(id));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/users")
    @RequirePermission("system_user:create")
    public ApiResponse addUser(@PathVariable("id") Long id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        return addUserToDept(id, params, request);
    }

    @PostMapping("/unassigned/users")
    @RequirePermission("system_user:create")
    public ApiResponse addUnassignedUser(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        return addUserToDept(null, params, request);
    }

    @PutMapping("/users/dept")
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
            Long deptId = params.get("deptId") != null ? ((Number) params.get("deptId")).longValue() : null;
            assertCanWriteField(request, "dept", "无部门字段写入权限");
            userService.moveUsersToDept(userIds, deptId, (String) request.getAttribute("username"));
            operationLogService.record(request, "USER", "MOVE_DEPT", "部门内调整用户部门，用户数：" + userIds.size() + "，目标部门：" + deptId);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "MOVE_DEPT", "部门内调整用户部门失败", false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "MOVE_DEPT", "部门内调整用户部门失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/users/{id}")
    @RequirePermission("system_user:update")
    public ApiResponse updateUser(@PathVariable("id") Long id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String realName = (String) params.get("realName");
            String role = (String) params.get("role");
            Object rawRoleIds = params.get("roleIds");
            Integer status = params.get("status") != null ? ((Number) params.get("status")).intValue() : null;
            String password = (String) params.get("password");
            if (params.containsKey("realName")) {
                assertCanWriteField(request, "realName", "无姓名字段写入权限");
            }
            if (rawRoleIds instanceof List<?> || params.containsKey("role")) {
                assertHasFeature(request, "system_user:roles", "无分配角色权限");
                assertCanWriteField(request, "roles", "无角色字段写入权限");
                if (rawRoleIds instanceof List<?>) {
                    roleService.assertCanAssignUserRoles(id, parseLongList(rawRoleIds), (String) request.getAttribute("username"));
                } else {
                    assertCanUpdateLegacyRole(id, role, request);
                }
            }
            if (params.containsKey("status")) {
                assertHasFeature(request, "system_user:status", "无启停用户权限");
                assertCanWriteField(request, "status", "无状态字段写入权限");
            }
            if (password != null && !password.isBlank()) {
                assertHasFeature(request, "system_user:password", "无重置密码权限");
            }
            userService.updateSystemUser(id, realName, role, status, password, null, false, (String) request.getAttribute("username"));
            if (rawRoleIds instanceof List<?>) {
                roleService.assignUserRoles(id, parseLongList(rawRoleIds), (String) request.getAttribute("username"));
            }
            operationLogService.record(request, "USER", "UPDATE", "部门内编辑用户：" + id);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "UPDATE", "部门内编辑用户失败：" + id, false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "UPDATE", "部门内编辑用户失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/users/status")
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
            operationLogService.record(request, "USER", status != null && status == 1 ? "ENABLE" : "DISABLE", "部门内批量启停用户，用户数：" + userIds.size());
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "STATUS", "部门内批量启停用户失败", false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "STATUS", "部门内批量启停用户失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private ApiResponse addUserToDept(Long deptId, Map<String, Object> params, HttpServletRequest request) {
        try {
            String username = (String) params.get("username");
            String password = (String) params.get("password");
            String realName = (String) params.get("realName");
            String role = params.get("role") != null ? (String) params.get("role") : "user";
            Object rawRoleIds = params.get("roleIds");
            Integer status = params.get("status") != null ? ((Number) params.get("status")).intValue() : 1;
            assertCanWriteField(request, "username", "无账号字段写入权限");
            assertCanWriteField(request, "realName", "无姓名字段写入权限");
            if (deptId != null) {
                assertHasFeature(request, "system_user:dept", "无调整部门权限");
                assertCanWriteField(request, "dept", "无部门字段写入权限");
            }
            if (rawRoleIds instanceof List<?> || params.containsKey("role")) {
                assertHasFeature(request, "system_user:roles", "无分配角色权限");
                assertCanWriteField(request, "roles", "无角色字段写入权限");
            }
            if (params.containsKey("status")) {
                assertHasFeature(request, "system_user:status", "无启停用户权限");
                assertCanWriteField(request, "status", "无状态字段写入权限");
            }
            var user = userService.createSystemUser(username, password, realName, role, deptId, status, (String) request.getAttribute("username"));
            if (rawRoleIds instanceof List<?>) {
                roleService.assignUserRoles(user.getId(), parseLongList(rawRoleIds), (String) request.getAttribute("username"));
            }
            operationLogService.record(request, "USER", "CREATE", "部门内新增用户：" + username + "，部门：" + deptId);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "USER", "CREATE", "部门内新增用户失败", false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "USER", "CREATE", "部门内新增用户失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping
    @RequirePermission("system_department:create")
    public ApiResponse add(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String name = (String) params.get("name");
            Long parentId = params.get("parentId") != null ? ((Number) params.get("parentId")).longValue() : null;
            Integer sortOrder = params.get("sortOrder") != null ? ((Number) params.get("sortOrder")).intValue() : 0;
            departmentService.addDept(name, parentId, sortOrder);
            operationLogService.record(request, "DEPARTMENT", "CREATE", "新增部门：" + name + "，父部门：" + parentId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "DEPARTMENT", "CREATE", "新增部门失败", false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequireAnyPermission({"system_department:update", "system_department:manager"})
    public ApiResponse update(@PathVariable("id") Long id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String name = (String) params.get("name");
            Integer sortOrder = params.get("sortOrder") != null ? ((Number) params.get("sortOrder")).intValue() : null;
            Integer status = params.get("status") != null ? ((Number) params.get("status")).intValue() : null;
            Long managerUserId = params.get("managerUserId") != null ? ((Number) params.get("managerUserId")).longValue() : null;
            Boolean clearManager = params.get("clearManager") != null && (Boolean) params.get("clearManager");
            boolean basicChanged = params.containsKey("name") || params.containsKey("sortOrder") || params.containsKey("status");
            boolean managerChanged = params.containsKey("managerUserId") || clearManager;
            if (!basicChanged && !managerChanged) {
                throw new RuntimeException("无可更新内容");
            }
            if (basicChanged && !roleService.hasFeature((String) request.getAttribute("username"), (String) request.getAttribute("role"), "system_department:update")) {
                throw new PermissionDeniedException("无编辑部门权限");
            }
            if (managerChanged && !roleService.hasFeature((String) request.getAttribute("username"), (String) request.getAttribute("role"), "system_department:manager")) {
                throw new PermissionDeniedException("无设置部门负责人权限");
            }
            if (clearManager) {
                departmentService.clearManager(id);
                departmentService.updateDept(id, name, sortOrder, status, null);
            } else {
                departmentService.updateDept(id, name, sortOrder, status, managerUserId);
            }
            operationLogService.record(request, "DEPARTMENT", "UPDATE", "编辑部门：" + id + "，名称：" + name);
            return ApiResponse.success();
        } catch (PermissionDeniedException e) {
            operationLogService.record(request, "DEPARTMENT", "UPDATE", "编辑部门失败：" + id, false, e.getMessage());
            return ApiResponse.error(403, e.getMessage());
        } catch (RuntimeException e) {
            operationLogService.record(request, "DEPARTMENT", "UPDATE", "编辑部门失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission("system_department:delete")
    public ApiResponse delete(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            departmentService.deleteDept(id);
            operationLogService.record(request, "DEPARTMENT", "DELETE", "删除部门：" + id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            operationLogService.record(request, "DEPARTMENT", "DELETE", "删除部门失败：" + id, false, e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        }
    }

    private void assertHasFeature(HttpServletRequest request, String featureCode, String message) {
        if (!roleService.hasFeature((String) request.getAttribute("username"), (String) request.getAttribute("role"), featureCode)) {
            throw new PermissionDeniedException(message);
        }
    }

    private void assertCanWriteField(HttpServletRequest request, String fieldCode, String message) {
        if (!roleService.hasWritableField((String) request.getAttribute("username"), "system_user", fieldCode)) {
            throw new PermissionDeniedException(message);
        }
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

    private List<String> exportHeaders() {
        return List.of("部门名称", "层级", "上级部门", "负责人", "状态", "排序", "创建时间");
    }

    private List<String> importHeaders() {
        return List.of("部门名称", "父部门名称", "排序", "状态(启用/禁用)");
    }

    private List<List<String>> importSampleRows() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("导入示例部门A", "我的公司", "10", "启用"));
        rows.add(List.of("导入示例小组A", "导入示例部门A", "20", "启用"));
        return rows;
    }

    private List<List<String>> exportRows(List<Map<String, Object>> departments) {
        List<List<String>> rows = new ArrayList<>();
        for (Map<String, Object> department : departments) {
            rows.add(List.of(
                    stringValue(department.get("name")),
                    stringValue(department.get("level")),
                    stringValue(department.get("parentName")),
                    stringValue(department.get("managerName")),
                    Integer.valueOf(1).equals(department.get("status")) ? "启用" : "禁用",
                    stringValue(department.get("sortOrder")),
                    formatDateTime(department.get("createdAt"))
            ));
        }
        return rows;
    }

    private String stringValue(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private String formatDateTime(Object value) {
        String text = value == null ? null : String.valueOf(value);
        return text == null || text.isBlank() ? "-" : text.replace('T', ' ');
    }

    private String importSummary(Map<String, Object> result) {
        return "成功 " + stringValue(result.get("successCount"))
                + "，跳过 " + stringValue(result.get("skippedCount"))
                + "，失败 " + stringValue(result.get("failureCount"));
    }
}
