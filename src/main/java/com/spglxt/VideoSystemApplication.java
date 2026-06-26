package com.spglxt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 次元方舟视频管理系统 - Spring Boot 主应用类
 * 
 * @author spglxt
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching          // 启用缓存
@EnableJpaAuditing      // 启用JPA审计
@EnableScheduling       // 启用定时任务
@EnableAsync            // 启用异步任务
public class VideoSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoSystemApplication.class, args);
        System.out.println("\n" +
                "=======================================================\n" +
                "   次元方舟视频管理系统启动成功！\n" +
                "   System: Video Management System\n" +
                "   Version: 1.0.0\n" +
                "   访问地址: http://localhost:8080\n" +
                "=======================================================\n");
    }
}
