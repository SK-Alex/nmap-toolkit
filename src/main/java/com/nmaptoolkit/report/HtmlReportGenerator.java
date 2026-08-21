package com.nmaptoolkit.report;

/**
 * 将 NmapReport 渲染为 HTML 报告
 */
public class HtmlReportGenerator {

    private static final String CRITICAL = "#e74c3c";
    private static final String HIGH = "#e67e22";
    private static final String MEDIUM = "#f1c40f";
    private static final String INFO = "#3498db";

    /** 大模型分析结果（Markdown），可为空 */
    private String aiAnalysis = "";

    public void setAiAnalysis(String aiAnalysis) {
        this.aiAnalysis = aiAnalysis == null ? "" : aiAnalysis;
    }

    public String generate(NmapReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<title>Nmap 安全扫描报告</title>\n");
        sb.append("<style>\n");
        sb.append(css());
        sb.append("</style>\n</head>\n<body>\n");

        // 标题
        sb.append("<div class=\"header\">\n");
        sb.append("<h1>Nmap 安全扫描报告</h1>\n");
        sb.append("<p class=\"sub\">生成时间: ").append(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");
        if (report.scanSummary != null && !report.scanSummary.isBlank()) {
            sb.append("<p class=\"summary\">扫描摘要: ").append(escape(report.scanSummary)).append("</p>\n");
        }
        sb.append("</div>\n");

        // 风险概览
        int critical = report.countRisk("critical");
        int high = report.countRisk("high");
        int medium = report.countRisk("medium");
        int info = report.countRisk("info");
        int total = report.totalPorts();

        sb.append("<div class=\"overview\">\n");
        sb.append(card("总开放端口", total, "#34495e"));
        sb.append(card("严重", critical, CRITICAL));
        sb.append(card("高危", high, HIGH));
        sb.append(card("中危", medium, MEDIUM));
        sb.append(card("信息", info, INFO));
        sb.append("</div>\n");

        // 大模型分析结果
        if (!aiAnalysis.isBlank()) {
            sb.append("<h2>AI 智能分析</h2>\n");
            sb.append("<div class=\"ai-analysis\">\n");
            sb.append(markdownToHtml(aiAnalysis));
            sb.append("</div>\n");
        }

        // 主机详情
        sb.append("<h2>主机详情</h2>\n");
        for (NmapReport.Host host : report.hosts) {
            sb.append("<div class=\"host\">\n");
            sb.append("<div class=\"host-title\">");
            sb.append("<strong>").append(escape(host.address.isEmpty() ? host.hostname : host.address)).append("</strong>");
            if (!host.hostname.isEmpty() && !host.hostname.equals(host.address)) {
                sb.append(" <span class=\"muted\">(").append(escape(host.hostname)).append(")</span>");
            }
            if (!host.os.isEmpty()) {
                sb.append(" <span class=\"muted\">OS: ").append(escape(host.os)).append("</span>");
            }
            sb.append("</div>\n");

            if (host.ports.isEmpty()) {
                sb.append("<p class=\"muted\">无开放端口记录</p>\n");
            } else {
                sb.append("<table>\n<thead><tr><th>端口</th><th>协议</th><th>状态</th><th>服务</th><th>版本</th><th>风险等级</th><th>风险说明</th></tr></thead>\n<tbody>\n");
                for (NmapReport.Port p : host.ports) {
                    sb.append("<tr>");
                    sb.append("<td>").append(p.port).append("</td>");
                    sb.append("<td>").append(p.protocol).append("</td>");
                    sb.append("<td>").append(p.state).append("</td>");
                    sb.append("<td>").append(escape(p.service)).append("</td>");
                    sb.append("<td>").append(escape(p.version)).append("</td>");
                    sb.append("<td><span class=\"badge ").append(p.riskLevel).append("\">")
                      .append(label(p.riskLevel)).append("</span></td>");
                    sb.append("<td>").append(escape(p.riskDesc)).append("</td>");
                    sb.append("</tr>\n");
                }
                sb.append("</tbody></table>\n");
            }
            sb.append("</div>\n");
        }

        sb.append("</body>\n</html>");
        return sb.toString();
    }

    private String card(String label, int count, String color) {
        return "<div class=\"card\" style=\"border-top:4px solid " + color + ";\">" +
                "<div class=\"card-num\" style=\"color:" + color + ";\">" + count + "</div>" +
                "<div class=\"card-label\">" + label + "</div></div>\n";
    }

    private String label(String level) {
        return switch (level) {
            case "critical" -> "严重";
            case "high" -> "高危";
            case "medium" -> "中危";
            default -> "信息";
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * 轻量 Markdown 转 HTML（支持标题、粗体、列表、代码块）
     */
    private String markdownToHtml(String md) {
        StringBuilder sb = new StringBuilder();
        boolean inCode = false;
        for (String line : md.split("\\n", -1)) {
            String l = line;
            // 代码块
            if (l.trim().startsWith("```")) {
                if (inCode) {
                    sb.append("</pre>\n");
                    inCode = false;
                } else {
                    sb.append("<pre>");
                    inCode = true;
                }
                continue;
            }
            if (inCode) {
                sb.append(escape(l)).append("\n");
                continue;
            }
            // 标题
            if (l.startsWith("### ")) {
                sb.append("<h4>").append(inlineMd(l.substring(4))).append("</h4>\n");
            } else if (l.startsWith("## ")) {
                sb.append("<h3>").append(inlineMd(l.substring(3))).append("</h3>\n");
            } else if (l.startsWith("# ")) {
                sb.append("<h3>").append(inlineMd(l.substring(2))).append("</h3>\n");
            } else if (l.trim().matches("^[-*]\\s+.*")) {
                // 无序列表
                sb.append("<li>").append(inlineMd(l.trim().replaceFirst("^[-*]\\s+", ""))).append("</li>\n");
            } else if (l.trim().matches("^\\d+[\\.、]\\s+.*")) {
                // 有序列表
                sb.append("<li>").append(inlineMd(l.trim().replaceFirst("^\\d+[\\.、]\\s+", ""))).append("</li>\n");
            } else if (l.trim().isEmpty()) {
                sb.append("<br>\n");
            } else {
                sb.append("<p>").append(inlineMd(l)).append("</p>\n");
            }
        }
        if (inCode) sb.append("</pre>\n");
        return sb.toString();
    }

    /** 处理行内的粗体 **text** 和反引号 `code` */
    private String inlineMd(String s) {
        String esc = escape(s);
        esc = esc.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        esc = esc.replaceAll("`(.+?)`", "<code>$1</code>");
        return esc;
    }

    private String css() {
        return """
                body { font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif;
                       margin: 0; background: #f5f6fa; color: #2c3e50; }
                .header { background: linear-gradient(135deg, #2c3e50, #34495e);
                          color: #fff; padding: 28px 40px; }
                .header h1 { margin: 0; font-size: 24px; }
                .header .sub { margin: 8px 0 0; opacity: 0.8; font-size: 13px; }
                .header .summary { margin: 6px 0 0; opacity: 0.7; font-size: 12px; }
                .overview { display: flex; gap: 16px; padding: 24px 40px; flex-wrap: wrap; }
                .card { background: #fff; border-radius: 8px; padding: 16px 28px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.06); min-width: 100px; text-align: center; }
                .card-num { font-size: 28px; font-weight: 700; }
                .card-label { font-size: 13px; color: #7f8c8d; margin-top: 4px; }
                h2 { padding: 0 40px; margin: 10px 0 14px; font-size: 18px; }
                .host { background: #fff; margin: 0 40px 20px; border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.06); overflow: hidden; }
                .host-title { background: #ecf0f1; padding: 12px 20px; font-size: 15px; }
                .muted { color: #95a5a6; font-size: 12px; font-weight: normal; }
                table { width: 100%; border-collapse: collapse; font-size: 13px; }
                th, td { text-align: left; padding: 10px 14px; border-bottom: 1px solid #eee; }
                th { background: #fafafa; color: #7f8c8d; font-weight: 600; }
                .badge { display: inline-block; padding: 2px 10px; border-radius: 10px;
                         color: #fff; font-size: 12px; }
                .badge.critical { background: #e74c3c; }
                .badge.high { background: #e67e22; }
                .badge.medium { background: #f1c40f; color: #333; }
                .badge.info { background: #3498db; }
                .ai-analysis { background: #fff; margin: 0 40px 20px; border-radius: 8px;
                        padding: 20px 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06);
                        border-left: 4px solid #9b59b6; }
                .ai-analysis h3 { margin: 14px 0 8px; font-size: 15px; color: #8e44ad; }
                .ai-analysis h4 { margin: 12px 0 6px; font-size: 14px; color: #2c3e50; }
                .ai-analysis p { margin: 6px 0; line-height: 1.6; }
                .ai-analysis li { margin: 4px 0 4px 20px; line-height: 1.6; }
                .ai-analysis pre { background: #f8f9fa; padding: 12px; border-radius: 6px;
                        overflow-x: auto; font-size: 12px; }
                .ai-analysis code { background: #f0f0f0; padding: 1px 5px; border-radius: 3px;
                        font-size: 12px; }
                """;
    }
}
