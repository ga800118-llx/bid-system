package com.bid.system.service;

import com.bid.system.mapper.DashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    @Autowired
    private DashboardMapper dashboardMapper;

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", dashboardMapper.selectTotalCount());
        stats.put("monthlyNew", dashboardMapper.selectMonthCount());
        stats.put("pendingBidOpen", dashboardMapper.selectPendingCount());
        stats.put("agencyCount", dashboardMapper.selectAgencyCount());
        return stats;
    }

    public Map<String, Object> getMonthlyProjects() {
        List<Map<String, Object>> raw = dashboardMapper.selectMonthlyProjects();
        List<String> months = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            months.add(String.valueOf(row.get("month")));
            counts.add(((Number) row.get("count")).intValue());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("months", months);
        result.put("counts", counts);
        return result;
    }

    public Map<String, Object> getProjectsByAgency() {
        List<Map<String, Object>> raw = dashboardMapper.selectTopAgencies();
        List<String> agencies = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Map<String, Object> row : raw) {
            agencies.add(String.valueOf(row.get("agency")));
            counts.add(((Number) row.get("count")).intValue());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agencies", agencies);
        result.put("counts", counts);
        return result;
    }
}