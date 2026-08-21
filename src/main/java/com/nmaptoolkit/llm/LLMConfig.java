package com.nmaptoolkit.llm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 大模型配置：baseUrl、apiKey、模型名，持久化到 ~/.nmap-toolkit/llm-config.json
 */
public class LLMConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = Paths.get(System.getProperty("user.home"), ".nmap-toolkit", "llm-config.json");

    public String baseUrl = "https://api.openai.com/v1";   // OpenAI 兼容接口地址
    public String apiKey = "";                              // API Key
    public String model = "gpt-4o-mini";                    // 模型名
    public String promptTemplate = "security_analysis";     // 当前使用的提示词模板

    public void load() {
        try {
            if (!Files.exists(CONFIG_FILE)) return;
            String json = Files.readString(CONFIG_FILE, StandardCharsets.UTF_8);
            Map<String, Object> m = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
            if (m == null) return;
            if (m.get("baseUrl") != null) baseUrl = String.valueOf(m.get("baseUrl"));
            if (m.get("apiKey") != null) apiKey = String.valueOf(m.get("apiKey"));
            if (m.get("model") != null) model = String.valueOf(m.get("model"));
            if (m.get("promptTemplate") != null) promptTemplate = String.valueOf(m.get("promptTemplate"));
        } catch (IOException ignored) {
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("baseUrl", baseUrl);
            m.put("apiKey", apiKey);
            m.put("model", model);
            m.put("promptTemplate", promptTemplate);
            Files.writeString(CONFIG_FILE, GSON.toJson(m), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
