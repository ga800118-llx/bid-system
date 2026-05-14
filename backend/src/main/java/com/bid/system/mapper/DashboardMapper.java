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

    @Select("SELECT COUNT(*) FROM projects WHERE bid_open_time IS NOT NULL AND bid_open_time != ''")
    int selectPendingCount();

    @Select("SELECT COUNT(DISTINCT bidding_agency) FROM projects WHERE bidding_agency IS NOT NULL AND bidding_agency != ''")
    int selectAgencyCount();

    @Select("SELECT SUBSTRING(CONCAT('', created_at), 1, 7) AS `month`, COUNT(*) AS count FROM projects GROUP BY SUBSTRING(CONCAT('', created_at), 1, 7) ORDER BY `month` ASC")
    List<Map<String, Object>> selectMonthlyProjects();

    @Select("SELECT bidding_agency AS agency, COUNT(*) AS count FROM projects WHERE bidding_agency IS NOT NULL AND bidding_agency != '' GROUP BY bidding_agency ORDER BY COUNT(*) DESC LIMIT 5")
    List<Map<String, Object>> selectTopAgencies();
}
