package com.bid.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bid.system.entity.SystemTodo;
import com.bid.system.entity.SystemTodoAssignee;
import com.bid.system.entity.User;
import com.bid.system.mapper.SystemTodoAssigneeMapper;
import com.bid.system.mapper.SystemTodoMapper;
import com.bid.system.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SystemTodoService extends ServiceImpl<SystemTodoMapper, SystemTodo> {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String CONTENT_TEXT = "TEXT";
    private static final String CONTENT_HTML = "HTML";
    private static final String TODO_BIZ_TYPE = "TODO";
    private static final String ATTACHMENT_CATEGORY = "attachment";
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final UserMapper userMapper;
    private final SystemTodoAssigneeMapper todoAssigneeMapper;
    private final SystemMessageService systemMessageService;
    private final SystemFileService systemFileService;

    public SystemTodoService(UserMapper userMapper,
                             SystemTodoAssigneeMapper todoAssigneeMapper,
                             SystemMessageService systemMessageService,
                             SystemFileService systemFileService) {
        this.userMapper = userMapper;
        this.todoAssigneeMapper = todoAssigneeMapper;
        this.systemMessageService = systemMessageService;
        this.systemFileService = systemFileService;
    }

    public Map<String, Object> listMyTodos(int page, int size, String keyword, String status, String priority,
                                           String todoType, Integer overdueOnly, String dateFrom, String dateTo, Long userId) {
        List<Map<String, Object>> records = queryMyTodos(keyword, status, priority, todoType, overdueOnly, dateFrom, dateTo, userId);
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int from = Math.min((safePage - 1) * safeSize, records.size());
        int to = Math.min(from + safeSize, records.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records.subList(from, to));
        result.put("total", records.size());
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("summary", buildSummary(records));
        return result;
    }

    public List<Map<String, Object>> exportMyTodos(String keyword, String status, String priority,
                                                   String todoType, Integer overdueOnly, String dateFrom, String dateTo, Long userId) {
        return queryMyTodos(keyword, status, priority, todoType, overdueOnly, dateFrom, dateTo, userId);
    }

    public Map<String, Object> createTodo(String title, String content, String todoType, String priority,
                                          Long assigneeId, String dueAt, String bizType, Long bizId,
                                          Long creatorId, String creatorName) {
        List<Long> assigneeIds = assigneeId == null ? List.of() : List.of(assigneeId);
        return createTodo(title, content, todoType, priority, assigneeIds, dueAt, bizType, bizId,
                List.of(), CONTENT_TEXT, creatorId, creatorName);
    }

    public Map<String, Object> createTodo(String title, String content, String todoType, String priority,
                                          List<Long> assigneeIds, String dueAt, String bizType, Long bizId,
                                          List<Long> attachmentFileIds, String contentType,
                                          Long creatorId, String creatorName) {
        String safeTitle = normalize(title);
        String safeContent = normalize(content);
        String safeTodoType = upperOrDefault(todoType, "GENERAL");
        String safePriority = upperOrDefault(priority, "MEDIUM");
        String safeContentType = normalizeContentType(contentType);
        if (safeTitle.isEmpty()) throw new RuntimeException("待办标题不能为空");
        if (plainText(safeContent).isEmpty()) throw new RuntimeException("待办内容不能为空");

        List<User> assignees = resolveAssignees(assigneeIds);
        if (assignees.isEmpty()) throw new RuntimeException("请选择处理人");
        User firstAssignee = assignees.get(0);
        LocalDateTime parsedDueAt = parseDateTime(dueAt);

        LocalDateTime now = LocalDateTime.now();
        SystemTodo todo = new SystemTodo();
        todo.setTitle(safeTitle);
        todo.setContent(safeContent);
        todo.setContentType(safeContentType);
        todo.setTodoType(safeTodoType);
        todo.setPriority(safePriority);
        todo.setStatus(STATUS_PENDING);
        todo.setBizType(normalize(bizType));
        todo.setBizId(bizId);
        todo.setAssigneeId(firstAssignee.getId());
        todo.setAssigneeName(displayUserName(firstAssignee));
        todo.setCreatorId(creatorId);
        todo.setCreatorName(normalize(creatorName));
        todo.setDueAt(parsedDueAt);
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);
        save(todo);

        List<Long> targetUserIds = new ArrayList<>();
        for (User assignee : assignees) {
            SystemTodoAssignee row = new SystemTodoAssignee();
            row.setTodoId(todo.getId());
            row.setAssigneeId(assignee.getId());
            row.setAssigneeName(displayUserName(assignee));
            row.setStatus(STATUS_PENDING);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            todoAssigneeMapper.insert(row);
            targetUserIds.add(assignee.getId());
        }

        int attachmentCount = systemFileService.linkExistingFiles(todo.getId(), attachmentFileIds, TODO_BIZ_TYPE, ATTACHMENT_CATEGORY, creatorId);
        systemMessageService.sendMessage(
                safeTitle,
                safeContent,
                TODO_BIZ_TYPE,
                "USER",
                targetUserIds,
                List.of(),
                List.of(),
                safeContentType,
                creatorId,
                creatorName,
                TODO_BIZ_TYPE,
                todo.getId(),
                "/todo?todoId=" + todo.getId()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todoId", todo.getId());
        result.put("assigneeId", todo.getAssigneeId());
        result.put("assigneeIds", targetUserIds);
        result.put("assigneeCount", targetUserIds.size());
        result.put("attachmentCount", attachmentCount);
        result.put("status", todo.getStatus());
        return result;
    }

    public void updateMyTodoStatus(Long todoId, String status, Long userId) {
        if (todoId == null || userId == null) throw new RuntimeException("待办不存在");
        SystemTodo todo = getById(todoId);
        if (todo == null) throw new RuntimeException("待办不存在");
        SystemTodoAssignee assignee = requireMyAssignee(todoId, userId);
        String nextStatus = normalizeStatus(status);
        if (nextStatus.equals(assignee.getStatus())) return;
        LocalDateTime now = LocalDateTime.now();
        assignee.setStatus(nextStatus);
        assignee.setProcessedAt(STATUS_DONE.equals(nextStatus) ? now : null);
        assignee.setUpdatedAt(now);
        todoAssigneeMapper.updateById(assignee);
        refreshTodoAggregateStatus(todo, now);
    }

    public Map<String, Object> getMyTodoDetail(Long todoId, Long userId) {
        if (todoId == null || userId == null) throw new RuntimeException("待办不存在");
        SystemTodo todo = getById(todoId);
        if (todo == null) throw new RuntimeException("待办不存在");
        SystemTodoAssignee assignee = requireMyAssignee(todoId, userId);
        return buildRecord(todo, assignee);
    }

    private List<Map<String, Object>> queryMyTodos(String keyword, String status, String priority,
                                                   String todoType, Integer overdueOnly, String dateFrom, String dateTo, Long userId) {
        if (userId == null) throw new RuntimeException("用户不存在");
        String safeStatus = normalize(status).toUpperCase();
        LambdaQueryWrapper<SystemTodoAssignee> assigneeQuery = new LambdaQueryWrapper<SystemTodoAssignee>()
                .eq(SystemTodoAssignee::getAssigneeId, userId);
        if (!safeStatus.isEmpty()) {
            assigneeQuery.eq(SystemTodoAssignee::getStatus, normalizeStatus(safeStatus));
        }
        List<SystemTodoAssignee> assigneeRows = todoAssigneeMapper.selectList(assigneeQuery);
        if (assigneeRows.isEmpty()) return List.of();

        Map<Long, SystemTodoAssignee> assigneeMap = new LinkedHashMap<>();
        List<Long> todoIds = new ArrayList<>();
        for (SystemTodoAssignee assignee : assigneeRows) {
            if (assignee.getTodoId() == null || assigneeMap.containsKey(assignee.getTodoId())) continue;
            assigneeMap.put(assignee.getTodoId(), assignee);
            todoIds.add(assignee.getTodoId());
        }
        if (todoIds.isEmpty()) return List.of();

        Map<Long, SystemTodo> todoMap = new LinkedHashMap<>();
        for (SystemTodo todo : listByIds(todoIds)) {
            todoMap.put(todo.getId(), todo);
        }

        String kw = normalize(keyword);
        String safePriority = normalize(priority).toUpperCase();
        String safeTodoType = normalize(todoType).toUpperCase();
        LocalDateTime from = parseDateFrom(dateFrom);
        LocalDateTime to = parseDateTo(dateTo);

        List<Map<String, Object>> records = new ArrayList<>();
        for (Long todoId : todoIds) {
            SystemTodo todo = todoMap.get(todoId);
            if (todo == null) continue;
            SystemTodoAssignee assignee = assigneeMap.get(todoId);
            if (!kw.isEmpty()) {
                String title = normalize(todo.getTitle());
                String content = plainText(todo.getContent());
                if (!title.contains(kw) && !content.contains(kw)) continue;
            }
            if (!safePriority.isEmpty() && !safePriority.equalsIgnoreCase(normalize(todo.getPriority()))) continue;
            if (!safeTodoType.isEmpty() && !safeTodoType.equalsIgnoreCase(normalize(todo.getTodoType()))) continue;
            if (from != null && (todo.getCreatedAt() == null || todo.getCreatedAt().isBefore(from))) continue;
            if (to != null && (todo.getCreatedAt() == null || todo.getCreatedAt().isAfter(to))) continue;
            if (isTrue(overdueOnly) && !isOverdue(todo, assignee.getStatus())) continue;
            records.add(buildRecord(todo, assignee));
        }

        records.sort(Comparator
                .comparing((Map<String, Object> record) -> (LocalDateTime) record.get("dueAt"), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(record -> (LocalDateTime) record.get("createdAt"), Comparator.nullsLast(Comparator.reverseOrder())));
        return records;
    }

    private void refreshTodoAggregateStatus(SystemTodo todo, LocalDateTime now) {
        List<SystemTodoAssignee> assignees = listAssignees(todo.getId());
        if (assignees.isEmpty()) return;
        boolean allDone = true;
        boolean hasProcessing = false;
        for (SystemTodoAssignee assignee : assignees) {
            if (!STATUS_DONE.equals(assignee.getStatus())) allDone = false;
            if (STATUS_PROCESSING.equals(assignee.getStatus())) hasProcessing = true;
        }
        String aggregateStatus = allDone ? STATUS_DONE : (hasProcessing ? STATUS_PROCESSING : STATUS_PENDING);
        todo.setStatus(aggregateStatus);
        todo.setProcessedAt(STATUS_DONE.equals(aggregateStatus) ? now : null);
        todo.setUpdatedAt(now);
        updateById(todo);
    }

    private Map<String, Object> buildRecord(SystemTodo todo, SystemTodoAssignee myAssignee) {
        String personalStatus = myAssignee == null ? normalizeStatusOrDefault(todo.getStatus()) : normalizeStatusOrDefault(myAssignee.getStatus());
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", todo.getId());
        record.put("title", todo.getTitle());
        record.put("content", todo.getContent());
        record.put("contentText", plainText(todo.getContent()));
        record.put("contentType", normalizeContentType(todo.getContentType()));
        record.put("todoType", todo.getTodoType());
        record.put("priority", todo.getPriority());
        record.put("status", personalStatus);
        record.put("aggregateStatus", todo.getStatus());
        record.put("bizType", todo.getBizType());
        record.put("bizId", todo.getBizId());
        record.put("assigneeId", myAssignee == null ? todo.getAssigneeId() : myAssignee.getAssigneeId());
        record.put("assigneeName", myAssignee == null ? todo.getAssigneeName() : myAssignee.getAssigneeName());
        record.put("creatorId", todo.getCreatorId());
        record.put("creatorName", todo.getCreatorName());
        record.put("dueAt", todo.getDueAt());
        record.put("processedAt", myAssignee == null ? todo.getProcessedAt() : myAssignee.getProcessedAt());
        record.put("createdAt", todo.getCreatedAt());
        record.put("updatedAt", todo.getUpdatedAt());
        record.put("overdue", isOverdue(todo, personalStatus));
        record.put("assignees", listAssignees(todo.getId()).stream().map(this::buildAssigneeRecord).toList());
        record.put("attachments", systemFileService.listLinkedFiles(TODO_BIZ_TYPE, todo.getId(), ATTACHMENT_CATEGORY));
        return record;
    }

    private Map<String, Object> buildAssigneeRecord(SystemTodoAssignee assignee) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("assigneeId", assignee.getAssigneeId());
        record.put("assigneeName", assignee.getAssigneeName());
        record.put("status", assignee.getStatus());
        record.put("processedAt", assignee.getProcessedAt());
        return record;
    }

    private Map<String, Object> buildSummary(List<Map<String, Object>> records) {
        int pending = 0;
        int processing = 0;
        int done = 0;
        int overdue = 0;
        for (Map<String, Object> record : records) {
            String status = String.valueOf(record.get("status"));
            switch (status) {
                case STATUS_PENDING -> pending++;
                case STATUS_PROCESSING -> processing++;
                case STATUS_DONE -> done++;
                default -> {
                }
            }
            if (Boolean.TRUE.equals(record.get("overdue"))) overdue++;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pending", pending);
        summary.put("processing", processing);
        summary.put("done", done);
        summary.put("overdue", overdue);
        return summary;
    }

    private List<User> resolveAssignees(List<Long> assigneeIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (assigneeIds != null) {
            for (Long assigneeId : assigneeIds) {
                if (assigneeId != null) uniqueIds.add(assigneeId);
            }
        }
        if (uniqueIds.isEmpty()) throw new RuntimeException("请选择处理人");
        List<User> users = userMapper.selectBatchIds(uniqueIds);
        Map<Long, User> userMap = new LinkedHashMap<>();
        for (User user : users) {
            if (user != null && user.getId() != null) userMap.put(user.getId(), user);
        }
        List<User> enabledUsers = new ArrayList<>();
        for (Long userId : uniqueIds) {
            User user = userMap.get(userId);
            if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                enabledUsers.add(user);
            }
        }
        if (enabledUsers.isEmpty()) throw new RuntimeException("没有可派发的有效处理人");
        return enabledUsers;
    }

    private SystemTodoAssignee requireMyAssignee(Long todoId, Long userId) {
        SystemTodoAssignee assignee = todoAssigneeMapper.selectOne(new LambdaQueryWrapper<SystemTodoAssignee>()
                .eq(SystemTodoAssignee::getTodoId, todoId)
                .eq(SystemTodoAssignee::getAssigneeId, userId)
                .last("LIMIT 1"));
        if (assignee == null) throw new RuntimeException("只能查看或处理自己的待办");
        return assignee;
    }

    private List<SystemTodoAssignee> listAssignees(Long todoId) {
        if (todoId == null) return List.of();
        return todoAssigneeMapper.selectList(new LambdaQueryWrapper<SystemTodoAssignee>()
                .eq(SystemTodoAssignee::getTodoId, todoId)
                .orderByAsc(SystemTodoAssignee::getId));
    }

    private boolean isOverdue(SystemTodo todo, String status) {
        return todo.getDueAt() != null
                && !STATUS_DONE.equals(normalizeStatusOrDefault(status))
                && todo.getDueAt().isBefore(LocalDateTime.now());
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }

    private String normalizeStatus(String status) {
        String value = normalize(status).toUpperCase();
        if (!STATUS_PENDING.equals(value) && !STATUS_PROCESSING.equals(value) && !STATUS_DONE.equals(value)) {
            throw new RuntimeException("待办状态不正确");
        }
        return value;
    }

    private String normalizeStatusOrDefault(String status) {
        String value = normalize(status).toUpperCase();
        if (STATUS_PROCESSING.equals(value) || STATUS_DONE.equals(value)) return value;
        return STATUS_PENDING;
    }

    private String normalizeContentType(String contentType) {
        String value = normalize(contentType).toUpperCase();
        if (CONTENT_HTML.equals(value)) return CONTENT_HTML;
        return CONTENT_TEXT;
    }

    private String displayUserName(User user) {
        String realName = normalize(user.getRealName());
        return realName.isEmpty() ? user.getUsername() : realName;
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

    private String upperOrDefault(String value, String defaultValue) {
        String text = normalize(value).toUpperCase();
        return text.isEmpty() ? defaultValue : text;
    }

    private LocalDateTime parseDateTime(String value) {
        String text = normalize(value);
        if (text.isEmpty()) return null;
        String normalized = text.length() == 16 ? text + ":00" : text;
        return LocalDateTime.parse(normalized.replace(" ", "T"));
    }

    private LocalDateTime parseDateFrom(String value) {
        String text = normalize(value);
        if (text.isEmpty()) return null;
        return LocalDate.parse(text).atStartOfDay();
    }

    private LocalDateTime parseDateTo(String value) {
        String text = normalize(value);
        if (text.isEmpty()) return null;
        return LocalDate.parse(text).plusDays(1).atStartOfDay().minusSeconds(1);
    }
}
