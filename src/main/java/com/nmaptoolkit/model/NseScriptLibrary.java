package com.nmaptoolkit.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nmap 内置 NSE 脚本库：按分类组织，含功能描述
 */
public class NseScriptLibrary {

    public static final Map<String, String> CATEGORY_DESC = new LinkedHashMap<>();
    static {
        CATEGORY_DESC.put("auth", "身份认证：检测认证绕过、默认凭据、认证机制");
        CATEGORY_DESC.put("broadcast", "广播发现：通过局域网广播协议探测存活主机");
        CATEGORY_DESC.put("brute", "暴力破解：对目标服务进行密码猜解");
        CATEGORY_DESC.put("default", "默认脚本集：快速、可靠、低侵入性（-sC 即调用）");
        CATEGORY_DESC.put("discovery", "主动发现：枚举服务、共享资源、目录等");
        CATEGORY_DESC.put("dos", "拒绝服务：可能导致目标崩溃，需谨慎使用");
        CATEGORY_DESC.put("exploit", "漏洞利用：主动利用已知漏洞获取访问权限");
        CATEGORY_DESC.put("external", "外部依赖：需联网调用第三方 API 服务");
        CATEGORY_DESC.put("fuzzer", "模糊测试：发送异常数据以发现潜在漏洞");
        CATEGORY_DESC.put("info", "信息收集：获取系统/协议详情（较温和）");
        CATEGORY_DESC.put("intrusive", "侵入性：可能被记录日志或触发告警");
        CATEGORY_DESC.put("malware", "恶意软件检测：检测僵尸网络/蠕虫感染");
        CATEGORY_DESC.put("safe", "安全等级高：不会对目标造成负面影响");
        CATEGORY_DESC.put("version", "版本检测：配合 -sV 进一步识别服务版本");
        CATEGORY_DESC.put("vuln", "漏洞检测：检查已知安全漏洞（CVE 等）");
    }

    // 分类 -> 脚本列表
    public static final Map<String, List<NseScript>> SCRIPTS = new LinkedHashMap<>();

