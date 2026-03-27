package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.dto.LoginRequest;
import com.bid.system.dto.RegisterRequest;
import com.bid.system.entity.User;
import com.bid.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ApiResponse login(@Valid @RequestBody LoginRequest req) {
        try {
            String token = userService.login(req);
            Map<String, String> result = new HashMap<>();
            result.put("token", token);
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }

    @PostMapping("/register")
    public ApiResponse register(@Valid @RequestBody RegisterRequest req) {
        try {
            userService.register(req);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/info")
    public ApiResponse info(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        User user = userService.getUserInfo(username);
        return ApiResponse.success(user);
    }

    @GetMapping("/list")
    public ApiResponse list(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            return ApiResponse.error(403, "无权限访问");
        }
        return ApiResponse.success(userService.list());
    }

    @PutMapping("/role/{id}")
    public ApiResponse updateRole(@PathVariable Long id, @RequestParam String role, HttpServletRequest request) {
        String currentRole = (String) request.getAttribute("role");
        if (!"admin".equals(currentRole)) {
            return ApiResponse.error(403, "无权限访问");
        }
        User user = userService.getById(id);
        if (user == null) {
            return ApiResponse.error(404, "用户不存在");
        }
        user.setRole(role);
        userService.updateById(user);
        return ApiResponse.success();
    }
}