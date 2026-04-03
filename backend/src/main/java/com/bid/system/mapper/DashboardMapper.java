package com.bid.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bid.system.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper extends BaseMapper<Project> {
    int selectTotalCount();
    int selectMonthCount();
    int selectPendingCount();
    int selectAgencyCount();
    List<Map<String, Object>> selectMonthlyProjects();
    List<Map<String, Object>> selectTopAgencies();
}