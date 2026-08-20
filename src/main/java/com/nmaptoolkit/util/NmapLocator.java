package com.nmaptoolkit.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Nmap 定位器：负责找到可用的 nmap 可执行文件与数据目录，支持跨平台。
 *
 * 优先级：
 * 1. 系统 PATH 中已安装的 nmap（使用系统数据目录）
 * 2. jar 内置的 nmap（按当前 OS + 架构选择对应二进制，解压到 ~/.nmap-toolkit/nmap/）
 *
 * jar 内资源结构：
 * <pre>
 * nmap/
 * ├── darwin-arm64/nmap
 * ├── darwin-x86_64/nmap
 * ├── linux-x86_64/nmap
 * ├── linux-arm64/nmap
 * ├── windows-x86_64/nmap.exe
 * └── share/          (跨平台通用数据目录)
 *     ├── .filelist
 *     └── nmap/...
 * </pre>
 */
public class NmapLocator {

    private static Path bundledNmapBin;
    private static Path bundledDataDir;
    private static volatile boolean initialized = false;

    /**
     * 平台标识 -> jar 资源目录（相对 /nmap/）
     */
    private static final Map<String, String> PLATFORM_DIRS = new LinkedHashMap<>();
    static {
        PLATFORM_DIRS.put("macos-arm64", "darwin-arm64");
        PLATFORM_DIRS.put("macos-x86_64", "darwin-x86_64");
        PLATFORM_DIRS.put("linux-x86_64", "linux-x86_64");
        PLATFORM_DIRS.put("linux-arm64", "linux-arm64");
        PLATFORM_DIRS.put("linux-aarch64", "linux-arm64");
        PLATFORM_DIRS.put("windows-x86_64", "windows-x86_64");
        PLATFORM_DIRS.put("windows-amd64", "windows-x86_64");
    }

    public static class Location {
        public String executable;   // nmap 可执行文件绝对路径（或命令名）
        public String dataDir;      // 数据目录（--datadir），可能为 null
        public boolean bundled;     // 是否使用内置 nmap
        public String platform;     // 实际使用的平台标识
    }

    /**
     * 定位 nmap
     */
    public static synchronized Location locate() {
        // 优先系统 nmap
        if (CommandExecutor.isCommandAvailable("nmap")) {
            Location l = new Location();
            l.executable = "nmap";
            l.dataDir = null;
            l.bundled = false;
            l.platform = currentPlatform();
            return l;
        }
        // 尝试解压内置 nmap（按平台选择）
        Path bin = extractBundled();
        if (bin != null) {
            Location l = new Location();
            l.executable = bin.toAbsolutePath().toString();
            l.dataDir = bundledDataDir != null ? bundledDataDir.toAbsolutePath().toString() : null;
            l.bundled = true;
            l.platform = currentPlatform();
            return l;
        }
        return null;
    }

    /**
     * 判断当前平台是否有内置 nmap（不触发解压）
     */
    public static boolean hasBundledNmap() {
        String platform = currentPlatform();
        String dir = PLATFORM_DIRS.get(platform);
        if (dir == null) return false;
        String resPath = usesBinLibLayout(platform)
                ? "/nmap/" + dir + "/bin/" + binName()
                : "/nmap/" + dir + "/" + binName();
        return NmapLocator.class.getResourceAsStream(resPath) != null;
    }

    /**
     * 当前平台标识，如 macos-arm64
     */
    public static String currentPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = normalizeArch(System.getProperty("os.arch", ""));

