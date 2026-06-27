package com.spglxt.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轻量 .env 解析（GraalVM 原生镜像友好，不依赖 dotenv-java）
 */
public final class EnvLoader {

    private EnvLoader() {
    }

    public static Map<String, String> loadFromCommonPaths() {
        for (String dir : new String[]{"config", "."}) {
            Path envFile = Path.of(dir, ".env");
            Map<String, String> vars = load(envFile);
            if (!vars.isEmpty()) {
                return vars;
            }
        }
        return Map.of();
    }

    public static Map<String, String> load(Path envFile) {
        if (!Files.isRegularFile(envFile)) {
            return Map.of();
        }
        Map<String, String> vars = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (!key.isEmpty()) {
                    vars.put(key, value);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取 .env 失败: " + envFile, e);
        }
        return vars;
    }
}
