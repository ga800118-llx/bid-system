package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.SystemMessage;
import com.bid.system.entity.SystemMessageReceiver;
import com.bid.system.entity.User;
import com.bid.system.mapper.SystemMessageMapper;
import com.bid.system.mapper.SystemMessageReceiverMapper;
import com.bid.system.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SystemMessageService extends ServiceImpl<SystemMessageMapper, SystemMessage> {

    private static final String TARGET_ALL = "ALL";
    private static final String TARGET_USER = "USER";
    private static final String CONTENT_TEXT = "TEXT";
    private static final String CONTENT_HTML = "HTML";
    private static final String RECEIVER_TO = "TO";
    private static final String RECEIVER_CC = "CC";
    private static final String MESSAGE_BIZ_TYPE = "MESSAGE";
    private static final String ATTACHMENT_CATEGORY = "attachment";
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final SystemMessageReceiverMapper receiverMapper;
    private final UserMapper userMapper;
    private final SystemFileService systemFileService;

    public SystemMessageService(SystemMessageReceiverMapper receiverMapper,
                                UserMapper userMapper,
                                SystemFileService systemFileService) {
        this.receiverMapper = receiverMapper;
        this.userMapper = userMapper;
        this.systemFileService = systemFileService;
    }

    public Map<String, Object> listMyMessages(int page, int size, String keyword, String messageType, Integer readStatus, Long userId) {
        List<Map<String, Object>> records = queryMyMessages(keyword, messageType, readStatus, userId);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int from = Math.min((safePage - 1) * safeSize, records.size());
        int to = Math.min(from + safeSize, records.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records.subList(from, to));
        result.put("total", records.size());
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("unreadCount", unreadCount(userId));
        return result;
    }

    public List<Map<String, Object>> exportMyMessages(String keyword, String messageType, Integer readStatus, Long userId) {
        return queryMyMessages(keyword, messageType, readStatus, userId);
    }

    private List<Map<String, Object>> queryMyMessages(String keyword, String messageType, Integer readStatus, Long userId) {
        if (userId == null) throw new RuntimeException("用户不存在");
        LambdaQueryWrapper<SystemMessageReceiver> receiverQuery = new LambdaQueryWrapper<SystemMessageReceiver>()
                .eq(SystemMessageReceiver::getUserId, userId)
                .isNull(SystemMessageReceiver::getDeletedAt)
                .orderByDesc(SystemMessageReceiver::getCreatedAt);
        if (readStatus != null) {
            receiverQuery.eq(SystemMessageReceiver::getReadStatus, normalizeReadStatus(readStatus));
        }
        List<SystemMessageReceiver> receivers = receiverMapper.selectList(receiverQuery);
        Map<Long, SystemMessage> messageMap = loadMessageMap(receivers.stream().map(SystemMessageReceiver::getMessageId).toList());
        String kw = normalize(keyword);
        String type = normalize(messageType).toUpperCase();

        List<Map<String, Object>> records = new ArrayList<>();
        for (SystemMessageReceiver receiver : receivers) {
            SystemMessage message = messageMap.get(receiver.getMessageId());
            if (message == null) continue;
            if (!kw.isEmpty()) {
                String title = normalize(message.getTitle());
                String content = plainText(message.getContent());
                if (!title.contains(kw) && !content.contains(kw)) continue;
            }
            if (!type.isEmpty() && !type.equalsIgnoreCase(normalize(message.getMessageType()))) continue;
            records.add(buildRecord(message, receiver));
        }
        return records;
    }

    public int unreadCount(Long userId) {
        if (userId == null) return 0;
        return Math.toIntExact(receiverMapper.selectCount(new LambdaQueryWrapper<SystemMessageReceiver>()
                .eq(SystemMessageReceiver::getUserId, userId)
                .isNull(SystemMessageReceiver::getDeletedAt)
                .eq(SystemMessageReceiver::getReadStatus, 0)));
    }

    public void markRead(Long messageId, Long userId) {
        SystemMessageReceiver receiver = requireReceiver(messageId, userId);
        if (receiver.getReadStatus() != null && receiver.getReadStatus() == 1) return;
        receiver.setReadStatus(1);
        receiver.setReadAt(LocalDateTime.now());
        receiverMapper.updateById(receiver);
    }

    public int markAllRead(Long userId) {
        if (userId == null) throw new RuntimeException("用户不存在");
        List<SystemMessageReceiver> unreadList = receiverMapper.selectList(new LambdaQueryWrapper<SystemMessageReceiver>()
                .eq(SystemMessageReceiver::getUserId, userId)
                .isNull(SystemMessageReceiver::getDeletedAt)
                .eq(SystemMessageReceiver::getReadStatus, 0));
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (SystemMessageReceiver receiver : unreadList) {
            receiver.setReadStatus(1);
            receiver.setReadAt(now);
            receiverMapper.updateById(receiver);
            count++;
        }
        return count;
    }

    public void deleteMessage(Long messageId, Long userId) {
        SystemMessageReceiver receiver = requireReceiver(messageId, userId);
        if (receiver.getDeletedAt() != null) return;
        receiver.setDeletedAt(LocalDateTime.now());
        receiverMapper.updateById(receiver);
    }

    public Map<String, Object> sendMessage(String title, String content, String messageType, String targetType,
                                           List<Long> userIds, Long senderId, String senderName,
                                           String bizType, Long bizId, String relatedPath) {
        return sendMessage(title, content, messageType, targetType, userIds, List.of(), List.of(), CONTENT_TEXT,
                senderId, senderName, bizType, bizId, relatedPath);
    }

    public Map<String, Object> sendMessage(String title, String content, String messageType, String targetType,
                                           List<Long> userIds, List<Long> ccUserIds, List<Long> attachmentFileIds, String contentType,
                                           Long senderId, String senderName, String bizType, Long bizId, String relatedPath) {
        String safeTitle = normalize(title);
        String safeContent = normalize(content);
        String safeMessageType = normalize(messageType).toUpperCase();
        String safeTargetType = normalize(targetType).toUpperCase();
        String safeContentType = normalizeContentType(contentType);
        if (safeTitle.isEmpty()) throw new RuntimeException("消息标题不能为空");
        if (plainText(safeContent).isEmpty()) throw new RuntimeException("消息内容不能为空");
        if (safeMessageType.isEmpty()) throw new RuntimeException("消息类型不能为空");
        if (!TARGET_ALL.equals(safeTargetType) && !TARGET_USER.equals(safeTargetType)) {
            throw new RuntimeException("发送对象类型不正确");
        }
        List<User> targets = resolveTargetUsers(safeTargetType, userIds);
        if (targets.isEmpty()) throw new RuntimeException("接收用户不能为空");
        List<User> ccTargets = resolveCcUsers(ccUserIds, targets);

        LocalDateTime now = LocalDateTime.now();
        SystemMessage message = new SystemMessage();
        message.setTitle(safeTitle);
        message.setContent(safeContent);
        message.setContentType(safeContentType);
        message.setMessageType(safeMessageType);
        message.setTargetType(safeTargetType);
        message.setBizType(normalize(bizType));
        message.setBizId(bizId);
        message.setRelatedPath(normalize(relatedPath));
        message.setSenderId(senderId);
        message.setSenderName(normalize(senderName));
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        save(message);

        for (User target : targets) {
            insertReceiver(message.getId(), target.getId(), RECEIVER_TO, now);
        }
        for (User target : ccTargets) {
            insertReceiver(message.getId(), target.getId(), RECEIVER_CC, now);
        }
        int attachmentCount = systemFileService.linkExistingFiles(message.getId(), attachmentFileIds, MESSAGE_BIZ_TYPE, ATTACHMENT_CATEGORY, senderId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messageId", message.getId());
        result.put("targetCount", targets.size());
        result.put("ccCount", ccTargets.size());
        result.put("attachmentCount", attachmentCount);
        result.put("targetType", safeTargetType);
        return result;
    }

    private Map<Long, SystemMessage> loadMessageMap(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return Map.of();
        List<SystemMessage> messages = listByIds(messageIds);
        Map<Long, SystemMessage> map = new LinkedHashMap<>();
        for (SystemMessage message : messages) {
            map.put(message.getId(), message);
        }
        return map;
    }

    private Map<String, Object> buildRecord(SystemMessage message, SystemMessageReceiver receiver) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", message.getId());
        record.put("title", message.getTitle());
        record.put("content", message.getContent());
        record.put("contentText", plainText(message.getContent()));
        record.put("contentType", normalizeContentType(message.getContentType()));
        record.put("messageType", message.getMessageType());
        record.put("targetType", message.getTargetType());
        record.put("bizType", message.getBizType());
        record.put("bizId", message.getBizId());
        record.put("relatedPath", message.getRelatedPath());
        record.put("senderId", message.getSenderId());
        record.put("senderName", message.getSenderName());
        record.put("receiverType", normalizeReceiverType(receiver.getReceiverType()));
        record.put("readStatus", receiver.getReadStatus());
        record.put("readAt", receiver.getReadAt());
        record.put("createdAt", message.getCreatedAt());
        record.put("attachments", systemFileService.listLinkedFiles(MESSAGE_BIZ_TYPE, message.getId(), ATTACHMENT_CATEGORY));
        return record;
    }

    private SystemMessageReceiver requireReceiver(Long messageId, Long userId) {
        if (messageId == null || userId == null) throw new RuntimeException("消息不存在");
        SystemMessageReceiver receiver = receiverMapper.selectOne(new LambdaQueryWrapper<SystemMessageReceiver>()
                .eq(SystemMessageReceiver::getMessageId, messageId)
                .eq(SystemMessageReceiver::getUserId, userId)
                .last("LIMIT 1"));
        if (receiver == null || receiver.getDeletedAt() != null) throw new RuntimeException("消息不存在");
        return receiver;
    }

    private List<User> resolveTargetUsers(String targetType, List<Long> userIds) {
        if (TARGET_ALL.equals(targetType)) {
            return userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getStatus, 1)
                    .orderByAsc(User::getId));
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (userIds != null) {
            for (Long userId : userIds) {
                if (userId != null) uniqueIds.add(userId);
            }
        }
        if (uniqueIds.isEmpty()) throw new RuntimeException("请选择接收用户");
        List<User> users = userMapper.selectBatchIds(uniqueIds);
        List<User> enabledUsers = new ArrayList<>();
        for (User user : users) {
            if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                enabledUsers.add(user);
            }
        }
        if (enabledUsers.isEmpty()) throw new RuntimeException("没有可发送的有效用户");
        return enabledUsers;
    }

    private List<User> resolveCcUsers(List<Long> ccUserIds, List<User> toUsers) {
        Set<Long> toIds = new HashSet<>();
        for (User user : toUsers) {
            if (user.getId() != null) toIds.add(user.getId());
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (ccUserIds != null) {
            for (Long userId : ccUserIds) {
                if (userId != null && !toIds.contains(userId)) uniqueIds.add(userId);
            }
        }
        if (uniqueIds.isEmpty()) return List.of();
        List<User> users = userMapper.selectBatchIds(uniqueIds);
        List<User> enabledUsers = new ArrayList<>();
        for (User user : users) {
            if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                enabledUsers.add(user);
            }
        }
        return enabledUsers;
    }

    private void insertReceiver(Long messageId, Long userId, String receiverType, LocalDateTime now) {
        if (messageId == null || userId == null) return;
        SystemMessageReceiver receiver = new SystemMessageReceiver();
        receiver.setMessageId(messageId);
        receiver.setUserId(userId);
        receiver.setReceiverType(receiverType);
        receiver.setReadStatus(0);
        receiver.setCreatedAt(now);
        receiverMapper.insert(receiver);
    }

    private int normalizeReadStatus(Integer readStatus) {
        if (readStatus == null) return 0;
        if (readStatus != 0 && readStatus != 1) throw new RuntimeException("已读状态不正确");
        return readStatus;
    }

    private String normalizeContentType(String contentType) {
        String value = normalize(contentType).toUpperCase();
        if (CONTENT_HTML.equals(value)) return CONTENT_HTML;
        return CONTENT_TEXT;
    }

    private String normalizeReceiverType(String receiverType) {
        String value = normalize(receiverType).toUpperCase();
        if (RECEIVER_CC.equals(value)) return RECEIVER_CC;
        return RECEIVER_TO;
    }

    private String plainText(String content) {
        String text = normalize(content);
        if (text.isEmpty()) return "";
        text = text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"");
        return HTML_TAG_PATTERN.matcher(text).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
