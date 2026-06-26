package com.spglxt.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

/**
 * 自定义用户详情服务
 * 
 * @author spglxt
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    private String encodedAdminPassword;

    @PostConstruct
    void initEncodedPassword() {
        encodedAdminPassword = PASSWORD_ENCODER.encode(adminPassword);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户: {}", username);

        if (adminUsername.equals(username)) {
            return User.builder()
                    .username(adminUsername)
                    .password(encodedAdminPassword)
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false)
                    .build();
        }

        throw new UsernameNotFoundException("用户不存在: " + username);
    }
}
