package com.bid.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.http.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class MiniMaxTextExtractService {
    private static final Logger log = LoggerFactory.getLogger(MiniMaxTextExtractService.class);

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
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

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
            throw new RuntimeException("PDF提取脚本执行超时，超过5分钟");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("PDF提取脚本执行失败，退出码: " + exitCode);
        }

        String result = output.toString();
        Map map = objectMapper.readValue(result, Map.class);

        if (map.containsKey("error")) {
            String error = (String) map.get("error");
            if ("EMPTY".equals(error)) {
                throw new RuntimeException("该文件为扫描件或无文字层，请上传文字版PDF");
            }
            throw new RuntimeException("PDF提取失败: " + map.get("error"));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("text", (String) map.get("text"));
        res.put("textLength", map.get("textLength") != null ? ((Number) map.get("textLength")).intValue() : 0);
        res.put("pageCount", map.get("pageCount") != null ? ((Number) map.get("pageCount")).intValue() : 0);
        return res;
    }

    public Map<String, Object> extract(String pdfText) throws Exception {
        System.out.println("[DEBUG] extract() called. apiUrl=" + apiUrl);
        System.out.println("[DEBUG] apiKey prefix: " + (apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) : "null"));
        System.out.println("[DEBUG] httpClient version: " + httpClient.version().toString());
        String truncated = pdfText.length() > maxTextLength ? pdfText.substring(0, maxTextLength) : pdfText;
        String userPrompt = promptTemplateService.buildPrompt(truncated);
        String systemPrompt = promptTemplateService.getSystemPrompt();

        Map<String, Object> body = new HashMap<>();
        body.put("model", "MiniMax-M2.7");
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            body.put("messages", new Object[]{
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            });
        } else {
            body.put("messages", new Object[]{
                Map.of("role", "user", "content", userPrompt)
            });
        }
        body.put("temperature", 0.1);

        String jsonBody = objectMapper.writeValueAsString(body);

        String cleanApiUrl = apiUrl != null ? apiUrl.trim() : "https://api.minimax.chat";
        System.out.println("[DEBUG] cleanApiUrl: " + cleanApiUrl);
        String endpoint = cleanApiUrl + "/v1/text/chatcompletion_v2";
        System.out.println("[DEBUG] Full endpoint: " + endpoint);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(endpoint))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[DEBUG] MiniMax response: status=" + resp.statusCode() + " bodyLen=" + (resp.body() != null ? resp.body().length() : "null"));
        if (resp.statusCode() != 200) {
            throw new RuntimeException("MiniMax API调用失败，状态码: " + resp.statusCode() + "，响应: " + resp.body());
        }

        return parseResponse(resp.body());
    }

    private Map<String, Object> parseResponse(String jsonStr) throws Exception {
        JsonNode root = objectMapper.readTree(jsonStr);
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("MiniMax返回格式异常: " + jsonStr);
        }

        JsonNode msg = choices.get(0).get("message");
        String content = msg.has("content") && !msg.get("content").isNull()
            ? msg.get("content").asText()
            : "";

        content = content.trim();
        if (content.startsWith("`json")) {
            content = content.substring(7);
        } else if (content.startsWith("\"") && content.length() > 3) {
            content = content.substring(3);
        }
        if (content.endsWith("\"")) {
            content = content.substring(0, content.length() - 3);
        }
        content = content.trim();

        return objectMapper.readValue(content, Map.class);
    }
}
