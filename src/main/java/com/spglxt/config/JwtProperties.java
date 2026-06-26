package com.spglxt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 * 
 * @author spglxt
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * JWT密钥
     */
    private String secret;

    /**
     * JWT过期时间 (毫秒)
     */
    private Long expiration;

    /**
     * JWT请求头名称
     */
    private String header;

    /**
     * JWT令牌前缀
     */
    private String tokenPrefix;
}
