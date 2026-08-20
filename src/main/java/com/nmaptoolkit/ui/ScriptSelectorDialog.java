package com.nmaptoolkit.ui;

import com.nmaptoolkit.model.NseScript;
import com.nmaptoolkit.model.NseScriptLibrary;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 脚本选择对话框：
 * - 左：按分类列出的内置脚本（带功能描述）
 * - 右：已选脚本 + 本地自定义脚本管理
 */
public class ScriptSelectorDialog extends Dialog<ScriptSelectorDialog.Result> {

    public static class Result {
        public List<String> builtinScripts = new ArrayList<>();  // 选中的内置脚本名
        public List<String> localScripts = new ArrayList<>();    // 选中的本地脚本路径
    }

    private final Result result = new Result();
    private final ListView<NseScript> builtinList = new ListView<>();
    private final ListView<String> selectedList = new ListView<>();
    private final ListView<String> localList = new ListView<>();
    private final ComboBox<String> categoryBox = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final Label descLabel = new Label();

    public ScriptSelectorDialog(List<String> initialBuiltin, List<String> initialLocal) {
        // 预填已选
        if (initialBuiltin != null) result.builtinScripts.addAll(initialBuiltin);
        if (initialLocal != null) result.localScripts.addAll(initialLocal);

        setTitle("选择 Nmap 脚本");
        setHeaderText("内置 NSE 脚本库 + 本地自定义脚本");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        buildUI();
        refreshSelectedList();
        refreshLocalList();
    }

    private void buildUI() {
        // ---- 左侧：内置脚本浏览 ----
        VBox left = new VBox(8);
        left.setPrefWidth(430);

        // 分类下拉
        categoryBox.setItems(FXCollections.observableArrayList(NseScriptLibrary.CATEGORY_DESC.keySet()));
        categoryBox.setValue("vuln");
        categoryBox.setOnAction(e -> loadCategory());

        // 搜索框
        searchField.setPromptText("搜索脚本名/功能描述...");
        searchField.textProperty().addListener((o, a, b) -> loadCategory());

        builtinList.setPrefHeight(420);
        builtinList.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null) descLabel.setText("【" + b.category + "】" + b.description);
        });
        // 双击添加
        builtinList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && builtinList.getSelectionModel().getSelectedItem() != null) {
                addBuiltin(builtinList.getSelectionModel().getSelectedItem().name);
            }
        });

        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill:#555; -fx-font-size:12px;");
        descLabel.setMinHeight(40);

        left.getChildren().addAll(
                new Label("分类:"), categoryBox,
                new Label("搜索:"), searchField,
                new Label("内置脚本 (双击添加):"), builtinList,
                new Label("功能说明:"), descLabel);
        VBox.setVgrow(builtinList, Priority.ALWAYS);

        // ---- 右侧：已选 + 本地脚本 ----
        VBox right = new VBox(8);
        right.setPrefWidth(360);

        Label selLabel = new Label("已选内置脚本:");
        selectedList.setPrefHeight(200);
        selectedList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedList.getSelectionModel().getSelectedItem() != null) {
                removeBuiltin(selectedList.getSelectionModel().getSelectedItem());
            }
        });

        Button btnRemoveSel = new Button("移除选中脚本");
        btnRemoveSel.setOnAction(e -> removeBuiltin(selectedList.getSelectionModel().getSelectedItem()));

        Label localLabel = new Label("本地自定义脚本:");
        localList.setPrefHeight(180);
        localList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && localList.getSelectionModel().getSelectedItem() != null) {
                removeLocal(localList.getSelectionModel().getSelectedItem());
            }
        });

        Button btnAddLocalFile = new Button("添加本地脚本文件");
        btnAddLocalFile.setOnAction(e -> addLocalFiles());
        Button btnAddLocalDir = new Button("添加脚本目录");
        btnAddLocalDir.setOnAction(e -> addLocalDir());
        Button btnRemoveLocal = new Button("移除本地脚本");
        btnRemoveLocal.setOnAction(e -> removeLocal(localList.getSelectionModel().getSelectedItem()));

        HBox localBtnBox = new HBox(8, btnAddLocalFile, btnAddLocalDir, btnRemoveLocal);

        Label tip = new Label("提示: 本地脚本目录会以 --script=路径/*.nse 方式加载，\n支持 .nse 或 .lua 文件。");
        tip.setWrapText(true);
        tip.setStyle("-fx-text-fill:#888; -fx-font-size:11px;");

        right.getChildren().addAll(selLabel, selectedList, btnRemoveSel,
                new Separator(), localLabel, localList, localBtnBox, tip);
        VBox.setVgrow(selectedList, Priority.ALWAYS);
        VBox.setVgrow(localList, Priority.ALWAYS);

        HBox content = new HBox(16, left, right);
        content.setPadding(new Insets(12));
        getDialogPane().setContent(content);

        // 确定时填充结果
        setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Result r = new Result();
                r.builtinScripts.addAll(result.builtinScripts);
                r.localScripts.addAll(result.localScripts);
                return r;
            }
            return null;
        });

        loadCategory();
    }

    private void loadCategory() {
        String category = categoryBox.getValue();
        String kw = searchField.getText();
        List<NseScript> scripts;
        if (kw != null && !kw.isBlank()) {
            scripts = NseScriptLibrary.search(kw);
        } else {
            scripts = NseScriptLibrary.byCategory(category);
        }
        builtinList.getItems().setAll(scripts);
    }

    private void addBuiltin(String name) {
        if (name != null && !result.builtinScripts.contains(name)) {
            result.builtinScripts.add(name);
            refreshSelectedList();
        }
    }

    private void removeBuiltin(String name) {
        if (name != null) {
            result.builtinScripts.remove(name);
            refreshSelectedList();
        }
    }

    private void addLocalFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择本地脚本文件");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("NSE/Lua 脚本", "*.nse", "*.lua"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));
        List<File> files = chooser.showOpenMultipleDialog(getDialogPane().getScene().getWindow());
        if (files == null) return;
        for (File f : files) {
            if (!result.localScripts.contains(f.getAbsolutePath())) {
                result.localScripts.add(f.getAbsolutePath());
            }
        }
        refreshLocalList();
    }

    private void addLocalDir() {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("选择脚本目录");
        File dir = chooser.showDialog(getDialogPane().getScene().getWindow());
        if (dir == null) return;
        String path = dir.getAbsolutePath() + "/*.nse";
        if (!result.localScripts.contains(path)) {
            result.localScripts.add(path);
        }
        refreshLocalList();
    }

    private void removeLocal(String path) {
        if (path != null) {
            result.localScripts.remove(path);
            refreshLocalList();
        }
    }

    private void refreshSelectedList() {
        selectedList.getItems().setAll(result.builtinScripts);
    }

    private void refreshLocalList() {
        localList.getItems().setAll(result.localScripts);
    }
}
