package com.nmaptoolkit.report;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析后的 Nmap 报告数据模型
 */
public class NmapReport {

    public static class Host {
        public String address = "";
        public String hostname = "";
        public String state = "";
        public String os = "";
        public List<Port> ports = new ArrayList<>();
    }

    public static class Port {
        public String port = "";
        public String protocol = "";
        public String state = "";
        public String service = "";
        public String version = "";
        public String riskLevel = "info"; // critical/high/medium/info
        public String riskDesc = "";
    }

    public String scanSummary = "";
    public List<Host> hosts = new ArrayList<>();

    /**
     * 统计各类风险端口数量
     */
    public int countRisk(String level) {
        int c = 0;
        for (Host h : hosts) {
            for (Port p : h.ports) {
                if (p.riskLevel.equals(level)) c++;
            }
        }
        return c;
    }

    public int totalPorts() {
        int c = 0;
        for (Host h : hosts) c += h.ports.size();
        return c;
    }
}
