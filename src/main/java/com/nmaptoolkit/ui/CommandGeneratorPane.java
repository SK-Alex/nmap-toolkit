package com.nmaptoolkit.ui;

import com.nmaptoolkit.model.ConfigStore;
import com.nmaptoolkit.model.ScanConfig;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.util.Optional;

/**
 * 命令生成器界面：分组折叠面板 + 实时命令预览 + 历史/方案管理
 */
public class CommandGeneratorPane {

    private final BorderPane root = new BorderPane();
    private final ScanConfig config = new ScanConfig();
    private final ConfigStore store = new ConfigStore();

    // 控件引用
    private TextField tfTargets, tfExclude, tfPorts, tfPortRatio, tfMinRate, tfMaxRate,
            tfParallelism, tfHostTimeout, tfScript, tfSpoofIp, tfDecoys, tfSourcePort,
            tfProxy, tfDnsServers, tfOutputN, tfOutputX, tfOutputG;
    private Spinner<Integer> spTopPorts;
    private CheckBox cbIpv6, cbTopPorts, cbFast, cbAllPorts, cbNoPing, cbPingOnly, cbSkipHost,
            cbServiceVersion, cbOsDetection, cbScriptDefault, cbVerbose, cbOutputAll,
            cbRandomize, cbFragment;
    private ComboBox<String> cmbScanType, cmbTiming;
    private TextArea taCommand;
    private ListView<String> lvHistory, lvPresets;
    private Label lbScriptSummary, lbLocalSummary;
    private java.util.List<String> selectedBuiltinScripts = new java.util.ArrayList<>();
    private java.util.List<String> selectedLocalScripts = new java.util.ArrayList<>();

    // 命令执行相关
    private final com.nmaptoolkit.util.CommandExecutor executor = new com.nmaptoolkit.util.CommandExecutor();
    private TextArea taExecOutput;
    private Button btnRun, btnStop, btnSendToReport;
    private Label lbExecStatus;
    private java.util.function.Consumer<String> onResultReady;
    private Runnable onSendToReport;

    public CommandGeneratorPane() {
        buildUI();
    }

    /** 扫描结果就绪回调（供 MainWindow 注入） */
    public void setOnResultReady(java.util.function.Consumer<String> cb) { this.onResultReady = cb; }

    /** 发送到报告生成器回调（供 MainWindow 注入，用于切换标签页） */
    public void setOnSendToReport(Runnable cb) { this.onSendToReport = cb; }

