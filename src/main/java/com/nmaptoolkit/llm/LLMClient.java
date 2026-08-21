package com.nmaptoolkit.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 大模型客户端：调用 OpenAI 兼容的 Chat Completions 接口。
 * 支持任意 OpenAI 兼容服务（OpenAI、DeepSeek、通义千问、Kimi、Ollama 等）。
 */
public class LLMClient {

    private final LLMConfig config;
    private final HttpClient httpClient;

    public LLMClient(LLMConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 同步调用大模型，返回分析结果文本
     *
     * @param systemPrompt 系统提示词（内置模板）
     * @param userContent  用户内容（扫描结果）
     * @throws Exception 调用失败时抛出
     */
    public String chat(String systemPrompt, String userContent) throws Exception {
        String endpoint = normalizeBaseUrl(config.baseUrl) + "/chat/completions";

        JsonObject body = new JsonObject();
        body.addProperty("model", config.model);

        JsonArray messages = new JsonArray();
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);
        messages.add(sysMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userContent);
        messages.add(userMsg);

        body.add("messages", messages);
        body.addProperty("temperature", 0.3);
        body.addProperty("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("API 调用失败 (HTTP " + response.statusCode() + "): " + extractError(response.body()));
        }

        return parseContent(response.body());
    }

    /**
     * 异步调用大模型
     */
    public void chatAsync(String systemPrompt, String userContent,
                          java.util.function.Consumer<String> onSuccess,
                          java.util.function.Consumer<String> onError) {
        Thread t = new Thread(() -> {
            try {
                String result = chat(systemPrompt, userContent);
                if (onSuccess != null) onSuccess.accept(result);
            } catch (Exception e) {
                if (onError != null) onError.accept(e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private String normalizeBaseUrl(String url) {
        String u = url == null ? "" : url.trim();
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        if (u.isEmpty()) u = "https://api.openai.com/v1";
        return u;
    }

    private String parseContent(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject first = choices.get(0).getAsJsonObject();
                JsonObject message = first.getAsJsonObject("message");
                if (message != null && message.has("content")) {
                    String content = message.get("content").getAsString();
                    return content != null ? content : "";
                }
            }
            return "";
        } catch (Exception e) {
            return responseBody;
        }
    }

    private String extractError(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject error = root.getAsJsonObject("error");
            if (error != null && error.has("message")) {
                return error.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }
}
