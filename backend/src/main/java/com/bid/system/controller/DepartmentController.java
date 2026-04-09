package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/system/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping("/tree")
    public ApiResponse tree() {
        return ApiResponse.success(departmentService.getTree());
    }

    @PostMapping
    public ApiResponse add(@RequestBody Map<String, Object> params) {
        try {
            String name = (String) params.get("name");
            Long parentId = params.get("parentId") != null ? ((Number) params.get("parentId")).longValue() : null;
            Integer sortOrder = params.get("sortOrder") != null ? ((Number) params.get("sortOrder")).intValue() : 0;
            departmentService.addDept(name, parentId, sortOrder);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        try {
            String name = (String) params.get("name");
            Integer sortOrder = params.get("sortOrder") != null ? ((Number) params.get("sortOrder")).intValue() : null;
            Integer status = params.get("status") != null ? ((Number) params.get("status")).intValue() : null;
            departmentService.updateDept(id, name, sortOrder, status);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id) {
        try {
            departmentService.deleteDept(id);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}