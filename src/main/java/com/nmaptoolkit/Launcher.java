package com.nmaptoolkit;

/**
 * 启动入口（与 Main 分离，避免 JavaFX 打包时模块启动问题）
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
