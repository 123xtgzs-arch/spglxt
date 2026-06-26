package com.spglxt.controller;

import com.spglxt.common.Result;
import com.spglxt.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 同时处理前端页面和API请求
 * 
 * @author spglxt
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${app.admin.username}")
    private String adminUsername;

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * 表单登录(与PHP版 doLogin 一致)
     */
    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(required = false) String remember,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSession session = request.getSession(true);
            session.setAttribute("username", username);
            session.setAttribute("token", jwtUtil.generateToken(username));
            if ("1".equals(remember)) {
                session.setMaxInactiveInterval(30 * 24 * 60 * 60);
            }

            log.info("用户 {} 表单登录成功", username);
            return "redirect:/dashboard";
        } catch (AuthenticationException e) {
            log.error("用户 {} 表单登录失败: {}", username, e.getMessage());
            redirectAttributes.addFlashAttribute("error", "用户名或密码错误");
            return "redirect:/login";
        }
    }

    /**
     * 退出登录(页面链接使用 GET, 与PHP版一致)
     */
    @GetMapping({"/logout", "/api/auth/logout"})
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    /**
     * 用户登录 (API接口)
     *
     * @param loginRequest 登录请求
     * @return JWT令牌
     */
    @PostMapping("/api/auth/login")
    @ResponseBody
    public Result<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // 生成JWT令牌
            String token = jwtUtil.generateToken(loginRequest.getUsername());

            Map<String, String> result = new HashMap<>();
            result.put("token", token);
            result.put("username", loginRequest.getUsername());

            log.info("用户 {} 登录成功", loginRequest.getUsername());
            return Result.success(result, "登录成功");

        } catch (AuthenticationException e) {
            log.error("用户 {} 登录失败: {}", loginRequest.getUsername(), e.getMessage());
            return Result.error("用户名或密码错误");
        }
    }

    /**
     * 刷新令牌
     *
     * @param token 旧令牌
     * @return 新令牌
     */
    @PostMapping("/api/auth/refresh")
    @ResponseBody
    public Result<Map<String, String>> refresh(@RequestParam String token) {
        String newToken = jwtUtil.refreshToken(token);
        if (newToken != null) {
            Map<String, String> result = new HashMap<>();
            result.put("token", newToken);
            return Result.success(result, "令牌刷新成功");
        }
        return Result.error("令牌刷新失败");
    }

    /**
     * 登出
     *
     * @return 登出结果
     */
    @PostMapping("/api/auth/logout")
    @ResponseBody
    public Result<Void> logout() {
        // JWT是无状态的,客户端删除令牌即可
        return Result.success("登出成功");
    }

    /**
     * 获取当前用户信息
     *
     * @param authentication 认证对象
     * @return 用户信息
     */
    @GetMapping("/api/auth/user")
    @ResponseBody
    public Result<Map<String, String>> getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Map<String, String> user = new HashMap<>();
            user.put("username", authentication.getName());
            user.put("role", "admin");
            return Result.success(user);
        }
        return Result.error(401, "未认证");
    }

    /**
     * 登录请求DTO
     */
    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }
}
