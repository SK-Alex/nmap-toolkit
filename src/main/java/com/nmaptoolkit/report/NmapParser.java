package com.nmaptoolkit.report;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nmap 输出解析器：支持普通文本输出(-oN)、XML(-oX)、Grepable(-oG) 三种格式
 */
public class NmapParser {

    // 端口行：80/tcp    open   http    Apache httpd 2.4.54
    private static final Pattern PORT_LINE = Pattern.compile(
            "^(\\d+)/(tcp|udp|sctp)\\s+(open|closed|filtered|open\\|filtered|closed\\|filtered)\\s+(\\S+)(?:\\s+(.*))?$");

    // XML: <host> ... <address addr="..." addrtype="ipv4"/> ...
    private static final Pattern XML_HOST = Pattern.compile(
            "<host[^>]*>.*?<address addr=\"([^\"]+)\"[^>]*addrtype=\"ipv4\".*?</host>",
            Pattern.DOTALL);
    private static final Pattern XML_HOSTNAME = Pattern.compile(
            "<hostname name=\"([^\"]+)\"");
    private static final Pattern XML_PORT = Pattern.compile(
            "<port protocol=\"(tcp|udp|sctp)\" portid=\"(\\d+)\">.*?" +
            "<state state=\"([^\"]+)\".*?" +
            "<service name=\"([^\"]*)\"[^>]*?(?:product=\"([^\"]*)\")?[^>]*?(?:version=\"([^\"]*)\")?.*?</port>",
            Pattern.DOTALL);

    /**
     * 自动识别格式并解析
     */
    public NmapReport parse(String content) {
        if (content == null || content.isBlank()) return new NmapReport();
        String c = content.trim();
        if (c.startsWith("<?xml") || c.contains("<nmaprun")) {
            return parseXml(c);
        } else if (c.contains("Host:") && c.contains("Ports:")) {
            return parseGrepable(c);
        } else {
            return parseNormal(c);
        }
    }

    private NmapReport parseNormal(String content) {
        NmapReport report = new NmapReport();
        NmapReport.Host current = null;
        String[] lines = content.split("\\r?\\n");

        for (String line : lines) {
            String l = line.trim();
            if (l.startsWith("Nmap scan report for")) {
                if (current != null) report.hosts.add(current);
                current = new NmapReport.Host();
                String target = l.substring("Nmap scan report for".length()).trim();
                if (target.startsWith("(") && target.endsWith(")")) {
                    // 形如 xxx (1.2.3.4)
                    int idx = target.lastIndexOf('(');
                    current.hostname = target.substring(0, idx).trim();
                    current.address = target.substring(idx + 1, target.length() - 1).trim();
                } else if (target.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    current.address = target;
                } else {
                    current.hostname = target;
                }
            } else if (l.startsWith("Host is ")) {
                if (current != null) current.state = "up";
            } else if (l.contains("OS details:") || l.contains("Running:")) {
                if (current != null) current.os = l;
            } else {
                Matcher m = PORT_LINE.matcher(l);
                if (m.matches() && current != null) {
                    NmapReport.Port p = new NmapReport.Port();
                    p.port = m.group(1);
                    p.protocol = m.group(2);
                    p.state = m.group(3);
                    p.service = m.group(4) == null ? "" : m.group(4);
                    p.version = m.group(5) == null ? "" : m.group(5).trim();
                    if (p.state.contains("open")) {
                        assignRisk(p);
                    }
                    current.ports.add(p);
                }
            }
        }
        if (current != null) report.hosts.add(current);
        return report;
    }

    private NmapReport parseGrepable(String content) {
        NmapReport report = new NmapReport();
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            if (!line.startsWith("Host:")) continue;
            NmapReport.Host h = new NmapReport.Host();
            // Host: 1.2.3.4 (name) Ports: 22/open/tcp//ssh///
            Matcher hm = Pattern.compile("Host:\\s+(\\S+)").matcher(line);
            if (hm.find()) h.address = hm.group(1);
            Matcher nm = Pattern.compile("\\(([^)]+)\\)").matcher(line);
            if (nm.find()) h.hostname = nm.group(1);
            h.state = "up";

            Matcher pm = Pattern.compile("(\\d+)/(open)/(tcp|udp|sctp)//([^/]*)/([^/]*)/").matcher(line);
            while (pm.find()) {
                NmapReport.Port p = new NmapReport.Port();
                p.port = pm.group(1);
                p.state = pm.group(2);
                p.protocol = pm.group(3);
                p.service = pm.group(4);
                p.version = pm.group(5);
                assignRisk(p);
                h.ports.add(p);
            }
            report.hosts.add(h);
        }
        return report;
    }

    private NmapReport parseXml(String content) {
        NmapReport report = new NmapReport();
        // 提取每个 host
        Matcher hostMatcher = XML_HOST.matcher(content);
        while (hostMatcher.find()) {
            NmapReport.Host h = new NmapReport.Host();
            h.address = hostMatcher.group(1);
            String hostBlock = hostMatcher.group(0);
            h.state = "up";

            Matcher nm = XML_HOSTNAME.matcher(hostBlock);
            if (nm.find()) h.hostname = nm.group(1);

            Matcher pm = XML_PORT.matcher(hostBlock);
            while (pm.find()) {
                NmapReport.Port p = new NmapReport.Port();
                p.protocol = pm.group(1);
                p.port = pm.group(2);
                p.state = pm.group(3);
                p.service = pm.group(4) == null ? "" : pm.group(4);
                String product = pm.group(5) == null ? "" : pm.group(5);
                String version = pm.group(6) == null ? "" : pm.group(6);
                p.version = (product + " " + version).trim();
                if (p.state.equals("open")) assignRisk(p);
                h.ports.add(p);
            }
            report.hosts.add(h);
        }

        // 提取扫描摘要
        Matcher sm = Pattern.compile("<finished[^>]*summary=\"([^\"]*)\"").matcher(content);
        if (sm.find()) report.scanSummary = sm.group(1);
        return report;
    }

    /**
     * 根据端口和服务自动风险定级
     */
    private void assignRisk(NmapReport.Port p) {
        String s = (p.service + " " + p.version).toLowerCase();
        int port = safeInt(p.port);

        // 严重：高危端口 + 已知危险服务
        if (port == 445 || port == 135 || port == 3389 || port == 1433 || port == 6379 || port == 2375) {
            p.riskLevel = "critical";
            p.riskDesc = "高危端口开放，存在被攻击风险";
        } else if (s.contains("ms17-010") || s.contains("rce") || s.contains("deserial")
                || s.contains("struts") || s.contains("weblogic") || s.contains("log4j")) {
            p.riskLevel = "critical";
            p.riskDesc = "存在已知高危漏洞特征";
        }
        // 高危：弱口令/未授权相关服务
        else if (port == 21 || port == 23 || port == 3306 || port == 5432 || port == 5900
                || port == 11211 || port == 9200 || port == 27017 || s.contains("telnet")
                || s.contains("ftp") || s.contains("mysql") || s.contains("redis")
                || s.contains("mongodb") || s.contains("vnc")) {
            p.riskLevel = "high";
            p.riskDesc = "存在弱口令/未授权访问风险";
        }
        // 中危：明文传输或版本泄露
        else if (port == 80 || port == 8080 || port == 8000 || port == 8888
                || s.contains("http") || port == 25 || port == 110 || port == 143) {
            p.riskLevel = "medium";
            p.riskDesc = "存在明文传输/版本信息泄露风险";
        }
        // 信息
        else {
            p.riskLevel = "info";
            p.riskDesc = "普通开放端口";
        }
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}
