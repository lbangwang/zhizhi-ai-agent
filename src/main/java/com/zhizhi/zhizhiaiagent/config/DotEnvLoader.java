package com.zhizhi.zhizhiaiagent.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 启动前加载项目根目录 {@code .env} 到 System Property。
 * <p>
 * 不覆盖已存在的环境变量 / 系统属性（IDEA Run Configuration 优先）。
 */
public final class DotEnvLoader {

    private DotEnvLoader() {
    }

    public static void load() {
        Path envFile = resolveEnvFile();
        if (envFile == null || !Files.isRegularFile(envFile)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.length() >= 2) {
                    char first = value.charAt(0);
                    char last = value.charAt(value.length() - 1);
                    if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                        value = value.substring(1, value.length() - 1);
                    }
                }
                if (key.isEmpty()) {
                    continue;
                }
                // 已有环境变量或系统属性时不覆盖
                if (System.getenv(key) != null) {
                    continue;
                }
                if (System.getProperty(key) != null) {
                    continue;
                }
                System.setProperty(key, value);
            }
            System.out.println("[DotEnvLoader] loaded: " + envFile.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[DotEnvLoader] failed to read " + envFile + ": " + e.getMessage());
        }
    }

    private static Path resolveEnvFile() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path candidate = cwd.resolve(".env");
        if (Files.isRegularFile(candidate)) {
            return candidate;
        }
        // IDEA 有时工作目录是 module 子目录
        Path parent = cwd.getParent();
        if (parent != null) {
            Path parentEnv = parent.resolve(".env");
            if (Files.isRegularFile(parentEnv)) {
                return parentEnv;
            }
        }
        return null;
    }
}
