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

    /**
     * 生成用于大模型分析的纯文本摘要
     */
    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        sb.append("扫描摘要: ").append(scanSummary == null ? "无" : scanSummary).append("\n\n");
        sb.append("主机数量: ").append(hosts.size()).append("\n");
        sb.append("开放端口总数: ").append(totalPorts()).append("\n");
        sb.append("风险统计 - 严重: ").append(countRisk("critical"))
          .append(" 高危: ").append(countRisk("high"))
          .append(" 中危: ").append(countRisk("medium"))
          .append(" 信息: ").append(countRisk("info")).append("\n\n");

        for (Host h : hosts) {
            sb.append("=== 主机 ").append(h.address.isEmpty() ? h.hostname : h.address).append(" ===\n");
            if (!h.hostname.isEmpty() && !h.hostname.equals(h.address)) {
                sb.append("主机名: ").append(h.hostname).append("\n");
            }
            if (!h.os.isEmpty()) {
                sb.append("操作系统: ").append(h.os).append("\n");
            }
            for (Port p : h.ports) {
                sb.append(String.format("  端口 %s/%s 状态=%s 服务=%s 版本=%s 风险=%s\n",
                        p.port, p.protocol, p.state, p.service, p.version,
                        riskLabel(p.riskLevel)));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String riskLabel(String level) {
        return switch (level) {
            case "critical" -> "严重";
            case "high" -> "高危";
            case "medium" -> "中危";
            default -> "信息";
        };
    }
}
