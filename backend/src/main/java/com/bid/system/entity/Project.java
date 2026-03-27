package com.bid.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("projects")
public class Project implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectName;
    private String projectCode;
    private String biddingAgency;
    private String clientUnit;
    private LocalDateTime bidOpenTime;
    private LocalDateTime complaintDeadline;

    private BigDecimal ceilingPrice;

    private BigDecimal floorPrice;

    private String contractPayment;
    private String expertComposition;
    private String priceScoreMethod;
    private String subjectiveScoreDetails;

    private BigDecimal bidBond;

    private BigDecimal performanceBond;

    private String filePath;
    private String fileOriginalName;
    private Long uploaderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getBiddingAgency() { return biddingAgency; }
    public void setBiddingAgency(String biddingAgency) { this.biddingAgency = biddingAgency; }
    public String getClientUnit() { return clientUnit; }
    public void setClientUnit(String clientUnit) { this.clientUnit = clientUnit; }
    public LocalDateTime getBidOpenTime() { return bidOpenTime; }
    public void setBidOpenTime(LocalDateTime bidOpenTime) { this.bidOpenTime = bidOpenTime; }
    public LocalDateTime getComplaintDeadline() { return complaintDeadline; }
    public void setComplaintDeadline(LocalDateTime complaintDeadline) { this.complaintDeadline = complaintDeadline; }
    public BigDecimal getCeilingPrice() { return ceilingPrice; }
    public void setCeilingPrice(BigDecimal ceilingPrice) { this.ceilingPrice = ceilingPrice; }
    public BigDecimal getFloorPrice() { return floorPrice; }
    public void setFloorPrice(BigDecimal floorPrice) { this.floorPrice = floorPrice; }
    public String getContractPayment() { return contractPayment; }
    public void setContractPayment(String contractPayment) { this.contractPayment = contractPayment; }
    public String getExpertComposition() { return expertComposition; }
    public void setExpertComposition(String expertComposition) { this.expertComposition = expertComposition; }
    public String getPriceScoreMethod() { return priceScoreMethod; }
    public void setPriceScoreMethod(String priceScoreMethod) { this.priceScoreMethod = priceScoreMethod; }
    public String getSubjectiveScoreDetails() { return subjectiveScoreDetails; }
    public void setSubjectiveScoreDetails(String subjectiveScoreDetails) { this.subjectiveScoreDetails = subjectiveScoreDetails; }
    public BigDecimal getBidBond() { return bidBond; }
    public void setBidBond(BigDecimal bidBond) { this.bidBond = bidBond; }
    public BigDecimal getPerformanceBond() { return performanceBond; }
    public void setPerformanceBond(BigDecimal performanceBond) { this.performanceBond = performanceBond; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileOriginalName() { return fileOriginalName; }
    public void setFileOriginalName(String fileOriginalName) { this.fileOriginalName = fileOriginalName; }
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long uploaderId) { this.uploaderId = uploaderId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}