package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.Project;
import com.bid.system.mapper.ProjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService extends ServiceImpl<ProjectMapper, Project> {

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private MiniMaxExtractService miniMaxExtractService;

    @Value("")
    private String uploadPath;

    private BigDecimal parseMoney(String val) {
        if (val == null || val.isEmpty()) return null;
        // Remove all non-digit characters except dot
        String num = val.replaceAll("[^" + "0-9." + "]", "");
        try { return new BigDecimal(num); } catch (Exception e) { return null; }
    }

    private LocalDateTime parseDateTime(String val) {
        if (val == null || val.isEmpty()) return null;
        try {
            String n = val.replace("年", "-").replace("月", "-").replace("日", "").replace(" ", "T").replace("--", "-");
            if (n.length() <= 10) n = n.contains("T") ? n.substring(0, 16) : n + "T00:00";
            return LocalDateTime.parse(n.substring(0, 16));
        } catch (Exception e) { return null; }
    }

    private Project mapToProject(Map<String, Object> data) {
        Project p = new Project();
        p.setProjectName(getString(data, "projectName"));
        p.setProjectCode(getString(data, "projectCode"));
        p.setBiddingAgency(getString(data, "biddingAgency"));
        p.setClientUnit(getString(data, "clientUnit"));
        p.setBidOpenTime(parseDateTime(getString(data, "bidOpenTime")));
        p.setComplaintDeadline(parseDateTime(getString(data, "complaintDeadline")));
        p.setCeilingPrice(parseMoney(getString(data, "ceilingPrice")));
        p.setFloorPrice(parseMoney(getString(data, "floorPrice")));
        p.setContractPayment(getString(data, "contractPayment"));
        p.setExpertComposition(getString(data, "expertComposition"));
        p.setPriceScoreMethod(getString(data, "priceScoreMethod"));
        p.setSubjectiveScoreDetails(getString(data, "subjectiveScoreDetails"));
        p.setBidBond(parseMoney(getString(data, "bidBond")));
        p.setPerformanceBond(parseMoney(getString(data, "performanceBond")));
        return p;
    }



    private String getString(Map<String, Object> data, String key) {
        Object val = data.get(key);
        return val == null ? null : String.valueOf(val);
    }

    public Project uploadAndExtract(MultipartFile file, Long uploaderId) throws Exception {
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String savedFilename = UUID.randomUUID().toString() + suffix;
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath();
        Files.createDirectories(uploadDir);
        Path filePath = uploadDir.resolve(savedFilename);
        file.transferTo(filePath.toFile());
        Map<String, Object> data = miniMaxExtractService.extractFromFile(file);
        Project project = mapToProject(data);
        project.setFilePath(filePath.toString());
        project.setFileOriginalName(originalFilename);
        project.setUploaderId(uploaderId);
        projectMapper.insert(project);
        return project;
    }

    private String extractText(File file, String suffix) throws Exception {
        String lower = suffix.toLowerCase();
        if (lower.endsWith(".pdf")) return extractPdfText(file);
        else if (lower.endsWith(".docx")) return extractDocxText(file);
        else if (lower.endsWith(".doc")) return extractDocText(file);
        return "";
    }

    private String extractPdfText(File file) throws Exception {
        try (PDDocument doc = Loader.loadPDF(file)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractDocxText(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            return doc.getParagraphs().stream().map(XWPFParagraph::getText).collect(Collectors.joining("\n"));
        }
    }

    private String extractDocText(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            return new org.apache.poi.hwpf.HWPFDocument(fis).getText().toString();
        }
    }

    public Page<Project> search(String keyword, int page, int size) {
        Page<Project> pg = new Page<>((long) page, (long) size);
        LambdaQueryWrapper<Project> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) w.like(Project::getProjectName, keyword);
        w.orderByDesc(Project::getCreatedAt);
        return projectMapper.selectPage(pg, w);
    }

    public Project getDetail(Long id) { return projectMapper.selectById(id); }
}
