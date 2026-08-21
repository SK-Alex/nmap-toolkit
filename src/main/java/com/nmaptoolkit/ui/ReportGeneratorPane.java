package com.nmaptoolkit.ui;

import com.nmaptoolkit.llm.LLMClient;
import com.nmaptoolkit.llm.LLMConfig;
import com.nmaptoolkit.llm.PromptLibrary;
import com.nmaptoolkit.report.HtmlReportGenerator;
import com.nmaptoolkit.report.NmapParser;
import com.nmaptoolkit.report.NmapReport;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 报告生成器界面：解析 Nmap 输出并渲染为 HTML 报告，支持大模型分析与导出
 */
public class ReportGeneratorPane {

    private final BorderPane root = new BorderPane();
    private final NmapParser parser = new NmapParser();
    private final HtmlReportGenerator htmlGen = new HtmlReportGenerator();
    private final LLMConfig llmConfig = new LLMConfig();

    private TextArea taInput;
    private WebView webView;
    private Label lbStatus;
    private String currentHtml = "";
    private NmapReport currentReport;

    // 大模型分析相关
    private Tab aiTab;
    private TextArea taAiResult;
    private String aiAnalysis = "";

    public ReportGeneratorPane() {
        llmConfig.load();
        buildUI();
    }

    private void buildUI() {
        // 顶部工具栏
        ToolBar toolBar = new ToolBar();
        Button btnImport = new Button("导入文件");
        btnImport.setOnAction(e -> importFile());
        Button btnParse = new Button("解析生成报告");
        btnParse.setOnAction(e -> parseAndRender());
        btnParse.getStyleClass().add("primary-btn");
        Button btnExample = new Button("载入示例");
        btnExample.setOnAction(e -> loadExample());
        Button btnAiAnalyze = new Button("大模型分析");
        btnAiAnalyze.setOnAction(e -> aiAnalyze());
        btnAiAnalyze.getStyleClass().add("ai-btn");
        Button btnAiConfig = new Button("大模型配置");
        btnAiConfig.setOnAction(e -> aiConfig());
        Button btnExportHtml = new Button("导出 HTML");
        btnExportHtml.setOnAction(e -> exportHtml());
        Button btnExportPdf = new Button("导出 PDF");
        btnExportPdf.setOnAction(e -> exportPdf());

        lbStatus = new Label("请导入或粘贴 Nmap 输出内容");
        lbStatus.setPadding(new Insets(0, 12, 0, 12));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolBar.getItems().addAll(btnImport, btnParse, btnExample,
                new Separator(), btnAiAnalyze, btnAiConfig,
                new Separator(), btnExportHtml, btnExportPdf, spacer, lbStatus);

        // 左侧：输入区
        taInput = new TextArea();
        taInput.setPromptText("在此粘贴 Nmap 输出，或点击\"导入文件\"...");
        VBox left = new VBox(6, new Label("Nmap 输出内容:"), taInput);
        left.setPadding(new Insets(10));
        VBox.setVgrow(taInput, Priority.ALWAYS);

        // 右侧：报告预览 + AI 分析 两个标签页
        webView = new WebView();
        VBox previewBox = new VBox(6, new Label("报告预览:"), webView);
        previewBox.setPadding(new Insets(10));
        VBox.setVgrow(webView, Priority.ALWAYS);

        taAiResult = new TextArea();
        taAiResult.setEditable(false);
        taAiResult.setPromptText("点击\"大模型分析\"按钮，AI 分析结果将显示在这里...");
        taAiResult.setWrapText(true);
        VBox aiBox = new VBox(6, new Label("AI 分析结果:"), taAiResult);
        aiBox.setPadding(new Insets(10));
        VBox.setVgrow(taAiResult, Priority.ALWAYS);

        TabPane rightTabs = new TabPane();
        Tab previewTab = new Tab("报告预览");
        previewTab.setClosable(false);
        previewTab.setContent(previewBox);
        aiTab = new Tab("AI 分析");
        aiTab.setClosable(false);
        aiTab.setContent(aiBox);
        rightTabs.getTabs().addAll(previewTab, aiTab);

        SplitPane split = new SplitPane(left, rightTabs);
        split.setDividerPositions(0.4);

        root.setTop(toolBar);
        root.setCenter(split);
    }

    /**
     * 供命令生成器调用：直接注入扫描结果并自动解析生成报告
     *
     * @param content Nmap 输出内容
     */
    public void setInput(String content) {
        if (content == null || content.isBlank()) {
            lbStatus.setText("扫描结果为空，无法生成报告");
            return;
        }
        taInput.setText(content);
        parseAndRender();
    }

    /**
     * 供命令生成器调用：仅注入内容，不自动解析
     */
    public void setInputOnly(String content) {
        if (content != null) {
            taInput.setText(content);
            lbStatus.setText("已接收扫描结果，点击\"解析生成报告\"");
        }
    }

