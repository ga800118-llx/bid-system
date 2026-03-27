package com.bid.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.cert.X509Certificate;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.Loader;

@Service
public class MiniMaxExtractService {

    // MiniMax 官方 API 地址（MiniMax-M2.7 模型）
    private static final String API_URL = "https://api.minimax.chat/v1/text/chatcompletion_v2";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // MiniMax Token Plan Key
    private static final String API_KEY = "d668d25b-3724-4b8c-b8a1-ba785bffe270";
    // MiniMax-M2.7 推理模型
    private static final String MODEL = "MiniMax-M2.7";

    // 信息提取 Prompt
    // 注意：MiniMax-M2.7 推理模型会先输出 reasoning_content（思考过程），
    // 实际答案在 content 中，若 content 为空则从 reasoning_content 提取
    private static final String EXTRACT_PROMPT =
        "你是一个招投标文件信息提取专家。请从以下文本中提取结构化信息，返回标准JSON，只返回JSON不要其他内容。" +
        "字段列表：projectName(项目名称)、projectCode(项目编号)、biddingAgency(招标代理机构)、clientUnit(发标单位)、" +
        "bidOpenTime(开标时间YYYY-MM-DD)、complaintDeadline(质疑截止时间YYYY-MM-DD)、" +
        "ceilingPrice(拦标价数字，单位元)、floorPrice(下限价数字，单位元)、" +
        "contractPayment(合同付款方式)、expertComposition(专家组成)、priceScoreMethod(价格分计算方法)、" +
        "subjectiveScoreDetails(主观分评审细则——仅包含评审时需要评委主观判断的条目，如实施方案评分、售后服务方案、整体评价等，客观评分项如业绩、资质证书数量等不要写入此字段)、" +
        "bidBond(投标保证金数字，单位元)、" +
        "performanceBond(履约保证金——如果写的是百分比或公式如合同价的5%，则填比例数字如0.05；如果是固定金额则填具体数字)。" +
        "如果某个字段在文件中不存在，设为null，不要省略字段。只返回JSON。";

    static {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) {}
                public void checkServerTrusted(X509Certificate[] xcs, String string) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hn, sn) -> true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Map<String, Object> extractFromText(String text) throws Exception {
        String truncated = text.length() > 8000 ? text.substring(0, 8000) : text;
        return callChatApi(EXTRACT_PROMPT + truncated);
    }

    public Map<String, Object> extractFromFile(MultipartFile file) throws Exception {
        return extractFromImages(file.getBytes());
    }

    private Map<String, Object> extractFromImages(byte[] pdfBytes) throws Exception {
        PDDocument document = Loader.loadPDF(pdfBytes);
        int pageCount = document.getNumberOfPages();
        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < Math.min(pageCount, 5); i++) {
            BufferedImage img = new PDFRenderer(document).renderImageWithDPI(i, 150);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            imageUrls.add("data:image/jpeg;base64," + base64);
        }
        document.close();
        return callVisionApi(imageUrls);
    }

    private Map<String, Object> callVisionApi(List<String> imageUrls) throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        List<Map<String, Object>> contentList = new ArrayList<>();
        for (String imgUrl : imageUrls) {
            Map<String, Object> imgUrlObj = new HashMap<>();
            imgUrlObj.put("url", imgUrl);
            Map<String, Object> imgContent = new HashMap<>();
            imgContent.put("type", "image_url");
            imgContent.put("image_url", imgUrlObj);
            contentList.add(imgContent);
        }
        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", EXTRACT_PROMPT);
        contentList.add(textContent);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", contentList);
        messages.add(userMsg);

        Map<String, Object> body = new HashMap<>();
        body.put("model", MODEL);
        body.put("messages", messages);
        body.put("max_tokens", 8192);
        body.put("temperature", 0.1);

        String resp = doPost(body);
        System.err.println("MiniMax vision response length: " + resp.length());
        JsonNode root = MAPPER.readTree(resp);
        return parseJsonResponse(extractContent(root));
    }

    private Map<String, Object> callChatApi(String prompt) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", MODEL);
        body.put("messages", List.of(java.util.Map.of("role", "user", "content", prompt)));
        body.put("max_tokens", 8192);
        body.put("temperature", 0.1);

        String resp = doPost(body);
        System.err.println("MiniMax text response length: " + resp.length());

        JsonNode root = MAPPER.readTree(resp);
        String content = extractContent(root);

        Files.writeString(
            Paths.get("C:/Users/Administrator/.openclaw/workspace/minimax_resp.txt"),
            content,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );

        if (content.startsWith("```")) {
            int fn = content.indexOf(10);
            int ln = content.lastIndexOf("```");
            if (fn > 0 && ln > fn) content = content.substring(fn + 1, ln).trim();
        }
        return parseJsonResponse(content);
    }

    /**
     * 从 MiniMax API 响应中提取 content。
     * MiniMax-M2.7 为推理模型，若 message.content 为空，
     * 则实际答案在 reasoning_content 中。
     */
    private String extractContent(JsonNode root) {
        JsonNode msg = root.path("choices").path(0).path("message");
        String content = msg.path("content").asText("").trim();
        if (!content.isEmpty()) return content;
        String reasoning = msg.path("reasoning_content").asText("").trim();
        return reasoning;
    }

    private String doPost(Map<String, Object> body) throws Exception {
        URL url = URI.create(API_URL).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(120000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(MAPPER.writeValueAsBytes(body));
        }
        int code = conn.getResponseCode();
        String resp = readResponse(conn);
        if (code != 200) {
            throw new RuntimeException("MiniMax API error " + code + ": " + resp.substring(0, Math.min(200, resp.length())));
        }
        return resp;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private Map<String, Object> parseJsonResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        try {
            JsonNode node = MAPPER.readTree(json);
            node.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                if (v.isTextual()) result.put(e.getKey(), v.asText());
                else if (v.isNumber()) result.put(e.getKey(), new java.math.BigDecimal(v.toString()).stripTrailingZeros().toPlainString());
                else if (v.isNull()) result.put(e.getKey(), null);
                else result.put(e.getKey(), v.toString());
            });
        } catch (Exception e) {
            result.put("_raw", json);
            result.put("_parse_error", e.getMessage());
        }
        return result;
    }
}
