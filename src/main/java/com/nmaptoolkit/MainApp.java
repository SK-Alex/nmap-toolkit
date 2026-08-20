package com.nmaptoolkit;

import com.nmaptoolkit.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Nmap Toolkit 主应用
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainWindow window = new MainWindow();
        Scene scene = new Scene(window.getRoot(), 1200, 780);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        primaryStage.setTitle("Nmap Toolkit - 图形化管理工具套件");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