    private void importFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择 Nmap 输出文件");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("所有文件", "*.*"),
                new FileChooser.ExtensionFilter("文本/XML", "*.txt", "*.xml", "*.nmap", "*.log"));
        File f = chooser.showOpenDialog(root.getScene().getWindow());
        if (f == null) return;
        try {
            String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            taInput.setText(content);
            lbStatus.setText("已导入: " + f.getName());
        } catch (Exception ex) {
            lbStatus.setText("读取文件失败: " + ex.getMessage());
        }
    }

    private void parseAndRender() {
        String content = taInput.getText();
        if (content == null || content.isBlank()) {
            lbStatus.setText("请先输入或导入 Nmap 输出内容");
            return;
        }
        try {
            NmapReport report = parser.parse(content);
            currentReport = report;
            htmlGen.setAiAnalysis(aiAnalysis);
            currentHtml = htmlGen.generate(report);
            webView.getEngine().loadContent(currentHtml);
            int hosts = report.hosts.size();
            int ports = report.totalPorts();
            lbStatus.setText(String.format("解析完成: %d 个主机, %d 个端口 (严重:%d 高危:%d 中危:%d)",
                    hosts, ports,
                    report.countRisk("critical"), report.countRisk("high"), report.countRisk("medium")));
        } catch (Exception ex) {
            lbStatus.setText("解析失败: " + ex.getMessage());
        }
    }

    /**
     * 大模型分析
     */
    private void aiAnalyze() {
        if (currentReport == null) {
            parseAndRender();
            if (currentReport == null) {
                lbStatus.setText("请先解析生成报告，再进行大模型分析");
                return;
            }
        }
        if (llmConfig.apiKey.isBlank()) {
            showInfo("请先配置大模型 API Key（点击\"大模型配置\"）");
            return;
        }

        // 切换到 AI 分析标签页
        aiTab.getTabPane().getSelectionModel().select(aiTab);
        taAiResult.setText("正在调用大模型分析，请稍候...\n");
        lbStatus.setText("状态: 正在调用大模型分析...");

        PromptLibrary.Template template = PromptLibrary.get(llmConfig.promptTemplate);
        String systemPrompt = template.prompt();
        String userContent = currentReport.toPlainText();

        LLMClient client = new LLMClient(llmConfig);
        client.chatAsync(systemPrompt, userContent,
                result -> javafx.application.Platform.runLater(() -> {
                    aiAnalysis = result;
                    taAiResult.setText(result);
                    lbStatus.setText("状态: 大模型分析完成（提示词: " + template.name() + "）");
                    // 重新渲染报告，融合 AI 分析
                    renderWithAi();
                }),
                error -> javafx.application.Platform.runLater(() -> {
                    taAiResult.setText("大模型分析失败:\n" + error);
                    lbStatus.setText("状态: 大模型分析失败");
                }));
    }

    /**
     * 融合 AI 分析结果重新渲染报告
     */
    private void renderWithAi() {
        if (currentReport == null) return;
        htmlGen.setAiAnalysis(aiAnalysis);
        currentHtml = htmlGen.generate(currentReport);
        webView.getEngine().loadContent(currentHtml);
    }

    /**
     * 打开大模型配置对话框
     */
    private void aiConfig() {
        LLMConfigDialog.show(
                java.util.List.copyOf(PromptLibrary.all().keySet()),
                llmConfig);
        lbStatus.setText("大模型配置已更新");
    }

    private void loadExample() {
        taInput.setText("""
                Starting Nmap 7.94 ( https://nmap.org ) at 2026-08-13 10:00 CST
                Nmap scan report for 192.168.1.1
                Host is up (0.0032s latency).
                Not shown: 995 closed ports
                PORT     STATE SERVICE    VERSION
                22/tcp   open  ssh        OpenSSH 8.9p1
                80/tcp   open  http       Apache httpd 2.4.54
                443/tcp  open  https      nginx 1.22.0
                445/tcp  open  microsoft-ds Microsoft Windows 7 - 10 microsoft-ds
                3306/tcp open  mysql      MySQL 5.7.42

                Nmap scan report for 192.168.1.10
                Host is up (0.0018s latency).
                Not shown: 998 closed ports
                PORT     STATE SERVICE VERSION
                22/tcp   open  ssh     OpenSSH 7.4
                8080/tcp open  http    Apache Tomcat 9.0.71

                Nmap done: 2 IP addresses (2 hosts up) scanned in 3.45 seconds
                """);
        lbStatus.setText("已载入示例数据，点击\"解析生成报告\"");
    }

    private void exportHtml() {
        if (currentHtml.isEmpty()) {
            parseAndRender();
            if (currentHtml.isEmpty()) return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出 HTML 报告");
        chooser.setInitialFileName("nmap-report.html");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML 文件", "*.html"));
        File f = chooser.showSaveDialog(root.getScene().getWindow());
        if (f == null) return;
        try {
            Files.writeString(f.toPath(), currentHtml, StandardCharsets.UTF_8);
            lbStatus.setText("已导出 HTML: " + f.getAbsolutePath());
        } catch (Exception ex) {
            lbStatus.setText("导出失败: " + ex.getMessage());
        }
    }

    private void exportPdf() {
        // 使用 WebView 的打印能力，通过系统打印对话框导出 PDF (macOS 原生支持)
        if (currentHtml.isEmpty()) {
            parseAndRender();
            if (currentHtml.isEmpty()) return;
        }
        webView.getEngine().loadContent(currentHtml);
        // 延迟执行打印，确保内容加载完成
        javafx.animation.PauseTransition delay =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        delay.setOnFinished(e -> {
            javafx.print.PrinterJob job = javafx.print.PrinterJob.createPrinterJob();
            if (job != null) {
                boolean printed = job.showPrintDialog(root.getScene().getWindow());
                if (printed) {
                    job.getJobSettings().setJobName("Nmap安全扫描报告");
                    boolean success = job.printPage(webView);
                    if (success) {
                        job.endJob();
                        lbStatus.setText("PDF 导出完成 (通过打印对话框)");
                    } else {
                        lbStatus.setText("打印失败");
                    }
                }
            } else {
                lbStatus.setText("无法创建打印任务");
            }
        });
        delay.play();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("提示");
        alert.show();
    }

    public BorderPane getRoot() {
        return root;
    }
}
