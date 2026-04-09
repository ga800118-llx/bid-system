package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.Department;
import com.bid.system.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DepartmentService extends ServiceImpl<DepartmentMapper, Department> {

    public List<Map<String, Object>> getTree() {
        LambdaQueryWrapper<Department> q = new LambdaQueryWrapper<>();
        q.orderByAsc(Department::getSortOrder);
        List<Department> all = list(q);
        return buildTree(all, null);
    }

    private List<Map<String, Object>> buildTree(List<Department> all, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Department d : all) {
            boolean match = (parentId == null && d.getParentId() == null) || (parentId != null && parentId.equals(d.getParentId()));
            if (match) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", d.getId());
                node.put("name", d.getName());
                node.put("parentId", d.getParentId());
                node.put("sortOrder", d.getSortOrder());
                node.put("status", d.getStatus());
                node.put("children", buildTree(all, d.getId()));
                result.add(node);
            }
        }
        return result;
    }

    public void addDept(String name, Long parentId, Integer sortOrder) {
        Department d = new Department();
        d.setName(name);
        d.setParentId(parentId);
        d.setSortOrder(sortOrder != null ? sortOrder : 0);
        d.setStatus(1);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        save(d);
    }

    public void updateDept(Long id, String name, Integer sortOrder, Integer status) {
        Department d = getById(id);
        if (d == null) throw new RuntimeException("DEPT_NOT_FOUND");
        if (name != null) d.setName(name);
        if (sortOrder != null) d.setSortOrder(sortOrder);
        if (status != null) d.setStatus(status);
        d.setUpdatedAt(LocalDateTime.now());
        updateById(d);
    }

    public int deleteDept(Long id) {
        long childCount = count(new LambdaQueryWrapper<Department>().eq(Department::getParentId, id));
        if (childCount > 0) throw new RuntimeException("HAS_CHILDREN");
        removeById(id);
        return (int) childCount;
    }
}