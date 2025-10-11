package com.pbl4.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Cấu hình này cho phép các yêu cầu từ bên ngoài (ví dụ: trang web pbl4-web)
     * có thể gọi đến các API của bạn mà không bị trình duyệt chặn.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Áp dụng cho tất cả các API có đường dẫn bắt đầu bằng /api/
                .allowedOrigins("*")   // Cho phép tất cả các tên miền (domain) gọi đến
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Các phương thức được phép
                .allowedHeaders("*"); // Cho phép tất cả các header
    }
}