# Nmap Toolkit - 图形化管理工具套件

针对 **Nmap** 开发的 Java 图形化管理工具，包含两大模块：

1. **命令生成器** - 图形化配置 Nmap 参数，自动生成正确命令
2. **报告生成器** - 解析 Nmap 输出，自动风险定级并生成 HTML/PDF 报告

## 环境要求

- JDK 21（已验证 `21.0.11 LTS`）
- Maven 3.6+
- macOS 需安装 JavaFX 21 依赖（已在 pom 中配置，Maven 自动拉取）

## 内置 Nmap（免安装，跨平台）

工具已**内置 nmap 二进制与完整数据目录**（NSE 脚本、nselib 库、OS/服务指纹库），打包在 jar 的 `nmap/` 目录内。

执行扫描时的定位优先级：
1. 系统 PATH 中已安装的 nmap（使用系统数据目录）
2. jar 内置的 nmap（按当前 OS + 架构自动选择对应二进制，首次解压到 `~/.nmap-toolkit/nmap/`，并自动注入 `--datadir`）

因此**对方拿到 jar 后无需单独安装 nmap 即可直接扫描**。

### 平台目录结构

```
nmap/
├── darwin-arm64/nmap              # macOS Apple Silicon
├── darwin-x86_64/bin/nmap + lib/  # macOS Intel（带 dylib）
├── linux-x86_64/nmap              # Linux x86_64
├── linux-arm64/nmap               # Linux ARM64
├── windows-x86_64/nmap.exe + DLL  # Windows x86_64
└── share/                         # 跨平台通用数据目录
```

### 已内置平台

- [x] darwin-arm64（macOS Apple Silicon）
- [x] darwin-x86_64（macOS Intel）
- [x] linux-x86_64
- [x] windows-x86_64（nmap.exe + DLL）
- [ ] linux-arm64（官方无预编译包）

> Windows 提示：`-sS` 等原始包扫描需额外安装 Npcap 驱动（内核驱动无法打包）。不装 Npcap 时 `-sT`、版本探测、NSE 脚本仍可用。详见 `src/main/resources/nmap/PLATFORMS.md`。

## 编译运行

```bash
cd nmap-toolkit

# 编译
mvn clean compile

# 运行（需 JavaFX 模块）
mvn javafx:run

# 或打包为可执行 fat-jar
mvn clean package
java -jar target/nmap-toolkit-1.0.0.jar
```

> 若 `mvn javafx:run` 未配置插件，可直接用以下方式运行：
> ```bash
> mvn clean package
> java --module-path "$HOME/.m2/repository/org/openjfx/javafx-controls/21.0.4" \
>      --add-modules javafx.controls,javafx.fxml,javafx.web \
>      -cp target/nmap-toolkit-1.0.0.jar com.nmaptoolkit.Launcher
> ```
> 但 shade 打包后通常可直接 `java -jar` 运行（JavaFX 已被打进 fat-jar）。

## 功能说明

### 命令生成器
- **分组折叠面板**：端口控制 / 主机发现 / 扫描控制 / 服务与系统探测 / 输出设置 / 伪装代理网络
- **扫描类型**：SYN / TCP / UDP / NULL / FIN / Xmas / ACK / 窗口 / Maimon / SCTP 等
- **时序模板**：-T0 ~ -T5
- **端口预设**：自定义端口、Top Ports、快速扫描、全端口
- **快捷预设**：Web 快速 / 全端口 / 隐蔽扫描
- **实时命令预览**：任何配置改动自动刷新
- **命令历史**：自动记录最近 20 条（双击回填）
- **方案保存/加载**：配置持久化到 `~/.nmap-toolkit/config.json`

### 报告生成器
- **三种输入**：粘贴、导入文件、载入示例
- **多格式解析**：普通输出(-oN)、XML(-oX)、Grepable(-oG)
- **自动风险定级**：
  - 🔴 严重：高危端口(445/135/3389/1433/6379 等) + 已知漏洞特征
  - 🟠 高危：弱口令/未授权服务(FTP/Telnet/MySQL/Redis/MongoDB/VNC)
  - 🟡 中危：明文传输/版本泄露(HTTP/SMTP/POP3)
  - 🔵 信息：其他开放端口
- **HTML 报告**：风险概览卡片 + 主机详情表格
- **导出**：HTML 文件 / PDF（通过系统打印对话框）

### 大模型分析（AI 智能分析）
- **支持任意 OpenAI 兼容接口**：OpenAI、DeepSeek、通义千问、Kimi、Ollama 等
- **内置 4 套提示词模板**：
  - 安全综合分析：全面评估整体风险、关键风险点、暴露面、加固建议
  - 漏洞评估：聚焦版本脆弱性与 CVE 关联
  - 修复建议：输出可落地的整改加固措施
  - 报告摘要：生成简洁的扫描结论摘要
- **一键分析**：点击「大模型分析」自动将扫描结果 + 提示词发送给模型
- **结果融合**：AI 分析结果自动融入 HTML 报告（独立「AI 智能分析」区块）
- **配置持久化**：API 地址、Key、模型名保存到 `~/.nmap-toolkit/llm-config.json`

> 使用方式：解析报告后，点击「大模型配置」填入 API 地址和 Key，再点击「大模型分析」即可。

## 项目结构

```
nmap-toolkit/
├── pom.xml
└── src/main/
    ├── java/com/nmaptoolkit/
    │   ├── Launcher.java            # 启动入口
    │   ├── MainApp.java             # JavaFX 主应用
    │   ├── model/
    │   │   ├── ScanConfig.java      # 扫描配置模型 + 命令生成
    │   │   └── ConfigStore.java     # 配置持久化
    │   ├── report/
    │   │   ├── NmapReport.java      # 报告数据模型
    │   │   ├── NmapParser.java      # 输出解析器
    │   │   └── HtmlReportGenerator.java
    │   └── ui/
    │       ├── MainWindow.java
    │       ├── CommandGeneratorPane.java
    │       └── ReportGeneratorPane.java
    └── resources/css/style.css
```
## 免责声明

本工具为安全测试辅助工具，仅供授权范围内的渗透测试、安全评估和学习使用。请勿用于任何非法用途，使用者须遵守所在地法律法规。
