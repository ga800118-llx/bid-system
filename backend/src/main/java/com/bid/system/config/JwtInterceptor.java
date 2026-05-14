package com.bid.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bid.system.entity.Department;
import com.bid.system.entity.User;
import com.bid.system.mapper.DepartmentMapper;
import com.bid.system.mapper.UserMapper;
import com.bid.system.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

    public JwtInterceptor(UserMapper userMapper, DepartmentMapper departmentMapper) {
        this.userMapper = userMapper;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || path.equals("/api/user/login") || path.equals("/api/user/register") || path.equals("/api/user/ping")) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或登录已过期\"}");
            return false;
        }
        token = token.substring(7);
        try {
            User user = findUserFromToken(token);
            if (!isActiveLoginUser(user)) {
                writeUnauthorized(response, "未登录或登录已过期");
                return false;
            }
            request.setAttribute("userId", user.getId());
            request.setAttribute("username", user.getUsername());
            request.setAttribute("role", user.getRole());
            return true;
        } catch (Exception e) {
            writeUnauthorized(response, "Token无效或已过期");
            return false;
        }
    }

    private User findUserFromToken(String token) {
        Long userId = JwtUtil.getUserId(token);
        if (userId != null) {
            return userMapper.selectById(userId);
        }
        String username = JwtUtil.getUsername(token);
        if (username == null || username.isBlank()) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private boolean isActiveLoginUser(User user) {
        if (user == null) return false;
        if (user.getStatus() != null && user.getStatus() == 0) return false;
        if ("admin".equals(user.getRole())) return true;
        if (user.getDeptId() == null) return false;
        Department dept = departmentMapper.selectById(user.getDeptId());
        return dept != null && (dept.getStatus() == null || dept.getStatus() != 0);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + message + "\"}");
    }
}
