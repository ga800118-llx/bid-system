package com.bid.system.controller;

import com.bid.system.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return dashboardService.getStats();
    }

    @GetMapping("/monthly")
    public Map<String, Object> getMonthly() {
        return dashboardService.getMonthlyProjects();
    }

    @GetMapping("/by-agency")
    public Map<String, Object> getByAgency() {
        return dashboardService.getProjectsByAgency();
    }
}