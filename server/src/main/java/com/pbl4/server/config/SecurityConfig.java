package com.pbl4.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.server.service.UserDetailsServiceImpl;
// KHÔNG CẦN import UserService ở đây nữa
// import com.pbl4.server.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private final UserDetailsServiceImpl userDetailsService;
    // private final UserService userService; // <-- XÓA DÒNG NÀY
    private final ObjectMapper objectMapper;

    // Sửa Constructor: Xóa tham số UserService
    public SecurityConfig(UserDetailsServiceImpl userDetailsService, /* UserService userService, */ ObjectMapper objectMapper) {
        this.userDetailsService = userDetailsService;
        // this.userService = userService; // <-- XÓA DÒNG NÀY
        this.objectMapper = objectMapper;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService)
                                     .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/login", "/error", "/assets/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("username")
                .passwordParameter("password")
                // --- SỬA LẠI SUCCESS HANDLER ---
                .successHandler((request, response, authentication) -> { // 'authentication' chứa thông tin user đã đăng nhập
                    response.setStatus(HttpStatus.OK.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    String username = authentication.getName(); // Lấy username
                    Map<String, Object> data = new HashMap<>();
                    data.put("message", "Login Successful");
                    data.put("username", username);
                    // Lấy role từ authentication (nếu có)
                    authentication.getAuthorities().stream().findFirst().ifPresent(grantedAuthority -> {
                        data.put("role", grantedAuthority.getAuthority().replace("ROLE_", "")); // Bỏ tiền tố ROLE_ nếu có
                    });

                    // Bạn không cần gọi UserService ở đây nữa
                    // Nếu muốn lấy User DTO đầy đủ, bạn phải tạo endpoint /api/auth/me riêng

                    response.getWriter().write(objectMapper.writeValueAsString(data));
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Invalid username or password\"}");
                })
                .permitAll()
            )
            .logout(/* ... cấu hình logout như cũ ... */);

        return http.build();
    }
}