    static {
        // ---- default ----
        put("default", "banner", "获取服务 Banner 信息");
        put("default", "http-title", "获取网页标题");
        put("default", "http-headers", "获取 HTTP 响应头");
        put("default", "http-methods", "枚举允许的 HTTP 方法");
        put("default", "http-robots.txt", "解析 robots.txt 文件");
        put("default", "http-favicon", "获取网站 favicon 图标");
        put("default", "http-generator", "识别网页生成器/框架");
        put("default", "http-auth", "获取 Web 服务的认证方式");
        put("default", "ssh-hostkey", "获取 SSH 主机密钥指纹");
        put("default", "ssh-auth-methods", "获取 SSH 支持的认证方法");
        put("default", "ssl-cert", "获取 SSL/TLS 证书信息");
        put("default", "ssl-enum-ciphers", "枚举支持的 SSL/TLS 密码套件");
        put("default", "smtp-commands", "枚举 SMTP 支持的命令");
        put("default", "snmp-info", "获取 SNMP 基本信息");
        put("default", "nbstat", "NetBIOS 名称服务信息");
        put("default", "smb-os-discovery", "通过 SMB 识别操作系统");
        put("default", "smb-security-mode", "获取 SMB 安全模式");
        put("default", "smb2-security-mode", "获取 SMB2 安全模式");
        put("default", "rpcinfo", "枚举 RPC 服务");
        put("default", "nfs-showmount", "枚举 NFS 共享");
        put("default", "nfs-ls", "列出 NFS 共享目录");
        put("default", "mysql-info", "获取 MySQL 服务器信息");
        put("default", "ms-sql-info", "获取 MS SQL 服务器信息");
        put("default", "ldap-rootdse", "获取 LDAP 根目录信息");
        put("default", "ike-version", "识别 IKE 服务版本");
        put("default", "ipidseq", "检测 IP ID 序列生成方式");

        // ---- auth ----
        put("auth", "ajp-auth", "获取 AJP 服务的认证方式和 realm");
        put("auth", "auth-owners", "通过 identd 查询端口所有者");
        put("auth", "auth-spoof", "检测 identd 是否伪造回复");
        put("auth", "http-auth", "获取 Web 服务认证方式");
        put("auth", "http-auth-finder", "寻找需要认证的页面");
        put("auth", "http-default-accounts", "检测 Web 应用默认凭据");
        put("auth", "http-method-tamper", "HTTP 动词篡改绕过保护");
        put("auth", "ipmi-cipher-zero", "检测 IPMI Cipher Zero 认证绕过");
        put("auth", "realvnc-auth-bypass", "检测 RealVNC 认证绕过");
        put("auth", "socks-auth-info", "检测 SOCKS 代理认证机制");
        put("auth", "ssh-auth-methods", "返回 SSH 支持的认证方法");
        put("auth", "ssh-publickey-acceptance", "检测 SSH 公钥认证是否接受");
        put("auth", "x11-access", "检查是否允许连接 X 服务器");

        // ---- brute ----
        put("brute", "ftp-brute", "FTP 服务器密码审计");
        put("brute", "ssh-brute", "SSH 服务器密码暴力破解");
        put("brute", "telnet-brute", "Telnet 服务器密码审计");
        put("brute", "http-brute", "HTTP Basic/Digest/NTLM 认证审计");
        put("brute", "http-form-brute", "HTTP 表单认证密码审计");
        put("brute", "http-wordpress-brute", "WordPress 密码审计");
        put("brute", "http-joomla-brute", "Joomla 密码审计");
        put("brute", "mysql-brute", "MySQL 密码猜测");
        put("brute", "ms-sql-brute", "MS SQL Server 密码猜测");
        put("brute", "pgsql-brute", "PostgreSQL 密码猜测");
        put("brute", "oracle-brute", "Oracle 服务器密码审计");
        put("brute", "oracle-enum-users", "枚举 Oracle 有效用户名");
        put("brute", "oracle-sid-brute", "猜测 Oracle SID");
        put("brute", "redis-brute", "Redis 密码审计");
        put("brute", "mongodb-brute", "MongoDB 密码审计");
        put("brute", "smb-brute", "SMB 用户名/密码组合猜测");
        put("brute", "snmp-brute", "SNMP community string 暴力破解");
        put("brute", "smtp-brute", "SMTP 服务器密码审计");
        put("brute", "pop3-brute", "POP3 账户密码猜测");
        put("brute", "imap-brute", "IMAP 服务器密码审计");
        put("brute", "ldap-brute", "LDAP 认证暴力破解");
        put("brute", "vnc-brute", "VNC 服务器密码审计");
        put("brute", "sip-brute", "SIP 账户密码审计");
        put("brute", "xmpp-brute", "XMPP 服务器密码审计");
        put("brute", "irc-brute", "IRC 服务器密码审计");
        put("brute", "rsync-brute", "rsync 协议密码审计");
        put("brute", "cvs-brute", "CVS pserver 认证密码审计");
        put("brute", "svn-brute", "Subversion 服务器密码审计");
        put("brute", "pcanywhere-brute", "pcAnywhere 远程访问密码审计");
        put("brute", "citrix-brute-xml", "Citrix XML 服务凭据猜测");

        // ---- discovery ----
        put("discovery", "dns-brute", "DNS 子域名暴力枚举");
        put("discovery", "dns-srv-enum", "枚举 DNS SRV 记录");
        put("discovery", "dns-zone-transfer", "DNS 区域传送");
        put("discovery", "dns-service-discovery", "DNS-SD 服务发现");
        put("discovery", "snmp-interfaces", "通过 SNMP 枚举网络接口");
        put("discovery", "snmp-netstat", "通过 SNMP 获取 netstat 输出");
        put("discovery", "llmnr-resolve", "LLMNR 主机名解析");
        put("discovery", "lltd-discovery", "LLTD 协议发现局域网主机");
        put("discovery", "modbus-discover", "枚举 Modbus slave ID 收集设备信息");
        put("discovery", "dhcp-discover", "向 DHCP 服务器获取配置参数");
        put("discovery", "targets-traceroute", "将 traceroute 跳点加入扫描队列");
        put("discovery", "targets-sniffer", "本地网络嗅探发现地址");
        put("discovery", "ipv6-node-info", "IPv6 节点信息查询");
        put("discovery", "ipv6-multicast-mld-list", "IPv6 多播地址发现");

        // ---- broadcast ----
        put("broadcast", "broadcast-ping", "广播 ping 发现主机");
        put("broadcast", "broadcast-dhcp-discover", "通过 DHCP 广播发现网络信息");
        put("broadcast", "broadcast-dhcp6-discover", "DHCPv6 发现");
        put("broadcast", "broadcast-dns-service-discovery", "DNS-SD 广播服务发现");
        put("broadcast", "broadcast-ms-sql-discover", "发现 Microsoft SQL 服务器");
        put("broadcast", "broadcast-netbios-master-browser", "发现主浏览器和域");
        put("broadcast", "broadcast-upnp-info", "UPnP 信息发现");
        put("broadcast", "broadcast-wpad-discover", "WPAD 代理自动发现");
        put("broadcast", "broadcast-wsdd-discover", "WS-Discovery 设备发现");
        put("broadcast", "broadcast-jenkins-discover", "发现 Jenkins 服务器");
        put("broadcast", "broadcast-pc-anywhere", "发现 PC-Anywhere 主机");
        put("broadcast", "broadcast-sonicwall-discover", "发现 SonicWall 防火墙");

        // ---- vuln ----
        put("vuln", "smb-vuln-ms17-010", "检测 MS17-010 EternalBlue 漏洞");
        put("vuln", "smb-vuln-ms08-067", "检测 MS08-067 远程代码执行漏洞");
        put("vuln", "smb-vuln-ms06-025", "检测 Ras RPC MS06-025 漏洞");
        put("vuln", "smb-vuln-ms07-029", "检测 DNS Server RPC MS07-029 漏洞");
        put("vuln", "smb-vuln-ms10-054", "检测 MS10-054 SMB 内存破坏漏洞");
        put("vuln", "smb-vuln-ms10-061", "检测 MS10-061 打印模拟漏洞");
        put("vuln", "smb-vuln-conficker", "检测 Conficker 蠕虫感染");
        put("vuln", "smb-vuln-cve-2017-7494", "检测 Samba 任意共享库加载漏洞");
        put("vuln", "smb-double-pulsar-backdoor", "检测 Double Pulsar SMB 后门");
        put("vuln", "smb2-vuln-uptime", "检测 SMB2 服务器 uptime 漏洞");
        put("vuln", "ssl-heartbleed", "检测 OpenSSL Heartbleed 漏洞 CVE-2014-0160");
        put("vuln", "ssl-poodle", "检测 SSLv3 POODLE 漏洞");
        put("vuln", "ssl-ccs-injection", "检测 CCS Injection 漏洞 CVE-2014-0224");
        put("vuln", "sslv2-drown", "检测 SSLv2 及 DROWN 漏洞");
        put("vuln", "tls-ticketbleed", "检测 F5 Ticketbleed 漏洞 CVE-2016-9244");
        put("vuln", "http-shellshock", "利用 Shellshock 漏洞 CVE-2014-6271");
        put("vuln", "http-sql-injection", "爬取并寻找 SQL 注入漏洞");
        put("vuln", "http-stored-xss", "检测潜在存储型 XSS 漏洞");
        put("vuln", "http-passwd", "目录遍历获取 /etc/passwd");
        put("vuln", "http-phpmyadmin-dir-traversal", "phpMyAdmin 目录遍历漏洞");
        put("vuln", "http-vuln-cve2014-3704", "检测 Drupal Drupageddon 漏洞");
        put("vuln", "http-vuln-cve2017-5638", "检测 Apache Struts 远程代码执行");
        put("vuln", "http-vuln-cve2015-1635", "检测 MS15-034 远程代码执行");
        put("vuln", "http-vuln-cve2012-1823", "检测 PHP-CGI 远程代码执行");
        put("vuln", "http-vuln-cve2013-0156", "检测 Rails 对象注入漏洞");
        put("vuln", "ftp-proftpd-backdoor", "检测 ProFTPD 1.3.3c 后门");
        put("vuln", "ftp-vsftpd-backdoor", "检测 vsFTPd 2.3.4 后门");
        put("vuln", "ftp-libopie", "检测 FTPd OPIE 栈溢出漏洞");
        put("vuln", "rdp-vuln-ms12-020", "检测 MS12-020 RDP 漏洞");
        put("vuln", "realvnc-auth-bypass", "检测 RealVNC 认证绕过");
        put("vuln", "rmi-vuln-classloader", "检测 Java RMI 类加载 RCE");
        put("vuln", "rsa-vuln-roca", "检测 RSA 密钥 ROCA 攻击");
        put("vuln", "samba-vuln-cve-2012-1182", "检测 Samba 堆溢出漏洞");
        put("vuln", "smtp-vuln-cve2010-4344", "检测 Exim 堆溢出漏洞");
        put("vuln", "mysql-vuln-cve2012-2122", "检测 MySQL 认证绕过漏洞");
        put("vuln", "vulners", "根据 CPE 输出已知漏洞和 CVSS 评分");
        put("vuln", "ipmi-cipher-zero", "检测 IPMI Cipher Zero 认证绕过");

        // ---- exploit ----
        put("exploit", "clamav-exec", "利用 ClamAV 未授权命令执行");
        put("exploit", "distcc-cve2004-2687", "利用 distcc 远程代码执行");
        put("exploit", "http-fileupload-exploiter", "利用不安全文件上传表单");
        put("exploit", "http-rfi-spider", "爬取远程文件包含漏洞");
        put("exploit", "jdwp-exec", "Java 远程调试端口代码执行");
        put("exploit", "qconn-exec", "QNX QCONN 未授权命令执行");
        put("exploit", "smb-psexec", "类似 psexec 的远程进程执行");
        put("exploit", "smb-webexec-exploit", "WebExec 漏洞利用");
        put("exploit", "http-awstatstotals-exec", "Awstats Totals 远程代码执行");
        put("exploit", "http-axis2-dir-traversal", "Axis2 目录遍历漏洞");

        // ---- dos ----
        put("dos", "http-slowloris", "Slowloris 拒绝服务测试");
        put("dos", "http-slowloris-check", "Slowloris 漏洞检测（不真正 DoS）");
        put("dos", "smb-flood", "耗尽 SMB 连接限制");
        put("dos", "smb-vuln-cve2009-3103", "Windows SMB 拒绝服务检测");
        put("dos", "smb-vuln-regsvc-dos", "Windows 2000 regsvc 崩溃检测");
        put("dos", "broadcast-avahi-dos", "Avahi NULL UDP 包拒绝服务检测");
        put("dos", "ipv6-ra-flood", "IPv6 路由通告洪泛");

        // ---- malware ----
        put("malware", "dns-zeustracker", "检测 Zeus 僵尸网络");
        put("malware", "http-google-malware", "检测 Google 恶意软件黑名单");
        put("malware", "http-malware-host", "检测已知服务器入侵痕迹");
        put("malware", "http-virustotal", "通过 VirusTotal 检测恶意文件");
        put("malware", "p2p-conficker", "检测 Conficker 感染");
        put("malware", "smb-vuln-conficker", "检测 Conficker 蠕虫");
        put("malware", "stuxnet-detect", "检测 Stuxnet 蠕虫");

        // ---- info ----
        put("info", "http-enum", "枚举 Web 目录与文件");
        put("info", "http-git", "检测 .git 目录泄露");
        put("info", "http-sitemap-generator", "生成站点地图");
        put("info", "http-security-headers", "检测安全响应头");
        put("info", "http-cors", "检测 CORS 配置");
        put("info", "http-errors", "检测错误页面信息泄露");
        put("info", "mysql-databases", "枚举 MySQL 数据库");
        put("info", "ms-sql-config", "获取 MS SQL 配置");
        put("info", "whois-domain", "查询域名 whois 信息");
        put("info", "whois-ip", "查询 IP whois 信息");
        put("info", "asn-query", "ASN 归属查询");
        put("info", "dns-nsid", "获取 DNS 服务器 NSID");
        put("info", "dns-recursion", "检测 DNS 递归查询");

        // ---- version ----
        put("version", "docker-version", "识别 Docker 版本");
        put("version", "iax2-version", "识别 IAX2 协议版本");
        put("version", "ike-version", "识别 IKE 服务版本");
        put("version", "mikrotik-routeros-version", "识别 MikroTik RouterOS 版本");
        put("version", "ndmp-version", "识别 NDMP 服务版本");
        put("version", "oracle-tns-version", "识别 Oracle TNS 版本");
        put("version", "pptp-version", "识别 PPTP 服务版本");
        put("version", "stun-version", "识别 STUN 服务版本");
        put("version", "teamspeak2-version", "识别 TeamSpeak2 版本");
        put("version", "tftp-version", "识别 TFTP 服务版本");
        put("version", "vmware-version", "识别 VMware 版本");
        put("version", "weblogic-t3-info", "获取 WebLogic T3 协议信息");
        put("version", "wdb-version", "识别 wdb 服务版本");

        // ---- safe / fuzzer / external / intrusive (精选) ----
        put("safe", "http-date", "获取 HTTP 服务器时间");
        put("safe", "http-title", "获取网页标题");
        put("safe", "ssh-hostkey", "获取 SSH 主机密钥");
        put("fuzzer", "http-form-fuzzer", "HTTP 表单模糊测试");
        put("fuzzer", "dns-fuzz", "DNS 模糊测试");
        put("fuzzer", "http-fuzz", "HTTP 模糊测试");
        put("external", "http-virustotal", "通过 VirusTotal 检测恶意文件");
        put("external", "ip-geolocation-geoplugin", "通过 GeoPlugin 获取 IP 地理位置");
        put("external", "ip-geolocation-ipinfodb", "通过 IPInfoDB 获取 IP 地理位置");
        put("external", "whois-domain", "查询域名 whois 信息");
        put("intrusive", "http-enum", "枚举 Web 目录（较侵入）");
        put("intrusive", "dns-brute", "DNS 子域名暴力枚举");
        put("intrusive", "samba-enum-shares", "枚举 Samba 共享");
    }

    private static void put(String category, String name, String desc) {
        SCRIPTS.computeIfAbsent(category, k -> new ArrayList<>())
                .add(new NseScript(name, category, desc));
    }

    /**
     * 按分类查找脚本
     */
    public static List<NseScript> byCategory(String category) {
        return SCRIPTS.getOrDefault(category, new ArrayList<>());
    }

    /**
     * 按名称/描述模糊搜索
     */
    public static List<NseScript> search(String keyword) {
        List<NseScript> result = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            SCRIPTS.values().forEach(result::addAll);
            return result;
        }
        String kw = keyword.toLowerCase();
        for (List<NseScript> list : SCRIPTS.values()) {
            for (NseScript s : list) {
                if (s.name.toLowerCase().contains(kw) || s.description.toLowerCase().contains(kw)) {
                    result.add(s);
                }
            }
        }
        return result;
    }
}
