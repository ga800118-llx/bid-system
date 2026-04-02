package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.Project;
import com.bid.system.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService extends ServiceImpl<ProjectMapper, Project> {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private MiniMaxTextExtractService miniMaxTextExtractService;

    @Value("${app.upload.path}")
    private String uploadPath;

    private BigDecimal parseMoney(Object val) {
        if (val == null) return null;
        String s = String.valueOf(val).replaceAll("[^0-9.]", "");
        try { return new BigDecimal(s); } catch (Exception e) { return null; }
    }

    private LocalDateTime parseDateTime(Object val) {
        if (val == null) return null;
        try {
            String n = String.valueOf(val).replace("　", "-").replace(" ", "-")
                .replace("/", "-").replace(" ", "T").replace("--", "-").trim();
            if (n.length() <= 10) n = n.contains("T") ? n.substring(0, 16) : n + "T00:00";
            return LocalDateTime.parse(n.substring(0, 16));
        } catch (Exception e) { return null; }
    }

    private Project mapToProject(Map<String, Object> data) {
        Project p = new Project();
        p.setProjectName(getString(data, "projectName", "project_name", "项目名称"));
        p.setProjectCode(getString(data, "projectCode", "project_code", "项目编号"));
        p.setBiddingAgency(getString(data, "biddingAgency", "bidding_agency", "招标代理机构"));
        p.setClientUnit(getString(data, "clientUnit", "tenderer", "招标人", "发标单位"));
        p.setBidOpenTime(getString(data, "bidOpenTime", "bid_open_time", "bidOpenTime"));
        p.setComplaintDeadline(getString(data, "complaintDeadline", "complaint_deadline", "complaintDeadline"));
        p.setCeilingPrice(parseMoney(getString(data, "ceilingPrice", "ceiling_price", "最高投标限价", "拦标价")));
        p.setFloorPrice(parseMoney(getString(data, "floorPrice", "floor_price", "下限价")));
        p.setContractPayment(getString(data, "contractPayment", "payment_terms", "付款方式", "付款条款"));
        p.setExpertComposition(getString(data, "expertComposition", "专家", "评标委员会"));
        p.setPriceScoreMethod(getString(data, "priceScoreMethod", "price_score_method", "价格分", "评标办法"));
        p.setSubjectiveScoreDetails(getString(data, "subjectiveScoreDetails", "subjective_score", "主观分", "技术分"));
        p.setBidBond(parseMoney(getString(data, "bidBond", "bid_bond", "投标保证金")));
        p.setPerformanceBond(getString(data, "performanceBond", "performance_bond", "履约保证金"));
        return p;
    }

    private String getString(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object val = data.get(key);
            if (val != null && !"null".equals(String.valueOf(val))) {
                return String.valueOf(val);
            }
        }
        return null;
    }

    public Project uploadAndExtract(MultipartFile file, Long uploaderId) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String savedFilename = UUID.randomUUID().toString() + suffix;
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath();
        Files.createDirectories(uploadDir);
        Path filePath = uploadDir.resolve(savedFilename);
        file.transferTo(filePath.toFile());

        // Extract text, length, page count
        Map<String, Object> extractResult = miniMaxTextExtractService.extractTextFromPdf(filePath.toString());
        String pdfText = (String) extractResult.get("text");
        Integer textLength = (Integer) extractResult.get("textLength");
        Integer pageCount = (Integer) extractResult.get("pageCount");

        if (pdfText == null || pdfText.isBlank()) {
            throw new RuntimeException("PDF文字提取结果为空");
        }

        // Save markdown file
        String baseName = originalFilename != null
            ? originalFilename.substring(0, originalFilename.lastIndexOf("."))
            : "document";
        String mdFileName = baseName + "_[MD.md]";
        Path mdPath = uploadDir.resolve(mdFileName);
        Files.writeString(mdPath, pdfText, StandardCharsets.UTF_8);

        // Extract structured data
        Map<String, Object> data = miniMaxTextExtractService.extract(pdfText);

        Project project = mapToProject(data);
        project.setFilePath(filePath.toString());
        project.setFileOriginalName(originalFilename);
        project.setUploaderId(uploaderId);
        project.setTextLength(textLength);
        project.setPageCount(pageCount);
        project.setMarkdownFilePath(mdPath.toString());
        projectMapper.insert(project);
        return project;
    }

    public Page<Project> search(String keyword, int page, int size) {
        Page<Project> pg = new Page<>((long) page, (long) size);
        LambdaQueryWrapper<Project> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) w.like(Project::getProjectName, keyword);
        w.orderByDesc(Project::getCreatedAt);
        return projectMapper.selectPage(pg, w);
    }

    public Project getDetail(Long id) { return projectMapper.selectById(id); }

    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<Project> projects = projectMapper.selectBatchIds(ids);
        for (Project p : projects) {
            try {
                String fp = p.getFilePath();
                String mfp = p.getMarkdownFilePath();
                if (fp != null && !fp.isEmpty()) {
                    java.io.File f = new java.io.File(fp);
                    if (f.exists()) f.delete();
                }
                if (mfp != null && !mfp.isEmpty()) {
                    java.io.File mf = new java.io.File(mfp);
                    if (mf.exists()) mf.delete();
                }
            } catch (Exception e) { }
        }
        return projectMapper.deleteBatchIds(ids);
    }
}
