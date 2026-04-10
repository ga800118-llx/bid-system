package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.service.RoleService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse list() {
        return ApiResponse.success(roleService.list());
    }

    @PostMapping
    public ApiResponse add(@RequestBody Map<String, Object> map) {
        try {
            String roleName = (String) map.get("roleName");
            String description = (String) map.get("description");
            if (roleName == null || roleName.isBlank()) {
                return ApiResponse.error(400, "role name cannot be empty");
            }
            roleService.addRole(roleName, description);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse update(@PathVariable("id") Long id, @RequestBody Map<String, Object> map) {
        try {
            String description = null;
            Integer status = null;
            if (map.get("description") != null) {
                description = (String) map.get("description");
            }
            if (map.get("status") != null) {
                status = ((Number) map.get("status")).intValue();
            }
            roleService.updateRole(id, description, status);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable("id") Long id) {
        try {
            roleService.deleteRole(id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
