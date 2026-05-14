package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.SystemFile;
import com.bid.system.entity.SystemFileLink;
import com.bid.system.entity.User;
import com.bid.system.mapper.SystemFileLinkMapper;
import com.bid.system.mapper.SystemFileMapper;
import com.bid.system.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SystemFileService extends ServiceImpl<SystemFileMapper, SystemFile> {

    private static final String STORAGE_LOCAL = "LOCAL";
    private static final DateTimeFormatter KEY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SystemFileLinkMapper systemFileLinkMapper;
    private final UserMapper userMapper;
    private final SystemConfigService systemConfigService;

    @Value("${app.upload.path:./uploads/}")
    private String uploadRootPath;

    public SystemFileService(SystemFileLinkMapper systemFileLinkMapper,
                             UserMapper userMapper,
                             SystemConfigService systemConfigService) {
        this.systemFileLinkMapper = systemFileLinkMapper;
        this.userMapper = userMapper;
        this.systemConfigService = systemConfigService;
    }

    public Map<String, Object> listFiles(int page, int size, String keyword, String extension, Integer status,
                                         Long uploaderId, String dateFrom, String dateTo, Boolean previewable) {
        List<SystemFile> allRecords = queryFiles(keyword, extension, status, uploaderId, dateFrom, dateTo, previewable);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int from = Math.min((safePage - 1) * safeSize, allRecords.size());
        int to = Math.min(from + safeSize, allRecords.size());
        List<Map<String, Object>> records = allRecords.subList(from, to).stream().map(this::buildFileRecord).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", allRecords.size());
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    public List<Map<String, Object>> exportFiles(String keyword, String extension, Integer status,
                                                 Long uploaderId, String dateFrom, String dateTo, Boolean previewable) {
        return queryFiles(keyword, extension, status, uploaderId, dateFrom, dateTo, previewable).stream()
                .map(this::buildFileRecord)
                .toList();
    }

    private List<SystemFile> queryFiles(String keyword, String extension, Integer status,
                                        Long uploaderId, String dateFrom, String dateTo, Boolean previewable) {
        LambdaQueryWrapper<SystemFile> q = new LambdaQueryWrapper<>();
        String kw = normalize(keyword);
        if (!kw.isEmpty()) q.like(SystemFile::getOriginalName, kw);
        String ext = normalizeExtension(extension);
        if (!ext.isEmpty()) q.eq(SystemFile::getExtension, ext);
        if (status != null) q.eq(SystemFile::getStatus, normalizeStatus(status));
        if (uploaderId != null) q.eq(SystemFile::getUploaderId, uploaderId);
        LocalDateTime start = parseStart(dateFrom);
        LocalDateTime end = parseEnd(dateTo);
        if (start != null) q.ge(SystemFile::getCreatedAt, start);
        if (end != null) q.le(SystemFile::getCreatedAt, end);
        q.orderByDesc(SystemFile::getCreatedAt);
        List<SystemFile> allRecords = list(q);
        if (previewable != null) {
            allRecords = allRecords.stream()
                    .filter(file -> previewable.equals(isPreviewable(file)))
                    .toList();
        }
        return allRecords;
    }

    public Map<String, Object> getFileDetail(Long id) {
        SystemFile file = requireFile(id);
        Map<String, Object> result = buildFileRecord(file);
        result.put("links", listLinksRaw(id, null, null).stream().map(this::buildLinkRecord).toList());
        return result;
    }

    public SystemFile uploadFile(MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) throw new RuntimeException("上传文件不能为空");
        if (!STORAGE_LOCAL.equals(resolveStorageType())) throw new RuntimeException("当前仅支持本地存储");
        long maxSizeBytes = resolveMaxSizeBytes();
        if (file.getSize() > maxSizeBytes) throw new RuntimeException("文件大小超出限制");
        String originalName = normalize(file.getOriginalFilename());
        if (originalName.isEmpty()) throw new RuntimeException("文件名不能为空");
        String extension = detectExtension(originalName);
        validateExtensionAllowed(extension);
        String objectKey = buildObjectKey(extension);
        Path storageDir = resolveStorageDir();
        Path targetPath = storageDir.resolve(objectKey);
        try {
            Files.createDirectories(storageDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            SystemFile saved = new SystemFile();
            saved.setStorageType(STORAGE_LOCAL);
            saved.setBucket(null);
            saved.setObjectKey(objectKey);
            saved.setOriginalName(originalName);
            saved.setExtension(extension);
            saved.setContentType(normalize(file.getContentType()));
            saved.setFileSize(file.getSize());
            saved.setFileHash(calculateFileHash(targetPath));
            saved.setStoragePath(targetPath.toAbsolutePath().toString());
            saved.setStatus(1);
            saved.setUploaderId(uploaderId);
            LocalDateTime now = LocalDateTime.now();
            saved.setCreatedAt(now);
            saved.setUpdatedAt(now);
            save(saved);
            return saved;
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败：" + e.getMessage());
        }
    }

    public void updateFileStatus(Long id, Integer status) {
        SystemFile file = requireFile(id);
        file.setStatus(normalizeStatus(status));
        file.setUpdatedAt(LocalDateTime.now());
        updateById(file);
    }

    public void disableFile(Long id) {
        updateFileStatus(id, 0);
    }

    public List<Map<String, Object>> listLinks(String bizType, Long bizId) {
        if (normalize(bizType).isEmpty()) throw new RuntimeException("业务类型不能为空");
        if (bizId == null) throw new RuntimeException("业务对象不能为空");
        return listLinksRaw(null, bizType, bizId).stream().map(this::buildLinkRecord).toList();
    }

    public List<Map<String, Object>> listLinkedFiles(String bizType, Long bizId, String categoryCode) {
        if (normalize(bizType).isEmpty()) throw new RuntimeException("业务类型不能为空");
        if (bizId == null) throw new RuntimeException("业务对象不能为空");
        String safeCategory = normalize(categoryCode);
        return listLinksRaw(null, bizType, bizId).stream()
                .filter(link -> safeCategory.isEmpty() || safeCategory.equals(normalize(link.getCategoryCode())))
                .map(link -> {
                    Map<String, Object> record = buildLinkRecord(link);
                    SystemFile file = getById(link.getFileId());
                    if (file != null) {
                        record.putAll(buildFileRecord(file));
                        record.put("linkId", link.getId());
                        record.put("fileId", link.getFileId());
                    }
                    return record;
                })
                .toList();
    }

    public int linkExistingFiles(Long bizId, List<Long> fileIds, String bizType, String categoryCode, Long createdBy) {
        if (bizId == null) throw new RuntimeException("业务对象不能为空");
        String safeBizType = normalize(bizType);
        if (safeBizType.isEmpty()) throw new RuntimeException("业务类型不能为空");
        String safeCategory = normalize(categoryCode);
        Set<Long> uniqueFileIds = new LinkedHashSet<>();
        if (fileIds != null) {
            for (Long fileId : fileIds) {
                if (fileId != null) uniqueFileIds.add(fileId);
            }
        }
        int count = 0;
        for (Long fileId : uniqueFileIds) {
            requireFile(fileId);
            long exists = systemFileLinkMapper.selectCount(new LambdaQueryWrapper<SystemFileLink>()
                    .eq(SystemFileLink::getFileId, fileId)
                    .eq(SystemFileLink::getBizType, safeBizType)
                    .eq(SystemFileLink::getBizId, bizId)
                    .eq(SystemFileLink::getCategoryCode, safeCategory));
            if (exists > 0) continue;
            SystemFileLink link = new SystemFileLink();
            link.setFileId(fileId);
            link.setBizType(safeBizType);
            link.setBizId(bizId);
            link.setCategoryCode(safeCategory);
            link.setSortOrder(count);
            link.setCreatedBy(createdBy);
            link.setCreatedAt(LocalDateTime.now());
            systemFileLinkMapper.insert(link);
            count++;
        }
        return count;
    }

    public SystemFileLink addLink(Long fileId, String bizType, Long bizId, String categoryCode, Integer sortOrder, Long createdBy) {
        if (fileId == null) throw new RuntimeException("文件不能为空");
        if (normalize(bizType).isEmpty()) throw new RuntimeException("业务类型不能为空");
        if (bizId == null) throw new RuntimeException("业务对象不能为空");
        requireFile(fileId);
        String safeBizType = normalize(bizType);
        String safeCategory = normalize(categoryCode);
        long count = systemFileLinkMapper.selectCount(new LambdaQueryWrapper<SystemFileLink>()
                .eq(SystemFileLink::getFileId, fileId)
                .eq(SystemFileLink::getBizType, safeBizType)
                .eq(SystemFileLink::getBizId, bizId)
                .eq(SystemFileLink::getCategoryCode, safeCategory));
        if (count > 0) throw new RuntimeException("文件关联已存在");
        SystemFileLink link = new SystemFileLink();
        link.setFileId(fileId);
        link.setBizType(safeBizType);
        link.setBizId(bizId);
        link.setCategoryCode(safeCategory);
        link.setSortOrder(sortOrder == null ? 0 : sortOrder);
        link.setCreatedBy(createdBy);
        link.setCreatedAt(LocalDateTime.now());
        systemFileLinkMapper.insert(link);
        return link;
    }

    public void removeLink(Long id) {
        SystemFileLink link = systemFileLinkMapper.selectById(id);
        if (link == null) throw new RuntimeException("文件关联不存在");
        systemFileLinkMapper.deleteById(id);
    }

    public Path resolveDownloadPath(Long id) {
        SystemFile file = requireFile(id);
        if (file.getStatus() != null && file.getStatus() == 0) throw new RuntimeException("文件已禁用");
        Path path = Paths.get(file.getStoragePath());
        if (!Files.exists(path) || !Files.isRegularFile(path)) throw new RuntimeException("文件不存在");
        return path;
    }

    public void assertPreviewable(Long id) {
        SystemFile file = requireFile(id);
        if (!isPreviewable(file)) {
            throw new RuntimeException("当前文件类型暂不支持在线预览");
        }
    }

    public Map<String, Object> getRules() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("maxSizeMb", Math.max(1, systemConfigService.getNumber("file.upload.max_size_mb", java.math.BigDecimal.valueOf(100)).intValue()));
        result.put("allowedExtensions", List.copyOf(resolveAllowedExtensions()));
        result.put("previewableExtensions", List.copyOf(resolvePreviewableExtensions()));
        return result;
    }

    public SystemFile requireFile(Long id) {
        SystemFile file = getById(id);
        if (file == null) throw new RuntimeException("文件不存在");
        return file;
    }

    private List<SystemFileLink> listLinksRaw(Long fileId, String bizType, Long bizId) {
        LambdaQueryWrapper<SystemFileLink> q = new LambdaQueryWrapper<>();
        if (fileId != null) q.eq(SystemFileLink::getFileId, fileId);
        if (bizType != null) q.eq(SystemFileLink::getBizType, normalize(bizType));
        if (bizId != null) q.eq(SystemFileLink::getBizId, bizId);
        q.orderByAsc(SystemFileLink::getSortOrder).orderByDesc(SystemFileLink::getCreatedAt);
        return systemFileLinkMapper.selectList(q);
    }

    private Map<String, Object> buildFileRecord(SystemFile file) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", file.getId());
        record.put("storageType", file.getStorageType());
        record.put("objectKey", file.getObjectKey());
        record.put("originalName", file.getOriginalName());
        record.put("extension", file.getExtension());
        record.put("contentType", file.getContentType());
        record.put("fileSize", file.getFileSize());
        record.put("fileHash", file.getFileHash());
        record.put("status", file.getStatus());
        record.put("uploaderId", file.getUploaderId());
        record.put("uploaderName", resolveUploaderName(file.getUploaderId()));
        record.put("previewable", isPreviewable(file));
        record.put("createdAt", file.getCreatedAt());
        record.put("updatedAt", file.getUpdatedAt());
        return record;
    }

    private Map<String, Object> buildLinkRecord(SystemFileLink link) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", link.getId());
        record.put("fileId", link.getFileId());
        record.put("bizType", link.getBizType());
        record.put("bizId", link.getBizId());
        record.put("categoryCode", link.getCategoryCode());
        record.put("sortOrder", link.getSortOrder());
        record.put("createdBy", link.getCreatedBy());
        record.put("createdAt", link.getCreatedAt());
        return record;
    }

    private String resolveUploaderName(Long uploaderId) {
        if (uploaderId == null) return null;
        User user = userMapper.selectById(uploaderId);
        if (user == null) return null;
        return normalize(user.getRealName()).isEmpty() ? user.getUsername() : user.getRealName();
    }

    private String resolveStorageType() {
        return normalize(systemConfigService.getString("file.storage.type", STORAGE_LOCAL)).toUpperCase();
    }

    private Path resolveStorageDir() {
        String relativePath = normalize(systemConfigService.getString("file.local.base_path", "uploads/files"));
        Path base = Paths.get(relativePath);
        if (!base.isAbsolute()) {
            Path uploadRoot = resolveUploadRoot();
            String uploadRootName = uploadRoot.getFileName() == null ? "" : uploadRoot.getFileName().toString();
            String firstSegment = base.getNameCount() > 0 ? base.getName(0).toString() : "";
            if (!uploadRootName.isEmpty() && uploadRootName.equals(firstSegment)) {
                base = uploadRoot.getParent() == null ? uploadRoot : uploadRoot.getParent().resolve(base).normalize();
            } else {
                base = uploadRoot.resolve(base).normalize();
            }
        }
        LocalDate now = LocalDate.now();
        return base.resolve(String.valueOf(now.getYear()))
                .resolve(String.format("%02d", now.getMonthValue()))
                .resolve(String.format("%02d", now.getDayOfMonth()));
    }

    private Path resolveUploadRoot() {
        Path uploadRoot = Paths.get(normalize(uploadRootPath));
        if (uploadRoot.isAbsolute()) {
            return uploadRoot.normalize();
        }
        return resolveProjectBaseDir().resolve(uploadRoot).normalize();
    }

    private Path resolveProjectBaseDir() {
        String configuredBaseDir = normalize(System.getProperty("bid.system.base-dir"));
        if (!configuredBaseDir.isEmpty()) {
            return Paths.get(configuredBaseDir).toAbsolutePath().normalize();
        }
        Path appHomeBase = resolveProjectBaseDirFromApplicationHome();
        if (appHomeBase != null) {
            return appHomeBase;
        }
        try {
            Path codeLocation = Paths.get(SystemFileService.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            Path current = Files.isRegularFile(codeLocation) ? codeLocation.getParent() : codeLocation;
            Path found = findProjectBaseDir(current);
            if (found != null) {
                return found;
            }
        } catch (URISyntaxException ignored) {
        }
        return Paths.get(System.getProperty("user.home", ".")).toAbsolutePath().normalize();
    }

    private Path resolveProjectBaseDirFromApplicationHome() {
        try {
            ApplicationHome home = new ApplicationHome(SystemFileService.class);
            if (home.getDir() == null) {
                return null;
            }
            Path current = home.getDir().toPath().toAbsolutePath().normalize();
            Path found = findProjectBaseDir(current);
            if (found != null) {
                return found;
            }
            if (current.getFileName() != null && "target".equals(current.getFileName().toString())) {
                Path backendDir = current.getParent();
                if (backendDir != null && backendDir.getFileName() != null && "backend".equals(backendDir.getFileName().toString())) {
                    Path projectDir = backendDir.getParent();
                    if (projectDir != null) {
                        return projectDir.toAbsolutePath().normalize();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Path findProjectBaseDir(Path start) {
        Path current = start;
        while (current != null) {
            if (Files.isDirectory(current.resolve("backend")) && Files.isDirectory(current.resolve("frontend"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private long resolveMaxSizeBytes() {
        int mb = Math.max(1, systemConfigService.getNumber("file.upload.max_size_mb", java.math.BigDecimal.valueOf(100)).intValue());
        return mb * 1024L * 1024L;
    }

    private boolean isPreviewable(SystemFile file) {
        if (file == null) return false;
        Set<String> previewableExtensions = resolvePreviewableExtensions();
        if (previewableExtensions.isEmpty()) return false;
        return previewableExtensions.contains(normalizeExtension(file.getExtension()));
    }

    private void validateExtensionAllowed(String extension) {
        Set<String> allowedExtensions = resolveAllowedExtensions();
        if (allowedExtensions.isEmpty()) return;
        String safeExtension = normalizeExtension(extension);
        if (!allowedExtensions.contains(safeExtension)) {
            throw new RuntimeException("当前文件类型不允许上传");
        }
    }

    private Set<String> resolveAllowedExtensions() {
        return parseExtensionConfig(systemConfigService.getString(
                "file.allowed_extensions",
                "pdf,png,jpg,jpeg,gif,webp,bmp,svg,txt,md,doc,docx,xls,xlsx,ppt,pptx,zip,rar,7z"
        ));
    }

    private Set<String> resolvePreviewableExtensions() {
        return parseExtensionConfig(systemConfigService.getString(
                "file.previewable_extensions",
                "pdf,png,jpg,jpeg,gif,webp,bmp,svg"
        ));
    }

    private Set<String> parseExtensionConfig(String value) {
        Set<String> result = new LinkedHashSet<>();
        String text = normalize(value);
        if (text.isEmpty()) return result;
        for (String item : text.split(",")) {
            String normalized = normalizeExtension(item);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private String buildObjectKey(String extension) {
        String prefix = KEY_TIME.format(LocalDateTime.now());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String ext = extension.isEmpty() ? "" : "." + extension;
        return prefix + "_" + random + ext;
    }

    private String detectExtension(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) return "";
        return originalName.substring(index + 1).trim().toLowerCase();
    }

    private String calculateFileHash(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, len);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new RuntimeException("计算文件摘要失败");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeExtension(String value) {
        return normalize(value).replaceFirst("^\\.", "").toLowerCase();
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) return 1;
        if (status != 0 && status != 1) throw new RuntimeException("状态不正确");
        return status;
    }

    private LocalDateTime parseStart(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.trim()).atStartOfDay();
    }

    private LocalDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay().minusNanos(1);
    }
}
