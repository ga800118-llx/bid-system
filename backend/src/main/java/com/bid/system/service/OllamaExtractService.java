package com.bid.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class OllamaExtractService {

    private static final String OLLAMA_URL = "http://192.168.200.159:11434/api/generate";
    private static final String MODEL = "qwen3.5:9b";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String EXTRACT_PROMPT = 
        "你是一个招投标文件信息提取专家。请从以下招标文件中准确提取14个字段信息，返回纯JSON（不带markdown代码块），格式如下：\n" +
        "{\"projectName\":\"项目名称\",\"projectCode\":\"项目编号\",\"biddingAgency\":\"招标代理机构\",\"clientUnit\":\"发标单位（招标人）\"," +
        "\"bidOpenTime\":\"开标时间\",\"complaintDeadline\":\"投诉质疑截止时间\",\"ceilingPrice\":\"拦标价（最高投标限价，元，只保留数字）\"," +
        "\"floorPrice\":\"下限价（元，只保留数字）\",\"contractPayment\":\"合同付款方式\",\"expertComposition\":\"专家的组成\"," +
        "\"priceScoreMethod\":\"价格分评分方式\",\"subjectiveScoreDetails\":\"主观分分值（实施方案、售后服务等）\"," +
        "\"bidBond\":\"投标保证金（元，只保留数字）\",\"performanceBond\":\"履约保证金（元，只保留数字）\"}\n" +
        "如果某字段在文件中找不到或不确定，填null。不要添加任何解释说明，只返回JSON。";

    public Map<String, Object> extractFromPdf(File pdfFile) throws Exception {
        String text = extractPdfText(pdfFile);
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("PDF文本提取失败或文件为空");
        }
        return callOllama(text);
    }

    private String extractPdfText(File file) throws Exception {
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            // 清理OCR识别可能的乱码字符
            return text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim();
        }
    }

    private Map<String, Object> callOllama(String text) throws Exception {
        String truncated = text.length() > 10000 ? text.substring(0, 10000) : text;
        
        String prompt = EXTRACT_PROMPT + "\n\n【招标文件内容（前10000字符）】\n" + truncated;
        
        String jsonPayload = "{\"model\":\"" + MODEL + "\",\"stream\":false,\"options\":{\"temperature\":0.1,\"num_predict\":2000},\"prompt\":\"" + escapeJson(prompt) + "\"}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama调用失败，状态码：" + response.statusCode() + " body:" + response.body().substring(0, Math.min(200, response.body().length())));
        }

        String respBody = response.body();
        JsonNode root = MAPPER.readTree(respBody);
        String rawResponse = root.has("response") ? root.get("response").asText() : "";
        
        // 清理可能的markdown代码块
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastCodeBlock = cleaned.lastIndexOf("```");
            if (firstNewline > 0 && lastCodeBlock > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastCodeBlock).trim();
            }
        }
        
        return parseJsonResponse(cleaned);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Map<String, Object> parseJsonResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        try {
            JsonNode node = MAPPER.readTree(json);
            node.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                if (v.isTextual()) result.put(e.getKey(), v.asText());
                else if (v.isNumber()) result.put(e.getKey(), v.asText());
                else if (v.isNull()) result.put(e.getKey(), null);
                else result.put(e.getKey(), v.toString());
            });
            return result;
        } catch (Exception e) {
            result.put("_raw", json);
            result.put("_error", e.getMessage());
            return result;
        }
    }
}