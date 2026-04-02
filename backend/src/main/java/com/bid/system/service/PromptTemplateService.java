package com.bid.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Service
public class PromptTemplateService {

    @Value("${app.prompt-template.path:C:/UPQQ/prompt_template.json}")
    private String templatePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile Map<String, String> cachedTemplate;

    private Map<String, String> loadTemplate() throws Exception {
        if (cachedTemplate != null) return cachedTemplate;
        File f = new File(templatePath);
        if (!f.exists()) {
            throw new RuntimeException("Prompt\u6a21\u677f\u6587\u4ef6\u4e0d\u5b58\u5728: " + templatePath);
        }
        String json = Files.readString(f.toPath());
        JsonNode root = objectMapper.readTree(json);
        Map<String, String> tmpl = new HashMap<>();
        tmpl.put("name", root.get("name").asText());
        tmpl.put("content", root.get("content").asText());
        tmpl.put("fieldDef", root.get("field_def").toString());
        tmpl.put("system", root.has("system") ? root.get("system").asText() : "");
        cachedTemplate = tmpl;
        return cachedTemplate;
    }

    public Map<String, String> getTemplate() throws Exception {
        return loadTemplate();
    }

    public String getSystemPrompt() throws Exception {
        return loadTemplate().get("system");
    }

    public void saveTemplate(String name, String system, String content, String fieldDefJson) throws Exception {
        File f = new File(templatePath);
        JsonNode fieldDefNode = objectMapper.readTree(fieldDefJson);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("name", name);
        root.put("system", system != null ? system : "");
        root.put("content", content);
        root.put("field_def", fieldDefNode);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(f, root);
        cachedTemplate = null;
    }

    public void saveTemplate(String name, String content, String fieldDefJson) throws Exception {
        saveTemplate(name, "", content, fieldDefJson);
    }

    public String buildPrompt(String pdfText) throws Exception {
        Map<String, String> t = loadTemplate();
        String content = t.get("content");
        String fieldDef = buildFieldDefText(t.get("fieldDef"));
        content = content.replace("{content}", pdfText);
        content = content.replace("{field_def}", fieldDef);
        return content;
    }

    private String buildFieldDefText(String fieldDefJson) {
        try {
            JsonNode node = objectMapper.readTree(fieldDefJson);
            JsonNode fields = node.get("fields");
            if (fields == null || !fields.isArray()) {
                return fieldDefJson;
            }
            StringBuilder sb = new StringBuilder();
            for (JsonNode fld : fields) {
                String label = fld.get("label").asText();
                String key = fld.get("key").asText();
                String semantic = fld.has("semantic") ? fld.get("semantic").asText() : "";
                sb.append(" - ").append(label).append(" (").append(key).append(") \n");
                if (!semantic.isEmpty()) {
                    sb.append("   \u610f\u4e49\uff1a").append(semantic).append(" \n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return fieldDefJson;
        }
    }
}
