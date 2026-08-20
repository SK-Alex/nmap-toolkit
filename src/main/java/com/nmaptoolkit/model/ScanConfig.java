package com.nmaptoolkit.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nmap 扫描配置模型，命令生成器的数据载体
 */
public class ScanConfig {

    // 目标设置
    public String targets = "";                 // 目标 IP / 域名 / 网段
    public String excludeTargets = "";          // 排除目标
    public boolean useIpv6 = false;             // -6

    // 端口控制
    public String ports = "";                   // 自定义端口，如 80,443,8000-9000
    public boolean topPorts = false;            // --top-ports
    public int topPortsCount = 100;
    public boolean fastScan = false;            // -F 快速扫描
    public String portRatio = "";               // --port-ratio
    public boolean allPorts = false;            // -p- 全端口

    // 主机发现
    public boolean noPing = false;              // -Pn
    public boolean pingOnly = false;            // -sn
    public boolean skipHostDiscovery = false;   // -sL

    // 扫描类型 (sS/sT/sU/sN/sF/sX/sA/sW/sM/sI)
    public String scanType = "-sS";             // 默认 SYN 半开

    // 扫描控制
    public String timing = "-T4";               // -T0 ~ -T5
    public String minRate = "";                 // --min-rate
    public String maxRate = "";                 // --max-rate
    public String parallelism = "";             // --min-parallelism
    public String hostTimeout = "";             // --host-timeout

    // 服务与系统探测
    public boolean serviceVersion = false;      // -sV
    public boolean osDetection = false;         // -O
    public boolean scriptDefault = false;       // -sC
    public String scriptArgs = "";              // --script 内置脚本名，逗号分隔
    public String localScripts = "";            // --script 本地自定义脚本路径，逗号分隔

    // 输出设置
    public boolean verbose = false;             // -v
    public String outputNormal = "";            // -oN
    public String outputXml = "";               // -oX
    public String outputGrep = "";              // -oG
    public boolean outputAll = false;           // -oA

    // 伪装/代理/网络
    public String spoofIp = "";                 // -S
    public String decoys = "";                  // -D
    public String sourcePort = "";              // --source-port
    public String proxy = "";                   // --proxies
    public String dnsServers = "";              // --dns-servers
    public boolean randomizeHosts = false;      // --randomize-hosts
    public boolean fragment = false;            // -f

    public ScanConfig() {
    }

    /**
     * 生成 Nmap 命令
     */
    public String buildCommand() {
        StringBuilder sb = new StringBuilder("nmap");

        // 扫描类型
        if (scanType != null && !scanType.isBlank()) {
            sb.append(' ').append(scanType.trim());
        }

        // 目标设置
        if (useIpv6) sb.append(" -6");
        if (excludeTargets != null && !excludeTargets.isBlank()) {
            sb.append(" --exclude ").append(excludeTargets.trim());
        }

        // 主机发现
        if (noPing) sb.append(" -Pn");
        if (pingOnly) sb.append(" -sn");
        if (skipHostDiscovery) sb.append(" -sL");

        // 端口控制
        if (allPorts) {
            sb.append(" -p-");
        } else if (ports != null && !ports.isBlank()) {
            sb.append(" -p ").append(ports.trim());
        } else if (topPorts && topPortsCount > 0) {
            sb.append(" --top-ports ").append(topPortsCount);
        } else if (fastScan) {
            sb.append(" -F");
        }
        if (portRatio != null && !portRatio.isBlank()) {
            sb.append(" --port-ratio ").append(portRatio.trim());
        }

        // 扫描控制
        if (timing != null && !timing.isBlank()) sb.append(' ').append(timing.trim());
        if (minRate != null && !minRate.isBlank()) sb.append(" --min-rate ").append(minRate.trim());
        if (maxRate != null && !maxRate.isBlank()) sb.append(" --max-rate ").append(maxRate.trim());
        if (parallelism != null && !parallelism.isBlank()) sb.append(" --min-parallelism ").append(parallelism.trim());
        if (hostTimeout != null && !hostTimeout.isBlank()) sb.append(" --host-timeout ").append(hostTimeout.trim());

        // 服务与系统探测
        if (serviceVersion) sb.append(" -sV");
        if (osDetection) sb.append(" -O");
        if (scriptDefault) sb.append(" -sC");
        // 合并内置脚本与本地自定义脚本
        String combined = joinScripts(scriptArgs, localScripts);
        if (!combined.isBlank()) sb.append(" --script ").append(combined);

        // 伪装/代理/网络
        if (spoofIp != null && !spoofIp.isBlank()) sb.append(" -S ").append(spoofIp.trim());
        if (decoys != null && !decoys.isBlank()) sb.append(" -D ").append(decoys.trim());
        if (sourcePort != null && !sourcePort.isBlank()) sb.append(" --source-port ").append(sourcePort.trim());
        if (proxy != null && !proxy.isBlank()) sb.append(" --proxies ").append(proxy.trim());
        if (dnsServers != null && !dnsServers.isBlank()) sb.append(" --dns-servers ").append(dnsServers.trim());
        if (randomizeHosts) sb.append(" --randomize-hosts");
        if (fragment) sb.append(" -f");

        // 输出设置
        if (verbose) sb.append(" -v");
        if (outputNormal != null && !outputNormal.isBlank()) sb.append(" -oN ").append(outputNormal.trim());
        if (outputXml != null && !outputXml.isBlank()) sb.append(" -oX ").append(outputXml.trim());
        if (outputGrep != null && !outputGrep.isBlank()) sb.append(" -oG ").append(outputGrep.trim());
        if (outputAll) sb.append(" -oA scan-output");

        // 目标
        sb.append(' ');
        sb.append(targets == null || targets.isBlank() ? "<target>" : targets.trim());

        return sb.toString();
    }

