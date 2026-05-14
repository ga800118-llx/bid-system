package com.bid.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("org_role_field")
public class RoleField {
    @TableId(type = IdType.INPUT)
    private Long roleId;
    private String moduleCode;
    private String fieldCode;
    private Integer canRead;
    private Integer canWrite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public String getFieldCode() { return fieldCode; }
    public void setFieldCode(String fieldCode) { this.fieldCode = fieldCode; }
    public Integer getCanRead() { return canRead; }
    public void setCanRead(Integer canRead) { this.canRead = canRead; }
    public Integer getCanWrite() { return canWrite; }
    public void setCanWrite(Integer canWrite) { this.canWrite = canWrite; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
