package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.OperationLog;
import com.bid.system.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OperationLogService extends ServiceImpl<OperationLogMapper, OperationLog> {

    private final RoleService roleService;

    public OperationLogService(RoleService roleService) {
        this.roleService = roleService;
    }

    public Map<String, Object> listLogs(int page, int size, String keyword, String module, String action,
                                        Integer success, String startDate, String endDate) {
        return listLogs(page, size, keyword, module, action, success, startDate, endDate, null);
    }

    public Map<String, Object> listLogs(int page, int size, String keyword, String module, String action,
                                        Integer success, String startDate, String endDate, String operatorUsername) {
        LambdaQueryWrapper<OperationLog> q = buildLogQuery(keyword, module, action, success, startDate, endDate);
        q.orderByDesc(OperationLog::getCreatedAt);
        Page<OperationLog> p = page(new Page<>(page, size), q);
        Set<String> readableFields = readableLogFields(operatorUsername);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", p.getRecords().stream().map(log -> buildLogRecord(log, readableFields)).toList());
        result.put("total", p.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("readableFields", new ArrayList<>(readableFields));
        return result;
    }

    public Map<String, Object> exportLogs(String keyword, String module, String action,
                                          Integer success, String startDate, String endDate, String operatorUsername) {
        LambdaQueryWrapper<OperationLog> q = buildLogQuery(keyword, module, action, success, startDate, endDate);
        q.orderByDesc(OperationLog::getCreatedAt);
        Set<String> readableFields = readableLogFields(operatorUsername);
        List<Map<String, Object>> records = list(q).stream().map(log -> buildLogRecord(log, readableFields)).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("readableFields", new ArrayList<>(readableFields));
        return result;
    }

    private LambdaQueryWrapper<OperationLog> buildLogQuery(String keyword, String module, String action,
                                                           Integer success, String startDate, String endDate) {
        LambdaQueryWrapper<OperationLog> q = new LambdaQueryWrapper<>();
        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) {
            q.and(w -> w.like(OperationLog::getOperatorUsername, kw).or().like(OperationLog::getContent, kw));
        }
        if (module != null && !module.isBlank()) q.eq(OperationLog::getModule, module.trim());
        if (action != null && !action.isBlank()) q.eq(OperationLog::getAction, action.trim());
        if (success != null) q.eq(OperationLog::getSuccess, success);
        LocalDateTime start = parseStart(startDate);
        LocalDateTime end = parseEnd(endDate);
        if (start != null) q.ge(OperationLog::getCreatedAt, start);
        if (end != null) q.le(OperationLog::getCreatedAt, end);
        return q;
    }

    private Set<String> readableLogFields(String operatorUsername) {
        if (operatorUsername == null || operatorUsername.isBlank()) {
            return new LinkedHashSet<>(List.of("operator", "ip", "content", "request"));
        }
        return roleService.getReadableFieldsForUser(operatorUsername, "system_log");
    }

    private Map<String, Object> buildLogRecord(OperationLog log, Set<String> readableFields) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", log.getId());
        m.put("module", log.getModule());
        m.put("action", log.getAction());
        m.put("success", log.getSuccess());
        m.put("createdAt", log.getCreatedAt());
        if (readableFields.contains("operator")) m.put("operatorUsername", log.getOperatorUsername());
        if (readableFields.contains("ip")) m.put("ip", log.getIp());
        if (readableFields.contains("request")) {
            m.put("requestMethod", log.getRequestMethod());
            m.put("requestPath", log.getRequestPath());
        }
        if (readableFields.contains("content")) {
            m.put("content", log.getContent());
            m.put("errorMessage", log.getErrorMessage());
        }
        return m;
    }

    public void record(HttpServletRequest request, String module, String action, String content) {
        record(request, module, action, content, true, null);
    }

    public void record(HttpServletRequest request, String module, String action, String content, boolean success, String errorMessage) {
        OperationLog log = new OperationLog();
        log.setOperatorUsername(resolveUsername(request));
        log.setModule(trim(module, 50));
        log.setAction(trim(action, 50));
        log.setContent(trim(content, 1000));
        log.setRequestMethod(request != null ? trim(request.getMethod(), 20) : null);
        log.setRequestPath(request != null ? trim(request.getRequestURI(), 255) : null);
        log.setIp(resolveIp(request));
        log.setSuccess(success ? 1 : 0);
        log.setErrorMessage(trim(errorMessage, 1000));
        log.setCreatedAt(LocalDateTime.now());
        save(log);
    }

    private String resolveUsername(HttpServletRequest request) {
        if (request == null) return "system";
        Object username = request.getAttribute("username");
        return username == null ? "anonymous" : String.valueOf(username);
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return trim(forwarded.split(",")[0].trim(), 100);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return trim(realIp, 100);
        return trim(request.getRemoteAddr(), 100);
    }

    private LocalDateTime parseStart(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.trim()).atStartOfDay();
    }

    private LocalDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay().minusNanos(1);
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