    /**
     * 合并内置脚本名与本地脚本路径（用逗号分隔）
     */
    private static String joinScripts(String builtin, String local) {
        StringBuilder sb = new StringBuilder();
        if (builtin != null && !builtin.isBlank()) {
            sb.append(builtin.trim());
        }
        if (local != null && !local.isBlank()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(local.trim());
        }
        return sb.toString();
    }

    /**
     * 导出配置（用于保存方案，供 JSON 序列化）
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("targets", targets);
        m.put("excludeTargets", excludeTargets);
        m.put("useIpv6", useIpv6);
        m.put("ports", ports);
        m.put("topPorts", topPorts);
        m.put("topPortsCount", topPortsCount);
        m.put("fastScan", fastScan);
        m.put("portRatio", portRatio);
        m.put("allPorts", allPorts);
        m.put("noPing", noPing);
        m.put("pingOnly", pingOnly);
        m.put("skipHostDiscovery", skipHostDiscovery);
        m.put("scanType", scanType);
        m.put("timing", timing);
        m.put("minRate", minRate);
        m.put("maxRate", maxRate);
        m.put("parallelism", parallelism);
        m.put("hostTimeout", hostTimeout);
        m.put("serviceVersion", serviceVersion);
        m.put("osDetection", osDetection);
        m.put("scriptDefault", scriptDefault);
        m.put("scriptArgs", scriptArgs);
        m.put("localScripts", localScripts);
        m.put("verbose", verbose);
        m.put("outputNormal", outputNormal);
        m.put("outputXml", outputXml);
        m.put("outputGrep", outputGrep);
        m.put("outputAll", outputAll);
        m.put("spoofIp", spoofIp);
        m.put("decoys", decoys);
        m.put("sourcePort", sourcePort);
        m.put("proxy", proxy);
        m.put("dnsServers", dnsServers);
        m.put("randomizeHosts", randomizeHosts);
        m.put("fragment", fragment);
        return m;
    }

    /**
     * 从 Map 恢复配置
     */
    @SuppressWarnings("unchecked")
    public void fromMap(Map<String, Object> m) {
        targets = str(m.get("targets"));
        excludeTargets = str(m.get("excludeTargets"));
        useIpv6 = bool(m.get("useIpv6"));
        ports = str(m.get("ports"));
        topPorts = bool(m.get("topPorts"));
        topPortsCount = num(m.get("topPortsCount"), 100);
        fastScan = bool(m.get("fastScan"));
        portRatio = str(m.get("portRatio"));
        allPorts = bool(m.get("allPorts"));
        noPing = bool(m.get("noPing"));
        pingOnly = bool(m.get("pingOnly"));
        skipHostDiscovery = bool(m.get("skipHostDiscovery"));
        scanType = str(m.get("scanType"));
        timing = str(m.get("timing"));
        minRate = str(m.get("minRate"));
        maxRate = str(m.get("maxRate"));
        parallelism = str(m.get("parallelism"));
        hostTimeout = str(m.get("hostTimeout"));
        serviceVersion = bool(m.get("serviceVersion"));
        osDetection = bool(m.get("osDetection"));
        scriptDefault = bool(m.get("scriptDefault"));
        scriptArgs = str(m.get("scriptArgs"));
        localScripts = str(m.get("localScripts"));
        verbose = bool(m.get("verbose"));
        outputNormal = str(m.get("outputNormal"));
        outputXml = str(m.get("outputXml"));
        outputGrep = str(m.get("outputGrep"));
        outputAll = bool(m.get("outputAll"));
        spoofIp = str(m.get("spoofIp"));
        decoys = str(m.get("decoys"));
        sourcePort = str(m.get("sourcePort"));
        proxy = str(m.get("proxy"));
        dnsServers = str(m.get("dnsServers"));
        randomizeHosts = bool(m.get("randomizeHosts"));
        fragment = bool(m.get("fragment"));
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static boolean bool(Object o) { return o != null && Boolean.parseBoolean(String.valueOf(o)); }
    private static int num(Object o, int def) {
        try { return o == null ? def : Integer.parseInt(String.valueOf(o)); }
        catch (NumberFormatException e) { return def; }
    }
}
