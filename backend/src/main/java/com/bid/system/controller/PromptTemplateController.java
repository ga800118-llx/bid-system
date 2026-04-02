package com.bid.system.controller;

import com.bid.system.dto.ApiResponse;
import com.bid.system.service.PromptTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/prompt")
public class PromptTemplateController {

    @Autowired
    private PromptTemplateService promptTemplateService;

    @GetMapping
    public ApiResponse getTemplate(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            return ApiResponse.error(403, "\u65e0\u6743\u9650");
        }
        try {
            Map<String, String> tmpl = promptTemplateService.getTemplate();
            return ApiResponse.success(tmpl);
        } catch (Exception e) {
            return ApiResponse.error("\u8bfb\u53d6\u5931\u8d25: " + e.getMessage());
        }
    }

    @PutMapping
    public ApiResponse saveTemplate(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            return ApiResponse.error(403, "\u65e0\u6743\u9650");
        }
        try {
            String name = body.get("name");
            String system = body.get("system");
            String content = body.get("content");
            String fieldDef = body.get("fieldDef");
            promptTemplateService.saveTemplate(name, system, content, fieldDef);
            return ApiResponse.success();
        } catch (Exception e) {
            return ApiResponse.error("\u4fdd\u5b58\u5931\u8d25: " + e.getMessage());
        }
    }
}
