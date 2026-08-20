package com.nmaptoolkit.model;

/**
 * Nmap NSE 脚本条目
 */
public class NseScript {
    public String name;         // 脚本名，如 http-title
    public String category;     // 分类，如 discovery / vuln
    public String description;  // 功能描述

    public NseScript(String name, String category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    @Override
    public String toString() {
        return name;
    }
}
