package com.bid.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class MiniMaxTextExtractService {

    @Value("${app.minimax.api-key}")
    private String apiKey;

    @Value("${app.minimax.api-url:https://api.minimax.chat}")
    private String apiUrl;

    @Value("${app.pdf-extract.script:C:/UPQQ/pdf_extract.py}")
    private String pdfExtractScript;

    @Value("${app.minimax.max-text-length:250000}")
    private int maxTextLength;

    @Autowired
    private PromptTemplateService promptTemplateService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Returns a map with keys: text (String), textLength (Integer), pageCount (Integer)
     */
    public Map<String, Object> extractTextFromPdf(String filePath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("python", "-X", "utf8", pdfExtractScript, filePath);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("PDF\u63d0\u53d6\u811a\u672f\u6267\u884c\u8d85\u65f6\uff0c\u8d85\u8fc75\u5206\u949f");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("PDF\u63d0\u53d6\u811a\u672f\u6267\u884c\u5931\u8d25\uff0c\u9000\u51fa\u7801: " + exitCode);
        }

        String result = output.toString();
        Map map = objectMapper.readValue(result, Map.class);

        if (map.containsKey("error")) {
            String error = (String) map.get("error");
            if ("EMPTY".equals(error)) {
                throw new RuntimeException("\u8be5\u6587\u4ef6\u4e3a\u626b\u63cf\u4ef6\u6216\u65e0\u6587\u5b57\u5c42\uff0c\u8bf7\u4e0a\u4f20\u6587\u5b57\u7248PDF");
            }
            throw new RuntimeException("PDF\u63d0\u53d6\u5931\u8d25: " + map.get("error"));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("text", (String) map.get("text"));
        res.put("textLength", map.get("textLength") != null ? ((Number) map.get("textLength")).intValue() : 0);
        res.put("pageCount", map.get("pageCount") != null ? ((Number) map.get("pageCount")).intValue() : 0);
        return res;
    }

    public Map<String, Object> extract(String pdfText) throws Exception {
        String truncated = pdfText.length() > maxTextLength ? pdfText.substring(0, maxTextLength) : pdfText;
        String userPrompt = promptTemplateService.buildPrompt(truncated);
        String systemPrompt = promptTemplateService.getSystemPrompt();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "MiniMax-M2.7");
        
        // Build messages array: system prompt (if any) + user prompt
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("messages", new Object[]{
                java.util.Map.of("role", "system", "content", systemPrompt),
                java.util.Map.of("role", "user", "content", userPrompt)
            });
        } else {
            body.put("messages", new Object[]{
                java.util.Map.of("role", "user", "content", userPrompt)
            });
        }
        body.put("temperature", 0.1);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.exchange(
            apiUrl + "/v1/text/chatcompletion_v2",
            HttpMethod.POST,
            entity,
            String.class
        );

        return parseResponse(resp.getBody());
    }

    private Map<String, Object> parseResponse(String jsonStr) throws Exception {
        JsonNode root = objectMapper.readTree(jsonStr);
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("MiniMax\u8fd4\u56fe\u683c\u5f0f\u5f02\u5e38: " + jsonStr);
        }

        JsonNode msg = choices.get(0).get("message");
        String content = msg.has("content") && !msg.get("content").isNull()
            ? msg.get("content").asText()
            : "";

        content = content.trim();
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("\"")) {
            content = content.substring(3);
        }
        if (content.endsWith("\"")) {
            content = content.substring(0, content.length() - 3);
        }
        content = content.trim();

        return objectMapper.readValue(content, Map.class);
    }
}