        if (os.contains("mac") || os.contains("darwin")) {
            return "macos-" + arch;
        } else if (os.contains("win")) {
            return "windows-" + arch;
        } else if (os.contains("linux")) {
            return "linux-" + arch;
        }
        return os + "-" + arch;
    }

    /**
     * 规范化架构标识：amd64/aarch64 -> x86_64/arm64
     */
    private static String normalizeArch(String arch) {
        if (arch == null) return "";
        arch = arch.toLowerCase();
        return switch (arch) {
            case "amd64", "x86_64" -> "x86_64";
            case "aarch64", "arm64" -> "arm64";
            default -> arch;
        };
    }

    private static String binName() {
        return currentPlatform().startsWith("windows") ? "nmap.exe" : "nmap";
    }

    /**
     * 平台是否使用 bin/lib 布局（二进制在 bin/ 子目录，依赖库在 lib/ 子目录）。
     * 目前仅 macOS x86_64 官方包采用此布局（二进制用 @rpath 引用 ../lib）。
     */
    private static boolean usesBinLibLayout(String platform) {
        return "macos-x86_64".equals(platform);
    }

    /**
     * 解压 jar 内置的 nmap 到用户目录
     */
    private static synchronized Path extractBundled() {
        if (initialized) return bundledNmapBin;
        initialized = true;

        String platform = currentPlatform();
        String platformDir = PLATFORM_DIRS.get(platform);
        if (platformDir == null) return null;

        try {
            Path baseDir = Paths.get(System.getProperty("user.home"), ".nmap-toolkit", "nmap");
            String name = binName();
            Path dataDir = baseDir.resolve("share");

            // 计算二进制路径（bin/lib 布局则二进制在 bin/ 子目录）
            Path binFile = usesBinLibLayout(platform)
                    ? baseDir.resolve("bin").resolve(name)
                    : baseDir.resolve(name);

            // 若已解压且可执行，直接复用
            if (Files.isExecutable(binFile)) {
                bundledNmapBin = binFile;
                bundledDataDir = dataDir;
                return binFile;
            }

            // 解压对应平台的二进制
            String binRes = usesBinLibLayout(platform)
                    ? "/nmap/" + platformDir + "/bin/" + name
                    : "/nmap/" + platformDir + "/" + name;
            try (InputStream bin = NmapLocator.class.getResourceAsStream(binRes)) {
                if (bin == null) return null; // 该平台无内置 nmap
                Files.createDirectories(binFile.getParent());
                Files.copy(bin, binFile, StandardCopyOption.REPLACE_EXISTING);
                makeExecutable(binFile);
            }

            // Windows：解压同目录 DLL（nmap.exe 依赖）
            if (platform.startsWith("windows")) {
                extractDir("/nmap/" + platformDir, "/nmap/" + platformDir + "/.filelist", baseDir, name);
            }
            // macOS x86_64：解压 lib 目录（二进制 @rpath 依赖 ../lib）
            if (usesBinLibLayout(platform)) {
                extractDir("/nmap/" + platformDir + "/lib", "/nmap/" + platformDir + "/.libfilelist", baseDir.resolve("lib"));
            }

            // 解压数据目录（跨平台通用，资源根为 /nmap/share/nmap）
            extractDir("/nmap/share/nmap", "/nmap/share/.filelist", dataDir);

            bundledNmapBin = binFile;
            bundledDataDir = dataDir;
            return binFile;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 递归解压 classpath 资源目录
     */
    private static void extractDir(String resourceRoot, String listResource, Path targetDir) throws IOException {
        extractDir(resourceRoot, listResource, targetDir, null);
    }

    /**
     * 递归解压 classpath 资源目录，可排除指定文件名（用于 Windows 排除已单独解压的 nmap.exe）
     */
    private static void extractDir(String resourceRoot, String listResource, Path targetDir, String excludeName) throws IOException {
        try (InputStream list = NmapLocator.class.getResourceAsStream(listResource)) {
            if (list == null) return;
            String content = new String(list.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String line : content.split("\\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (excludeName != null && line.equals(excludeName)) continue;
                String resPath = resourceRoot + "/" + line;
                Path outFile = targetDir.resolve(line);
                Files.createDirectories(outFile.getParent());
                try (InputStream in = NmapLocator.class.getResourceAsStream(resPath)) {
                    if (in != null) {
                        Files.copy(in, outFile, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static void makeExecutable(Path f) throws IOException {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.GROUP_EXECUTE);
            perms.add(PosixFilePermission.OTHERS_READ);
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(f, perms);
        } catch (UnsupportedOperationException ignored) {
            // Windows 等不支持 POSIX 权限
        }
    }
}
