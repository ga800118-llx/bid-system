package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.OrgUser;
import com.bid.system.mapper.OrgUserMapper;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrgUserService extends ServiceImpl<OrgUserMapper, OrgUser> {

    public Map<String, Object> getByDept(Long deptId, int page, int size) {
        LambdaQueryWrapper<OrgUser> q = new LambdaQueryWrapper<>();
        q.eq(OrgUser::getDeptId, deptId);
        q.orderByDesc(OrgUser::getCreatedAt);
        Page<OrgUser> p = page(new Page<>(page, size), q);
        var records = p.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("deptId", u.getDeptId());
            m.put("status", u.getStatus());
            return m;
        }).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", p.getTotal());
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
