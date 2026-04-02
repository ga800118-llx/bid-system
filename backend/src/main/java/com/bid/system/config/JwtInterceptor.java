package com.bid.system.config;

import com.bid.system.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        System.err.println("[JWT] path=" + path + " method=" + request.getMethod());
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
            String username = JwtUtil.getUsername(token);
            String role = JwtUtil.getRole(token);
            request.setAttribute("username", username);
            request.setAttribute("role", role);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token无效或已过期\"}");
            return false;
        }
    }
}