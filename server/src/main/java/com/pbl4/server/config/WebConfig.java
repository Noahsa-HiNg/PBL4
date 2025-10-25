package com.pbl4.server.config; // Đảm bảo đúng package

import org.springframework.context.annotation.Configuration; // Đảm bảo có import này
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // Đảm bảo có annotation này
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Áp dụng cho tất cả API bắt đầu bằng /api/
                .allowedOrigins( // Liệt kê CHÍNH XÁC các nguồn gốc được phép
                    "http://127.0.0.1:5500",
                    "http://localhost:5500"              
                    // Thêm các nguồn gốc khác nếu cần
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Các phương thức HTTP
                .allowedHeaders("*") // Cho phép mọi header
                .allowCredentials(true); // QUAN TRỌNG: Cho phép gửi cookie (cho session)
    }
}