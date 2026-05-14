package com.bid.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bid.system.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
