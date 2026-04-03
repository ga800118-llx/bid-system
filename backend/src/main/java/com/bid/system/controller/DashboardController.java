package com.bid.system.controller;

import com.bid.system.service.DashboardService;
import com.bid.system.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public ApiResponse getStats() {
        return ApiResponse.success(dashboardService.getStats());
    }

    @GetMapping("/monthly")
    public ApiResponse getMonthly() {
        return ApiResponse.success(dashboardService.getMonthlyProjects());
    }

    @GetMapping("/by-agency")
    public ApiResponse getByAgency() {
        return ApiResponse.success(dashboardService.getProjectsByAgency());
    }
}