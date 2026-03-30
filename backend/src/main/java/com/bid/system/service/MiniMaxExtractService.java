package com.bid.system.service;
    private static final String EXTRACT_PROMPT = 'You are an expert at extracting structured information from enterprise bidding documents. Please extract the following fields from the provided document and return a standard JSON object only. Use these exact field names: projectName projectCode biddingAgency clientUnit bidOpenTime complaintDeadline ceilingPrice floorPrice contractPayment expertComposition priceScoreMethod subjectiveScoreDetails bidBond performanceBond. Return ONLY the JSON no explanation.';

    static {
        try {
            SSLContext ctx = SSLContext.getInstance(" TLS\);
 ctx.init(null, new TrustManager[]{ new X509TrustManager() { public void checkClientTrusted(X509Certificate[] xcs, String string) {} public void checkServerTrusted(X509Certificate[] xcs, String string) {} public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; } } }, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, snp) -> true);
        } catch (Exception e) { e.printStackTraceTrace(); }
    }

    public Map<String, Object> extractFromFile(MultipartFile file) throws Exception {
        String fileId = uploadFile(file);
        return callDocumentApi(fileId);
    }

    private String uploadFile(MultipartFile file) throws Exception {
        String boundary = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();
        if (fileName == null) fileName = " document.pdf\;
 byte[] fileBytes = file.getBytes();
 URL url = URI.create(UPLOAD_URL).toURL();
 HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 conn.setRequestMethod(\POST\);
        os.write(fileBytes);
            os.write((" \\r\\n--\ + boundary + \--\\r\\n\).getBytes(StandardCharsets.UTF_8));
 }
 int code = conn.getResponseCode();
 String resp = readResponse(conn);
 if (code != 200) throw new RuntimeException(\MiniMax upload error \ + code + \: \ + resp.substring(0, Math.min(200, resp.length())));
 JsonNode node = MAPPER.readTree(resp);
 String fileId = node.path(\data\).path(\id\).asText();
        if (fileId.isEmpty()) throw new RuntimeException(" No file_id returned: \ + resp);
 return fileId;
 }

 private Map<String, Object> callDocumentApi(String fileId) throws Exception {
 List<Map<String, Object>> messages = new ArrayList<>();
 Map<String, Object> userContent = new HashMap<>();
 userContent.put(\type\, \text\);
 userContent.put(\text\, EXTRACT_PROMPT);
        Map<String, Object> docTool = new HashMap<>();
        docTool.put(" type\, DOCUMENT_TOOL);
 Map<String, Object> docToolDetail = new HashMap<>();
 docToolDetail.put(\tool_input\, Map.of(\file_ids\, List.of(fileId)));
 docTool.put(DOCUMENT_TOOL, docToolDetail);
 List<Object> contentList = new ArrayList<>();
 contentList.add(docTool);
 contentList.add(userContent);
 Map<String, Object> userMsg = new HashMap<>();
        userMsg.put(" role\, \user\);
 userMsg.put(\content\, contentList);
 messages.add(userMsg);
 Map<String, Object> body = new HashMap<>();
 body.put(\model\, MODEL);
 body.put(\messages\, messages);
 body.put(\max_tokens\, 8192);
 body.put(\temperature\, 0.1);
 String resp = doPost(body);
        JsonNode root = MAPPER.readTree(resp);
        String content = root.path(" choices\).path(0).path(\message\).path(\content\).asText(\\).trim();
 content = stripThinkingTags(content);
 return parseJsonResponse(content);
 }

 private String stripThinkingTags(String content) {
 content = content.replaceAll(\\\uFEFF\, \\).trim();
 content = content.replaceAll(\\\u200B\, \\).trim();
        content = content.replaceAll('\\uFEFF', ').trim();
        content = content.replaceAll('\\u200B', ').trim();
        content = content.replaceAll('</think>[^]]*?</think>', ').trim();
        int ajIdx = content.indexOf('{actual_json}');
        if (ajIdx >= 0) content = content.substring(0, ajIdx) + content.substring(ajIdx + '{actual_json}'.length());
        content = content.trim();
        if (content.startsWith('`')) {
            int fn = content.indexOf('\n');
            int ln = content.lastIndexOf('`');
            if (fn > 0 and ln > fn) content = content.substring(fn + 1, ln).trim();
        }
        int jsonStart = content.indexOf('{');
        if (jsonStart >= 0) content = content.substring(jsonStart);
        return content;
    }

    private Map<String, Object> parseJsonResponse(String json) {
        Map<String, Object> result = new HashMap<>();
        try {
            JsonNode node = MAPPER.readTree(json); node.fields().forEachRemaining(e -> {
                JsonNode v = e.getValue();
                if (v.isTextual()) result.put(e.getKey(), v.asText());
                else if (v.isNumber()) result.put(e.getKey(), new java.math.BigDecimal(v.toString()).stripTrailingZeros().toPlainString());
                else if (v.isNull()) result.put(e.getKey(), null);
                else result.put(e.getKey(), v.toString());
            });
        } catch (Exception e) { result.put('_raw', json); result.put('_parse_error', e.getMessage()); }
        return result;
    }

    private String doPost(Map<String, Object> body) throws Exception {
        URL url = URI.create(API_URL).toURL(); HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod('POST'); conn.setRequestProperty('Authorization', 'Bearer ' + API_KEY);
        conn.setRequestProperty('Content-Type', 'application/json; charset=utf-8');
        conn.setDoOutput(true); conn.setConnectTimeout(60000); conn.setReadTimeout(120000);
        try (OutputStream os = conn.getOutputStream()) { os.write(MAPPER.writeValueAsBytes(body)); }
        int code = conn.getResponseCode(); String resp = readResponse(conn);
        if (code != 200) throw new RuntimeException('MiniMax API error ' + code + ': ' + resp.substring(0, Math.min(200, resp.length())));
        return resp;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        try (Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) { s.useDelimiter('\\A'); return s.hasNext() ? s.next() : '; }
    }
}
        try (Scanner s = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) { s.useDelimiter('\\A'); return s.hasNext() ? s.next() : ''; }
