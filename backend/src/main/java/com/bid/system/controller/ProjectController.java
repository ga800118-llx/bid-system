package com.bid.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bid.system.dto.ApiResponse;
import com.bid.system.entity.Project;
import com.bid.system.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Value("${app.upload.path}")
    private String uploadPath;

    @PostMapping("/upload")
    public ApiResponse upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            return ApiResponse.error(403, "仅管理用户可上传文件");
        }
        String username = (String) request.getAttribute("username");
        try {
            Project p = projectService.uploadAndExtract(file, 1L);
            return ApiResponse.success(p);
        } catch (Exception e) {
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse list(@RequestParam(required = false) String keyword,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "10") int size) {
        Page<Project> result = projectService.search(keyword, page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse detail(@PathVariable Long id) {
        Project p = projectService.getDetail(id);
        if (p == null) {
            return ApiResponse.error(404, "项目不存在");
        }
        p.setFilePath(null);
        return ApiResponse.success(p);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id, HttpServletRequest request) {
        Project p = projectService.getDetail(id);
        if (p == null || p.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(p.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + p.getFileOriginalName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"admin".equals(role)) {
            return ApiResponse.error(403, "无权限删除");
        }
        projectService.removeById(id);
        return ApiResponse.success();
    }
}