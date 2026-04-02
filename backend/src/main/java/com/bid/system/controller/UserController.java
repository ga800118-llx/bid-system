package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.entity.User;
import com.bid.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ApiResponse register(@RequestBody User user) {
        try {
            User u = userService.register(user.getUsername(), user.getPassword());
            return ApiResponse.success(u);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse login(@RequestBody User user) {
        String token = userService.login(user.getUsername(), user.getPassword());
        if (token != null) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            return ApiResponse.success(data);
        }
        return ApiResponse.error(401, "用户名或密码错误");
    }

    @GetMapping("/list")
    public ApiResponse list(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            return ApiResponse.error(403, "无权限");
        }
        return ApiResponse.success(userService.list());
    }

    @PutMapping("/role/{id}")
    public ApiResponse updateRole(@PathVariable Long id, @RequestParam String role, HttpServletRequest request) {
        String reqRole = (String) request.getAttribute("role");
        if (!"admin".equals(reqRole)) {
            return ApiResponse.error(403, "无权限");
        }
        userService.updateRole(id, role);
        return ApiResponse.success();
    }

    @PutMapping("/password/{id}")
    public ApiResponse updatePassword(@PathVariable Long id, @RequestParam String password, HttpServletRequest request) {
        String reqRole = (String) request.getAttribute("role");
        if (!"admin".equals(reqRole)) {
            return ApiResponse.error(403, "无权限");
        }
        userService.updatePassword(id, password);
        return ApiResponse.success();
    }

    @GetMapping("/info")
    public ApiResponse info(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        User user = userService.findByUsername(username);
        if (user != null) {
            user.setPassword(null);
            return ApiResponse.success(user);
        }
        return ApiResponse.error(404, "用户不存在");
    }
}