    private void buildUI() {
        // 顶部：目标设置
        VBox top = new VBox(8);
        top.setPadding(new Insets(14, 16, 8, 16));

        GridPane targetGrid = new GridPane();
        targetGrid.setHgap(10);
        targetGrid.setVgap(8);

        tfTargets = new TextField();
        tfTargets.setPromptText("例如: 192.168.1.0/24 或 example.com 或 10.0.0.1-50");
        tfExclude = new TextField();
        tfExclude.setPromptText("排除目标，逗号分隔");

        targetGrid.add(new Label("目标 (Target):"), 0, 0);
        targetGrid.add(tfTargets, 1, 0);
        targetGrid.add(new Label("排除 (--exclude):"), 0, 1);
        targetGrid.add(tfExclude, 1, 1);
        GridPane.setHgrow(tfTargets, Priority.ALWAYS);
        GridPane.setHgrow(tfExclude, Priority.ALWAYS);

        // 左侧分组折叠面板
        Accordion accordion = new Accordion();

        // 1. 端口控制
        accordion.getPanes().add(new TitledPane("端口控制", buildPortPane()));
        // 2. 主机发现
        accordion.getPanes().add(new TitledPane("主机发现", buildDiscoveryPane()));
        // 3. 扫描控制
        accordion.getPanes().add(new TitledPane("扫描控制", buildScanControlPane()));
        // 4. 服务与系统探测
        accordion.getPanes().add(new TitledPane("服务与系统探测", buildProbePane()));
        // 5. 输出设置
        accordion.getPanes().add(new TitledPane("输出设置", buildOutputPane()));
        // 6. 伪装/代理/网络
        accordion.getPanes().add(new TitledPane("伪装/代理/网络", buildNetworkPane()));

        // 右侧：命令预览 + 历史 + 方案
        VBox right = new VBox(10);
        right.setPadding(new Insets(10));
        right.setPrefWidth(420);

        Label cmdLabel = new Label("生成的命令:");
        cmdLabel.setFont(Font.font(14));
        taCommand = new TextArea();
        taCommand.setWrapText(true);
        taCommand.setPrefRowCount(4);
        taCommand.setEditable(false);

        Button btnGenerate = new Button("生成命令");
        btnGenerate.setMaxWidth(Double.MAX_VALUE);
        btnGenerate.setOnAction(e -> refreshCommand());
        btnGenerate.getStyleClass().add("primary-btn");

        Button btnCopy = new Button("复制命令");
        btnCopy.setMaxWidth(Double.MAX_VALUE);
        btnCopy.setOnAction(e -> copyCommand());

        // 执行命令 / 中止 / 发送到报告生成器
        btnRun = new Button("执行命令");
        btnRun.setMaxWidth(Double.MAX_VALUE);
        btnRun.setOnAction(e -> runCommand());
        btnRun.getStyleClass().add("run-btn");

        btnStop = new Button("中止执行");
        btnStop.setMaxWidth(Double.MAX_VALUE);
        btnStop.setOnAction(e -> stopCommand());
        btnStop.setDisable(true);

        btnSendToReport = new Button("将扫描结果发送到报告生成器");
        btnSendToReport.setMaxWidth(Double.MAX_VALUE);
        btnSendToReport.setOnAction(e -> sendToReport());
        btnSendToReport.setDisable(true);

        HBox runBox = new HBox(8, btnRun, btnStop);
        lbExecStatus = new Label("状态: 就绪");
        lbExecStatus.setStyle("-fx-text-fill:#555; -fx-font-size:12px;");

        taExecOutput = new TextArea();
        taExecOutput.setEditable(false);
        taExecOutput.setPromptText("命令执行输出将显示在这里...");
        taExecOutput.setPrefRowCount(8);
        taExecOutput.setStyle("-fx-font-family: Menlo, Monaco, monospace; -fx-font-size:12px;");

        // 快捷预设按钮
        HBox quickBox = new HBox(8);
        Button btnQuickWeb = new Button("Web快速");
        btnQuickWeb.setOnAction(e -> quickPreset("web"));
        Button btnQuickFull = new Button("全端口扫描");
        btnQuickFull.setOnAction(e -> quickPreset("full"));
        Button btnQuickStealth = new Button("隐蔽扫描");
        btnQuickStealth.setOnAction(e -> quickPreset("stealth"));
        quickBox.getChildren().addAll(btnQuickWeb, btnQuickFull, btnQuickStealth);

        // 命令历史
        Label histLabel = new Label("命令历史 (最近 20 条):");
        lvHistory = new ListView<>();
        lvHistory.setPrefHeight(140);
        lvHistory.getItems().addAll(store.getHistory());
        lvHistory.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && lvHistory.getSelectionModel().getSelectedItem() != null) {
                taCommand.setText(lvHistory.getSelectionModel().getSelectedItem());
            }
        });

        // 方案管理
        HBox presetBar = new HBox(8);
        TextField tfPresetName = new TextField();
        tfPresetName.setPromptText("方案名称");
        Button btnSavePreset = new Button("保存方案");
        btnSavePreset.setOnAction(e -> savePreset(tfPresetName));
        Button btnLoadPreset = new Button("加载");
        btnLoadPreset.setOnAction(e -> loadPreset());
        Button btnDelPreset = new Button("删除");
        btnDelPreset.setOnAction(e -> deletePreset());
        presetBar.getChildren().addAll(tfPresetName, btnSavePreset);
        HBox.setHgrow(tfPresetName, Priority.ALWAYS);

        lvPresets = new ListView<>();
        lvPresets.setPrefHeight(100);
        lvPresets.getItems().addAll(store.getPresets().keySet());

        VBox presetSection = new VBox(6, presetBar, lvPresets,
                new HBox(8, btnLoadPreset, btnDelPreset));

        right.getChildren().addAll(cmdLabel, taCommand, btnGenerate, btnCopy, quickBox,
                runBox, lbExecStatus, taExecOutput, btnSendToReport,
                histLabel, lvHistory, new Separator(), new Label("已保存方案:"), presetSection);
        VBox.setVgrow(taExecOutput, Priority.ALWAYS);

        // 组合布局
        VBox left = new VBox(8, targetGrid, accordion);
        left.setPadding(new Insets(8, 8, 8, 16));

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.55);

        root.setCenter(split);

        // 监听变化自动刷新命令
        bindAutoRefresh();

        refreshCommand();
    }

    // ---------- 各分组面板 ----------

    private VBox buildPortPane() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        tfPorts = new TextField();
        tfPorts.setPromptText("如 80,443,8000-9000");

        cbTopPorts = new CheckBox("Top Ports (--top-ports)");
        spTopPorts = new Spinner<>(1, 65535, 100);
        spTopPorts.setEditable(true);
        HBox topRow = new HBox(8, cbTopPorts, spTopPorts);

        cbFast = new CheckBox("快速扫描 (-F)");
        cbAllPorts = new CheckBox("全端口扫描 (-p-)");

        tfPortRatio = new TextField();
        tfPortRatio.setPromptText("--port-ratio，如 0.9");

        box.getChildren().addAll(new Label("自定义端口 (-p):"), tfPorts, topRow, cbFast, cbAllPorts,
                new Label("端口比例 (--port-ratio):"), tfPortRatio);
        return box;
    }

    private VBox buildDiscoveryPane() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        cmbScanType = new ComboBox<>(FXCollections.observableArrayList(
                "-sS  SYN 半开扫描", "-sT  TCP 连接扫描", "-sU  UDP 扫描",
                "-sN  NULL 扫描", "-sF  FIN 扫描", "-sX  Xmas 扫描",
                "-sA  ACK 扫描", "-sW  窗口扫描", "-sM  Maimon 扫描",
                "-sY  SCTP 初始扫描", "-sZ  SCTP Cookie 扫描"));
        cmbScanType.setValue("-sS  SYN 半开扫描");

        cbNoPing = new CheckBox("跳过主机发现 (-Pn)");
        cbPingOnly = new CheckBox("仅主机发现 (-sn)");
        cbSkipHost = new CheckBox("仅列出目标 (-sL)");
        cbIpv6 = new CheckBox("IPv6 扫描 (-6)");

        box.getChildren().addAll(new Label("扫描类型:"), cmbScanType, cbNoPing, cbPingOnly, cbSkipHost, cbIpv6);
        return box;
    }

    private VBox buildScanControlPane() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        cmbTiming = new ComboBox<>(FXCollections.observableArrayList(
                "-T0  偏执慢速", "-T1  鬼祟", "-T2  礼貌", "-T3  正常",
                "-T4  激进", "-T5  疯狂"));
        cmbTiming.setValue("-T4  激进");

        tfMinRate = new TextField();
        tfMinRate.setPromptText("--min-rate，如 1000");
        tfMaxRate = new TextField();
        tfMaxRate.setPromptText("--max-rate，如 10000");
        tfParallelism = new TextField();
        tfParallelism.setPromptText("--min-parallelism，如 10");
        tfHostTimeout = new TextField();
        tfHostTimeout.setPromptText("--host-timeout，如 30s");

        box.getChildren().addAll(new Label("时序模板 (-T):"), cmbTiming,
                new Label("最小发包速率 (--min-rate):"), tfMinRate,
                new Label("最大发包速率 (--max-rate):"), tfMaxRate,
                new Label("最小并行度 (--min-parallelism):"), tfParallelism,
                new Label("主机超时 (--host-timeout):"), tfHostTimeout);
        return box;
    }

    private VBox buildProbePane() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        cbServiceVersion = new CheckBox("服务版本探测 (-sV)");
        cbOsDetection = new CheckBox("操作系统探测 (-O)");
        cbScriptDefault = new CheckBox("默认脚本扫描 (-sC)");

        // 脚本选择按钮 + 摘要
        Button btnScriptPicker = new Button("浏览内置脚本库...");
        btnScriptPicker.setMaxWidth(Double.MAX_VALUE);
        btnScriptPicker.setOnAction(e -> openScriptSelector());

        lbScriptSummary = new Label("未选择内置脚本");
        lbScriptSummary.setWrapText(true);
        lbScriptSummary.setStyle("-fx-text-fill:#555; -fx-font-size:12px;");

        // 本地自定义脚本模块
        Button btnLocalScript = new Button("管理本地自定义脚本...");
        btnLocalScript.setMaxWidth(Double.MAX_VALUE);
        btnLocalScript.setOnAction(e -> openScriptSelector());

        lbLocalSummary = new Label("未添加本地脚本");
        lbLocalSummary.setWrapText(true);
        lbLocalSummary.setStyle("-fx-text-fill:#555; -fx-font-size:12px;");

        // 保留手动输入框（高级用法，可直接输入脚本名/路径）
        tfScript = new TextField();
        tfScript.setPromptText("手动输入脚本名/路径 (可选，逗号分隔)");

        box.getChildren().addAll(
                cbServiceVersion, cbOsDetection, cbScriptDefault,
                new Separator(),
                new Label("内置脚本 (--script，点击浏览选择):"),
                btnScriptPicker, lbScriptSummary,
                new Label("本地自定义脚本:"),
                btnLocalScript, lbLocalSummary,
                new Label("手动输入 (高级):"), tfScript);
        return box;
    }

    /**
     * 打开脚本选择对话框（同时管理内置脚本与本地脚本）
     */
    private void openScriptSelector() {
        ScriptSelectorDialog dialog = new ScriptSelectorDialog(selectedBuiltinScripts, selectedLocalScripts);
        java.util.Optional<ScriptSelectorDialog.Result> res = dialog.showAndWait();
        if (res.isPresent()) {
            selectedBuiltinScripts.clear();
            selectedBuiltinScripts.addAll(res.get().builtinScripts);
            selectedLocalScripts.clear();
            selectedLocalScripts.addAll(res.get().localScripts);
            updateScriptSummary();
            refreshCommand();
        }
    }

    private void updateScriptSummary() {
        if (selectedBuiltinScripts.isEmpty()) {
            lbScriptSummary.setText("未选择内置脚本");
        } else {
            lbScriptSummary.setText("已选 " + selectedBuiltinScripts.size() + " 个: "
                    + String.join(", ", selectedBuiltinScripts));
        }
        if (selectedLocalScripts.isEmpty()) {
            lbLocalSummary.setText("未添加本地脚本");
        } else {
            lbLocalSummary.setText("已添加 " + selectedLocalScripts.size() + " 个本地脚本");
        }
    }

    private VBox buildOutputPane() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        cbVerbose = new CheckBox("详细输出 (-v)");
        cbOutputAll = new CheckBox("全部格式输出 (-oA)");
        tfOutputN = new TextField();
        tfOutputN.setPromptText("-oN 普通输出文件");
        tfOutputX = new TextField();
        tfOutputX.setPromptText("-oX XML 输出文件");
        tfOutputG = new TextField();
        tfOutputG.setPromptText("-oG Grepable 输出文件");

        box.getChildren().addAll(cbVerbose, cbOutputAll,
                new Label("普通输出 (-oN):"), tfOutputN,
                new Label("XML 输出 (-oX):"), tfOutputX,
                new Label("Grepable 输出 (-oG):"), tfOutputG);
        return box;
    }

    private VBox buildNetworkPane() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));

        tfSpoofIp = new TextField();
        tfSpoofIp.setPromptText("-S 源地址欺骗");
        tfDecoys = new TextField();
        tfDecoys.setPromptText("-D 诱饵，逗号分隔");
        tfSourcePort = new TextField();
        tfSourcePort.setPromptText("--source-port，如 53");
        tfProxy = new TextField();
        tfProxy.setPromptText("--proxies，如 http://proxy:8080");
        tfDnsServers = new TextField();
        tfDnsServers.setPromptText("--dns-servers，逗号分隔");
        cbRandomize = new CheckBox("随机化主机顺序 (--randomize-hosts)");
        cbFragment = new CheckBox("分片数据包 (-f)");

        box.getChildren().addAll(
                new Label("源地址欺骗 (-S):"), tfSpoofIp,
                new Label("诱饵 (-D):"), tfDecoys,
                new Label("源端口 (--source-port):"), tfSourcePort,
                new Label("代理 (--proxies):"), tfProxy,
                new Label("DNS 服务器 (--dns-servers):"), tfDnsServers,
                cbRandomize, cbFragment);
        return box;
    }

    // ---------- 逻辑 ----------

    private void refreshCommand() {
        syncFromUI();
        taCommand.setText(config.buildCommand());
    }

    private void syncFromUI() {
        config.targets = tfTargets.getText().trim();
        config.excludeTargets = tfExclude.getText().trim();
        config.useIpv6 = cbIpv6.isSelected();

        config.ports = tfPorts.getText().trim();
        config.topPorts = cbTopPorts.isSelected();
        config.topPortsCount = spTopPorts.getValue() == null ? 100 : spTopPorts.getValue();
        config.fastScan = cbFast.isSelected();
        config.allPorts = cbAllPorts.isSelected();
        config.portRatio = tfPortRatio.getText().trim();

        config.noPing = cbNoPing.isSelected();
        config.pingOnly = cbPingOnly.isSelected();
        config.skipHostDiscovery = cbSkipHost.isSelected();
        config.scanType = cmbScanType.getValue() == null ? "-sS" : cmbScanType.getValue().split("\\s+")[0];

        config.timing = cmbTiming.getValue() == null ? "-T4" : cmbTiming.getValue().split("\\s+")[0];
        config.minRate = tfMinRate.getText().trim();
        config.maxRate = tfMaxRate.getText().trim();
        config.parallelism = tfParallelism.getText().trim();
        config.hostTimeout = tfHostTimeout.getText().trim();

        config.serviceVersion = cbServiceVersion.isSelected();
        config.osDetection = cbOsDetection.isSelected();
        config.scriptDefault = cbScriptDefault.isSelected();
        // 内置脚本 = 勾选的脚本 + 手动输入
        String builtin = String.join(",", selectedBuiltinScripts);
        String manual = tfScript.getText().trim();
        if (!manual.isEmpty()) {
            builtin = builtin.isEmpty() ? manual : builtin + "," + manual;
        }
        config.scriptArgs = builtin;
        // 本地脚本
        config.localScripts = String.join(",", selectedLocalScripts);

        config.verbose = cbVerbose.isSelected();
        config.outputAll = cbOutputAll.isSelected();
        config.outputNormal = tfOutputN.getText().trim();
        config.outputXml = tfOutputX.getText().trim();
        config.outputGrep = tfOutputG.getText().trim();

        config.spoofIp = tfSpoofIp.getText().trim();
        config.decoys = tfDecoys.getText().trim();
        config.sourcePort = tfSourcePort.getText().trim();
        config.proxy = tfProxy.getText().trim();
        config.dnsServers = tfDnsServers.getText().trim();
        config.randomizeHosts = cbRandomize.isSelected();
        config.fragment = cbFragment.isSelected();
    }

    private void bindAutoRefresh() {
        // 对所有会改变命令的控件添加监听，自动刷新预览
        tfTargets.textProperty().addListener((o, a, b) -> refreshCommand());
        tfExclude.textProperty().addListener((o, a, b) -> refreshCommand());
        tfPorts.textProperty().addListener((o, a, b) -> refreshCommand());
        tfPortRatio.textProperty().addListener((o, a, b) -> refreshCommand());
        tfMinRate.textProperty().addListener((o, a, b) -> refreshCommand());
        tfMaxRate.textProperty().addListener((o, a, b) -> refreshCommand());
        tfParallelism.textProperty().addListener((o, a, b) -> refreshCommand());
        tfHostTimeout.textProperty().addListener((o, a, b) -> refreshCommand());
        tfScript.textProperty().addListener((o, a, b) -> refreshCommand());
        tfSpoofIp.textProperty().addListener((o, a, b) -> refreshCommand());
        tfDecoys.textProperty().addListener((o, a, b) -> refreshCommand());
        tfSourcePort.textProperty().addListener((o, a, b) -> refreshCommand());
        tfProxy.textProperty().addListener((o, a, b) -> refreshCommand());
        tfDnsServers.textProperty().addListener((o, a, b) -> refreshCommand());
        tfOutputN.textProperty().addListener((o, a, b) -> refreshCommand());
        tfOutputX.textProperty().addListener((o, a, b) -> refreshCommand());
        tfOutputG.textProperty().addListener((o, a, b) -> refreshCommand());

        CheckBox[] boxes = {cbIpv6, cbTopPorts, cbFast, cbAllPorts, cbNoPing, cbPingOnly,
                cbSkipHost, cbServiceVersion, cbOsDetection, cbScriptDefault, cbVerbose,
                cbOutputAll, cbRandomize, cbFragment};
        for (CheckBox cb : boxes) {
            cb.selectedProperty().addListener((o, a, b) -> refreshCommand());
        }
        cmbScanType.valueProperty().addListener((o, a, b) -> refreshCommand());
        cmbTiming.valueProperty().addListener((o, a, b) -> refreshCommand());
        spTopPorts.valueProperty().addListener((o, a, b) -> refreshCommand());
    }

    private void copyCommand() {
        String cmd = taCommand.getText();
        if (cmd == null || cmd.isBlank()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(cmd);
        Clipboard.getSystemClipboard().setContent(content);
        // 记录到历史
        store.addHistory(cmd);
        lvHistory.getItems().setAll(store.getHistory());
        showInfo("命令已复制到剪贴板");
    }

    /**
     * 执行当前生成的命令
     */
    private void runCommand() {
        syncFromUI();
        String cmd = config.buildCommand();
        if (cmd.contains("<target>")) {
            showInfo("请先在顶部填写目标 (Target)");
            return;
        }
        // 定位 nmap（优先系统，否则使用内置）
        com.nmaptoolkit.util.NmapLocator.Location loc = com.nmaptoolkit.util.NmapLocator.locate();
        if (loc == null) {
            showInfo("未找到可用的 nmap（系统未安装，且工具未内置 nmap）");
            return;
        }

        // 记录历史（保留原始命令，便于复制到别的机器）
        store.addHistory(cmd);
        lvHistory.getItems().setAll(store.getHistory());

        // 解析命令行（支持引号）
        java.util.List<String> args = parseCommandLine(cmd);
        if (args.isEmpty()) return;

        // 将命令首 token "nmap" 替换为实际可执行文件路径
        args.set(0, loc.executable);
        // 若使用内置 nmap，注入 --datadir
        if (loc.bundled && loc.dataDir != null) {
            args.add(1, "--datadir");
            args.add(2, loc.dataDir);
        }

        taExecOutput.clear();
        btnRun.setDisable(true);
        btnStop.setDisable(false);
        btnSendToReport.setDisable(true);
        lbExecStatus.setText("状态: 正在执行 (" + (loc.bundled ? "内置 nmap" : "系统 nmap") + ")...");

        executor.execute(args,
                line -> javafx.application.Platform.runLater(() -> {
                    taExecOutput.appendText(line + "\n");
                }),
                exitCode -> javafx.application.Platform.runLater(() -> {
                    btnRun.setDisable(false);
                    btnStop.setDisable(true);
                    if (exitCode == 0) {
                        lbExecStatus.setText("状态: 执行完成 (退出码 0)");
                        btnSendToReport.setDisable(false);
                    } else {
                        lbExecStatus.setText("状态: 执行结束 (退出码 " + exitCode + ")");
                        // 即使失败也允许查看/发送部分输出
                        btnSendToReport.setDisable(false);
                    }
                }));
    }

    /**
     * 中止执行
     */
    private void stopCommand() {
        executor.stop();
        lbExecStatus.setText("状态: 已中止");
        btnRun.setDisable(false);
        btnStop.setDisable(true);
    }

    /**
     * 将扫描结果发送到报告生成器
     */
    private void sendToReport() {
        String output = executor.getOutput();
        if (output == null || output.isBlank()) {
            showInfo("暂无扫描结果可发送");
            return;
        }
        if (onSendToReport != null) onSendToReport.run();
        if (onResultReady != null) onResultReady.accept(output);
    }

    /**
     * 解析命令行字符串为参数数组（支持双引号包裹）
     */
    private java.util.List<String> parseCommandLine(String cmd) {
        java.util.List<String> args = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < cmd.length(); i++) {
            char ch = cmd.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) args.add(current.toString());
        return args;
    }

    private void quickPreset(String type) {
        switch (type) {
            case "web" -> {
                tfPorts.setText("80,443,8000,8080,8443");
                cbServiceVersion.setSelected(true);
                cbScriptDefault.setSelected(true);
                cmbTiming.setValue("-T4  激进");
                cmbScanType.setValue("-sS  SYN 半开扫描");
            }
            case "full" -> {
                cbAllPorts.setSelected(true);
                cbNoPing.setSelected(true);
                cmbTiming.setValue("-T4  激进");
            }
            case "stealth" -> {
                cmbTiming.setValue("-T2  礼貌");
                tfMinRate.setText("10");
                cbFragment.setSelected(true);
                cbRandomize.setSelected(true);
                cmbScanType.setValue("-sS  SYN 半开扫描");
            }
        }
        refreshCommand();
    }

    private void savePreset(TextField nameField) {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showInfo("请输入方案名称");
            return;
        }
        syncFromUI();
        store.savePreset(name, config.toMap());
        lvPresets.getItems().setAll(store.getPresets().keySet());
        nameField.clear();
        showInfo("方案已保存: " + name);
    }

    private void loadPreset() {
        String name = lvPresets.getSelectionModel().getSelectedItem();
        if (name == null) {
            showInfo("请先选择一个方案");
            return;
        }
        java.util.Map<String, Object> m = store.getPresets().get(name);
        if (m != null) {
            config.fromMap(m);
            syncToUI();
            refreshCommand();
            showInfo("已加载方案: " + name);
        }
    }

    private void deletePreset() {
        String name = lvPresets.getSelectionModel().getSelectedItem();
        if (name == null) return;
        store.removePreset(name);
        lvPresets.getItems().setAll(store.getPresets().keySet());
    }

    private void syncToUI() {
        tfTargets.setText(config.targets);
        tfExclude.setText(config.excludeTargets);
        cbIpv6.setSelected(config.useIpv6);
        tfPorts.setText(config.ports);
        cbTopPorts.setSelected(config.topPorts);
        spTopPorts.getValueFactory().setValue(config.topPortsCount);
        cbFast.setSelected(config.fastScan);
        cbAllPorts.setSelected(config.allPorts);
        tfPortRatio.setText(config.portRatio);
        cbNoPing.setSelected(config.noPing);
        cbPingOnly.setSelected(config.pingOnly);
        cbSkipHost.setSelected(config.skipHostDiscovery);
        cbServiceVersion.setSelected(config.serviceVersion);
        cbOsDetection.setSelected(config.osDetection);
        cbScriptDefault.setSelected(config.scriptDefault);
        tfScript.setText("");
        // 恢复内置脚本选择列表
        selectedBuiltinScripts.clear();
        if (config.scriptArgs != null && !config.scriptArgs.isBlank()) {
            for (String s : config.scriptArgs.split(",")) {
                if (!s.isBlank()) selectedBuiltinScripts.add(s.trim());
            }
        }
        // 恢复本地脚本列表
        selectedLocalScripts.clear();
        if (config.localScripts != null && !config.localScripts.isBlank()) {
            for (String s : config.localScripts.split(",")) {
                if (!s.isBlank()) selectedLocalScripts.add(s.trim());
            }
        }
        updateScriptSummary();
        cbVerbose.setSelected(config.verbose);
        cbOutputAll.setSelected(config.outputAll);
        tfOutputN.setText(config.outputNormal);
        tfOutputX.setText(config.outputXml);
        tfOutputG.setText(config.outputGrep);
        tfSpoofIp.setText(config.spoofIp);
        tfDecoys.setText(config.decoys);
        tfSourcePort.setText(config.sourcePort);
        tfProxy.setText(config.proxy);
        tfDnsServers.setText(config.dnsServers);
        cbRandomize.setSelected(config.randomizeHosts);
        cbFragment.setSelected(config.fragment);

        // 扫描类型与时序
        for (String s : cmbScanType.getItems()) {
            if (s.startsWith(config.scanType + " ")) { cmbScanType.setValue(s); break; }
        }
        for (String s : cmbTiming.getItems()) {
            if (s.startsWith(config.timing + " ")) { cmbTiming.setValue(s); break; }
        }
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
