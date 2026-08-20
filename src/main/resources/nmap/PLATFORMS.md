# 内置 nmap 平台说明

本目录存放内置的 nmap 二进制，按平台分目录组织。数据目录 `share/` 为跨平台通用，无需重复放置。

## 目录结构

```
nmap/
├── darwin-arm64/nmap              # macOS Apple Silicon (M1/M2/M3)
├── darwin-x86_64/                 # macOS Intel（bin/lib 布局）
│   ├── bin/nmap
│   └── lib/*.dylib
├── linux-x86_64/nmap              # Linux x86_64 / amd64
├── linux-arm64/nmap               # Linux ARM64 (aarch64)
├── windows-x86_64/                # Windows x86_64（exe + DLL 同目录）
│   ├── nmap.exe
│   ├── libcrypto-3.dll
│   ├── libssl-3.dll
│   ├── libssh2.dll
│   └── zlibwapi.dll
└── share/                         # 跨平台通用数据目录（NSE 脚本/nselib/指纹库）
```

## 各平台内置状态

| 平台 | 状态 | 说明 |
|------|------|------|
| darwin-arm64 | ✅ 已内置 | Homebrew 构建，依赖系统库 |
| darwin-x86_64 | ✅ 已内置 | 官方 dmg 提取，带 bin/lib 布局（@rpath 依赖） |
| linux-x86_64 | ✅ 已内置 | 官方 RPM 提取，动态链接（依赖系统 libc/libpcap） |
| linux-arm64 | ⬜ 未内置 | 官方无预编译包，需源码交叉编译 |
| windows-x86_64 | ✅ 已内置 | 官方安装包提取（nmap.exe + 4 DLL） |

## 各平台注意事项

### macOS x86_64（Intel）
- 二进制使用 `@rpath` 引用 `../lib` 下的 dylib，解压时保持 `bin/nmap` + `lib/` 结构即可
- dylib 已做去符号链接处理（实际引用的名字 `libpcap.A.dylib`、`libz.1.dylib` 等均为实体文件）

### Linux x86_64
- 二进制为动态链接，依赖 `libc`、`libpcap` 等系统库（现代 Linux 发行版自带）
- 若精简发行版缺 `libpcap`，需 `apt install libpcap0.8` 或 `yum install libpcap`

### Windows x86_64
- 已内置 `nmap.exe` + 4 个 DLL（libcrypto/libssl/libssh2/zlibwapi）
- **重要限制**：`-sS`（SYN 半开）等原始包扫描依赖 **Npcap 驱动**（内核驱动，无法打包进 jar）
  - 需要用户手动安装 Npcap：https://npcap.com/ （安装时勾选 "Install Npcap in WinPcap API-compatible Mode"）
  - 不装 Npcap 时，`-sT`（TCP connect 扫描）、版本探测、NSE 脚本等仍可正常工作
- MSVC 运行时（MSVCP140.dll 等）在 Win10/11 通常已自带，缺失时会提示

## 如何为其他平台补充二进制

1. 在对应平台（或交叉编译环境）获取 nmap 二进制
2. 放到对应目录，命名规范：
   - macOS/Linux: `nmap`（无扩展名）
   - Windows: `nmap.exe`
   - macOS x86_64 需用 `bin/nmap` + `lib/` 布局（若有 @rpath 依赖）
3. 若有额外的 DLL/dylib 依赖，生成对应 `.filelist` / `.libfilelist` 清单
4. 重新打包：`./mvnw clean package`

## 数据目录说明

`share/` 目录为跨平台通用的纯文本数据（Lua 脚本、指纹库、服务探测库），各平台共享一份，无需重复。
