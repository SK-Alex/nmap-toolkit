package com.nmaptoolkit.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 命令执行器：后台运行命令，实时回传输出，支持中止
 */
public class CommandExecutor {

    private Process process;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final StringBuilder output = new StringBuilder();

    /**
     * 执行命令（阻塞在后台线程），实时推送输出
     *
     * @param commandLine 完整命令行（已按空格拆分，或直接传入数组）
     * @param onOutput    输出回调（增量行）
     * @param onFinish    完成回调（exitCode + 完整输出）
     */
    public void execute(List<String> args, Consumer<String> onOutput, Consumer<Integer> onFinish) {
        running.set(true);
        output.setLength(0);

        Thread t = new Thread(() -> {
            int exit = -1;
            try {
                ProcessBuilder pb = new ProcessBuilder(args);
                pb.redirectErrorStream(true); // 合并 stderr 到 stdout
                process = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append('\n');
                        if (onOutput != null) onOutput.accept(line);
                    }
                }
                exit = process.waitFor();
            } catch (Exception e) {
                output.append("[执行异常] ").append(e.getMessage()).append('\n');
                if (onOutput != null) onOutput.accept("[执行异常] " + e.getMessage());
            } finally {
                running.set(false);
                if (onFinish != null) onFinish.accept(exit);
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * 执行命令，等待完成并返回完整输出
     */
    public String executeAndWait(List<String> args) {
        StringBuilder sb = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            p.waitFor();
        } catch (Exception e) {
            sb.append("[执行异常] ").append(e.getMessage());
        }
        return sb.toString();
    }

    /**
     * 中止当前执行的命令
     */
    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroy();
            // 若未退出，强制 kill
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getOutput() {
        return output.toString();
    }

    /**
     * 检测命令是否可用
     */
    public static boolean isCommandAvailable(String cmd) {
        try {
            List<String> args = new ArrayList<>();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                args.add("where");
            } else {
                args.add("which");
            }
            args.add(cmd);
            Process p = new ProcessBuilder(args).start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
