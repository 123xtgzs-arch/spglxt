package com.spglxt.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Dotenv配置 - 加载.env文件
 * 在应用启动时自动读取项目根目录的.env文件
 * 
 * @author spglxt
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            Dotenv dotenv = null;
            // 发布包：config/.env；开发：根目录 .env
            for (String dir : new String[]{"./config", "./"}) {
                dotenv = Dotenv.configure()
                        .directory(dir)
                        .ignoreIfMissing()
                        .load();
                if (!dotenv.entries().isEmpty()) {
                    break;
                }
            }
            if (dotenv == null || dotenv.entries().isEmpty()) {
                System.out.println("ℹ️  未找到 .env，使用 application.yml 默认配置");
                return;
            }

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            Map<String, Object> dotenvProperties = new HashMap<>();

            // 将.env中的所有变量添加到Spring环境中
            dotenv.entries().forEach(entry -> {
                dotenvProperties.put(entry.getKey(), entry.getValue());
            });

            // 将属性添加到Spring Environment，优先级高于application.yml
            environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenvProperties", dotenvProperties)
            );

            System.out.println("✅ 成功加载 .env 配置文件");
            
        } catch (Exception e) {
            System.err.println("⚠️  警告: 无法加载 .env 文件，将使用 application.yml 中的配置");
            System.err.println("   错误信息: " + e.getMessage());
        }
    }
}
