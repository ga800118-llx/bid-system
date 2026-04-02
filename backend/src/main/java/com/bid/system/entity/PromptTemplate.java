package com.bid.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

public class PromptTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String content;
    private String fieldDef;
    @TableField("is_active")
    private Integer isActive;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFieldDef() { return fieldDef; }
    public void setFieldDef(String fieldDef) { this.fieldDef = fieldDef; }
    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
