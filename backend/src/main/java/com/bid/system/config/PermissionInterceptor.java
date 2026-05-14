package com.bid.system.config;

import com.bid.system.service.RoleService;
import com.bid.system.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final RoleService roleService;
    private final OperationLogService operationLogService;

    public PermissionInterceptor(RoleService roleService, OperationLogService operationLogService) {
        this.roleService = roleService;
        this.operationLogService = operationLogService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) return true;
        RequirePermission permission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (permission == null) {
            permission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        RequireAnyPermission anyPermission = handlerMethod.getMethodAnnotation(RequireAnyPermission.class);
        if (anyPermission == null) {
            anyPermission = handlerMethod.getBeanType().getAnnotation(RequireAnyPermission.class);
        }
        if (permission == null && anyPermission == null) return true;

        String username = (String) request.getAttribute("username");
        String role = (String) request.getAttribute("role");
        if (permission != null && roleService.hasFeature(username, role, permission.value())) return true;
        if (anyPermission != null) {
            for (String code : anyPermission.value()) {
                if (roleService.hasFeature(username, role, code)) return true;
            }
        }

        operationLogService.record(request, "AUTH", "PERMISSION_DENIED", "权限拒绝：" + request.getMethod() + " " + request.getRequestURI() + "，需要权限：" + requiredPermissions(permission, anyPermission), false, "无权限");
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"无权限\"}");
        return false;
    }

    private String requiredPermissions(RequirePermission permission, RequireAnyPermission anyPermission) {
        if (permission != null) return permission.value();
        if (anyPermission == null || anyPermission.value().length == 0) return "-";
        return String.join(" 或 ", anyPermission.value());
    }
}
