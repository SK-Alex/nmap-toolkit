package com.nmaptoolkit.ui;

import com.nmaptoolkit.llm.LLMConfig;
import com.nmaptoolkit.llm.PromptLibrary;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Optional;

/**
 * 大模型配置对话框：baseUrl、apiKey、模型名、提示词模板
 */
public class LLMConfigDialog {

    public static void show(java.util.List<String> presets, LLMConfig config) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("大模型配置");
        dialog.setHeaderText("配置 OpenAI 兼容的大模型接口");

        ButtonType saveBtn = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfBaseUrl = new TextField(config.baseUrl);
        tfBaseUrl.setPromptText("https://api.openai.com/v1");
        PasswordField pfApiKey = new PasswordField();
        pfApiKey.setText(config.apiKey);
        pfApiKey.setPromptText("sk-...");

        TextField tfModel = new TextField(config.model);
        tfModel.setPromptText("gpt-4o-mini");

        ComboBox<String> cmbPreset = new ComboBox<>(FXCollections.observableArrayList(presets));
        cmbPreset.setValue(config.promptTemplate);
        cmbPreset.setPrefWidth(400);

        // 常用预设 baseUrl
        ComboBox<String> cmbPresetUrl = new ComboBox<>(FXCollections.observableArrayList(
                "https://api.openai.com/v1",
                "https://api.deepseek.com/v1",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "https://api.moonshot.cn/v1",
                "http://localhost:11434/v1"));
        cmbPresetUrl.setValue(config.baseUrl);
        cmbPresetUrl.setPrefWidth(400);
        cmbPresetUrl.setOnAction(e -> tfBaseUrl.setText(cmbPresetUrl.getValue()));

        grid.add(new Label("API 地址 (Base URL):"), 0, 0);
        grid.add(tfBaseUrl, 1, 0);
        grid.add(new Label("常用预设:"), 0, 1);
        grid.add(cmbPresetUrl, 1, 1);
        grid.add(new Label("API Key:"), 0, 2);
        grid.add(pfApiKey, 1, 2);
        grid.add(new Label("模型名:"), 0, 3);
        grid.add(tfModel, 1, 3);
        grid.add(new Label("分析提示词模板:"), 0, 4);
        grid.add(cmbPreset, 1, 4);

        GridPane.setHgrow(tfBaseUrl, Priority.ALWAYS);
        GridPane.setHgrow(pfApiKey, Priority.ALWAYS);
        GridPane.setHgrow(tfModel, Priority.ALWAYS);
        GridPane.setHgrow(cmbPreset, Priority.ALWAYS);

        Label tip = new Label("支持任意 OpenAI 兼容接口：OpenAI、DeepSeek、通义千问、Kimi、Ollama 等。\n"
                + "提示词模板内置多种预设，可随时切换。");
        tip.setWrapText(true);
        tip.setStyle("-fx-text-fill:#888; -fx-font-size:11px;");
        grid.add(tip, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtn) {
            config.baseUrl = tfBaseUrl.getText().trim();
            config.apiKey = pfApiKey.getText().trim();
            config.model = tfModel.getText().trim();
            config.promptTemplate = cmbPreset.getValue();
            config.save();
        }
    }
}
