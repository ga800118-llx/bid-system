package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.Role;
import com.bid.system.mapper.RoleMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RoleService extends ServiceImpl<RoleMapper, Role> {

    public void addRole(String roleName, String description) {
        Role r=new Role();
        r.setRoleName(roleName);
        r.setDescription(description);
        r.setStatus(1);
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        save(r);
    }

    public void updateRole(Long id, String description, Integer status) {
        Role r=getById(id);
        if(r==null) throw new RuntimeException("role not found");
        if(description !=null) r.setDescription(description);
        if(status !=null) r.setStatus(status);
        r.setUpdatedAt(LocalDateTime.now());
        updateById(r);
    }

    public int deleteRole(Long id) {
        removeById(id);
        return 1;
    }
}
