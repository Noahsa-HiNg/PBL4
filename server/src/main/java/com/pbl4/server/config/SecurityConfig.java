package com.pbl4.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Sử dụng thuật toán mã hóa BCrypt, là tiêu chuẩn hiện nay
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tạm thời vô hiệu hóa CSRF để dễ test API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll() // Tạm thời cho phép tất cả request đến /api/
                .anyRequest().authenticated()
            );
        return http.build();
    }
}