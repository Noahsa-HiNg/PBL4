package com.pbl4.server.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {

    /**
     * API endpoint này dùng để kiểm tra xem server có đang chạy hay không.
     * Nó không cần kết nối database hay xử lý logic phức tạp.
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> checkHealth() {
        // Trả về một đối tượng Map đơn giản, Spring Boot sẽ tự động chuyển thành JSON
        // Ví dụ: {"status": "UP"}
        Map<String, String> response = Map.of("status", "UP");
        return ResponseEntity.ok(response);
    }
}