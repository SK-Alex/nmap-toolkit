package com.nmaptoolkit.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置持久化存储：保存/加载方案 + 命令历史（对标前端 localStorage）
 */
public class ConfigStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), ".nmap-toolkit");

    // 已保存的方案：名称 -> 配置
    private Map<String, Map<String, Object>> presets = new LinkedHashMap<>();
    // 命令历史（最多 20 条）
    private List<String> history = new ArrayList<>();

    public ConfigStore() {
        load();
    }

    public List<String> getHistory() {
        return history;
    }

    public Map<String, Map<String, Object>> getPresets() {
        return presets;
    }

    public void addHistory(String command) {
        history.remove(command);
        history.add(0, command);
        if (history.size() > 20) {
            history = new ArrayList<>(history.subList(0, 20));
        }
        save();
    }

    public void clearHistory() {
        history.clear();
        save();
    }

    public void savePreset(String name, Map<String, Object> config) {
        presets.put(name, config);
        save();
    }

    public void removePreset(String name) {
        presets.remove(name);
        save();
    }

    private Path dataFile() {
        return DATA_DIR.resolve("config.json");
    }

    private void load() {
        try {
            if (!Files.exists(dataFile())) return;
            String json = Files.readString(dataFile(), StandardCharsets.UTF_8);
            Map<String, Object> root = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
            if (root == null) return;
            if (root.get("presets") instanceof Map<?, ?> p) {
                presets = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : p.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> val = (Map<String, Object>) e.getValue();
                    presets.put(String.valueOf(e.getKey()), val);
                }
            }
            if (root.get("history") instanceof List<?> h) {
                history = new ArrayList<>();
                for (Object o : h) history.add(String.valueOf(o));
            }
        } catch (IOException ignored) {
        }
    }

    private void save() {
        try {
            Files.createDirectories(DATA_DIR);
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("presets", presets);
            root.put("history", history);
            Files.writeString(dataFile(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
