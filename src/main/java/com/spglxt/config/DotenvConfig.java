package com.spglxt.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 加载 config/.env 或根目录 .env
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            Map<String, String> vars = EnvLoader.loadFromCommonPaths();
            if (vars.isEmpty()) {
                System.out.println("未找到 .env，使用 application.yml 默认配置");
                return;
            }

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            Map<String, Object> dotenvProperties = new HashMap<>(vars);
            environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenvProperties", dotenvProperties)
            );
            System.out.println("成功加载 .env 配置文件");
        } catch (Exception e) {
            System.err.println("警告: 无法加载 .env，将使用 application.yml 中的配置");
            System.err.println("错误信息: " + e.getMessage());
        }
    }
}
