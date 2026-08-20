package com.nmaptoolkit.ui;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/**
 * 主窗口：命令生成器 + 报告生成器 两个标签页
 */
public class MainWindow {

    private final BorderPane root = new BorderPane();
    private final TabPane tabPane = new TabPane();
    private final CommandGeneratorPane commandPane;
    private final ReportGeneratorPane reportPane;

    public MainWindow() {
        commandPane = new CommandGeneratorPane();
        reportPane = new ReportGeneratorPane();

        Tab commandTab = new Tab("命令生成器");
        commandTab.setClosable(false);
        commandTab.setContent(commandPane.getRoot());

        Tab reportTab = new Tab("报告生成器");
        reportTab.setClosable(false);
        reportTab.setContent(reportPane.getRoot());

        tabPane.getTabs().addAll(commandTab, reportTab);
        root.setCenter(tabPane);

        // 桥接：命令生成器执行完成后，可将结果发送到报告生成器
        commandPane.setOnResultReady(result -> reportPane.setInput(result));
        commandPane.setOnSendToReport(this::switchToReportTab);
    }

    /**
     * 切换到报告生成器标签页
     */
    private void switchToReportTab() {
        tabPane.getSelectionModel().select(1);
    }

    public BorderPane getRoot() {
        return root;
    }
}
