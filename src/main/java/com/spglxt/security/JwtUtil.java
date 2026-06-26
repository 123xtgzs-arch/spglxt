package com.spglxt.security;

import com.spglxt.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 
 * @author spglxt
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    /**
     * 获取签名密钥
     */
    private Key getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT令牌
     *
     * @param username 用户名
     * @return JWT令牌
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        return createToken(claims, username);
    }

    /**
     * 创建JWT令牌
     *
     * @param claims   自定义声明
     * @param subject  主题(用户名)
     * @return JWT令牌
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("从令牌获取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从令牌中获取声明
     *
     * @param token JWT令牌
     * @return Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证令牌是否有效
     *
     * @param token    JWT令牌
     * @param username 用户名
     * @return 是否有效
     */
    public boolean validateToken(String token, String username) {
        try {
            String tokenUsername = getUsernameFromToken(token);
            return (tokenUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查令牌是否过期
     *
     * @param token JWT令牌
     * @return 是否过期
     */
    private boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新令牌
     *
     * @param token 旧令牌
     * @return 新令牌
     */
    public String refreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String username = claims.getSubject();
            return generateToken(username);
        } catch (Exception e) {
            log.error("刷新令牌失败: {}", e.getMessage());
            return null;
        }
    }
}
