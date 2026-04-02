package com.bid.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("extract_logs")
public class ExtractLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private String extractType;
    private Integer textLength;
    private Integer responseLength;
    private Boolean success;
    private String errorMessage;
    private Integer durationMs;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fn) { this.fileName = fn; }
    public String getExtractType() { return extractType; }
    public void setExtractType(String et) { this.extractType = et; }
    public Integer getTextLength() { return textLength; }
    public void setTextLength(Integer tl) { this.textLength = tl; }
    public Integer getResponseLength() { return responseLength; }
    public void setResponseLength(Integer rl) { this.responseLength = rl; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean s) { this.success = s; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String em) { this.errorMessage = em; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer dm) { this.durationMs = dm; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime ca) { this.createdAt = ca; }
}