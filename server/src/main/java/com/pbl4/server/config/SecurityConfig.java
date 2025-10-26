package com.pbl4.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
// Import filter JWT bạn đã tạo
import com.pbl4.server.security.JwtAuthenticationFilter; 
import com.pbl4.server.service.UserDetailsServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
// Import class mới này
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// Import class mới này
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// Import class mới này
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; 

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    @Autowired
    private ObjectMapper objectMapper;
    
    // 1. Tiêm Filter JWT bạn đã tạo
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // 2. Sửa Bean này để nó có thể được tiêm vào AuthController
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 3. Toàn bộ SecurityFilterChain được viết lại
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tắt CSRF (vì dùng JWT)
            .cors(Customizer.withDefaults())
            // 4. Báo cho Spring Security không tạo Session (STATELESS)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 5. Thêm xử lý lỗi 401 (Khi truy cập API mà không có Token)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> data = new HashMap<>();
                    data.put("message", "Yêu cầu xác thực. Vui lòng đăng nhập.");
                    data.put("error", "Unauthorized");
                    response.getWriter().write(objectMapper.writeValueAsString(data));
                })
            )
            
            // 6. Phân quyền Request
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/error").permitAll() 
                // Tất cả các API khác bắt đầu bằng /api/ đều yêu cầu xác thực
                .requestMatchers("/api/**").authenticated() 
                // Bất kỳ request nào khác cũng cần xác thực
                .anyRequest().authenticated() 
            )
            
            // 7. Thêm Filter JWT vào trước Filter mặc định
            // Filter này sẽ đọc Header "Authorization" và xác thực Token
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // KHÔNG CÒN .formLogin() Ở ĐÂY NỮA
        
        return http.build();
    }
}