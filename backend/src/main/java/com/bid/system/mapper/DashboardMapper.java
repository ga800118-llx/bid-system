package com.bid.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bid.system.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper extends BaseMapper<Project> {
    @Select("SELECT COUNT(*) FROM projects")
    int selectTotalCount();

    @Select("SELECT COUNT(*) FROM projects WHERE YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())")
    int selectMonthCount();

    @Select("SELECT COUNT(*) FROM projects WHERE bid_open_time IS NOT NULL AND bid_open_time != '' AND bid_open_time > NOW()")
    int selectPendingCount();

    @Select("SELECT COUNT(DISTINCT bidding_agency) FROM projects WHERE bidding_agency IS NOT NULL AND bidding_agency != ''")
    int selectAgencyCount();

    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS count FROM projects WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month ASC")
    List<Map<String, Object>> selectMonthlyProjects();

    @Select("SELECT bidding_agency AS agency, COUNT(*) AS count FROM projects WHERE bidding_agency IS NOT NULL AND bidding_agency != '' GROUP BY bidding_agency ORDER BY COUNT(*) DESC LIMIT 5")
    List<Map<String, Object>> selectTopAgencies();
}