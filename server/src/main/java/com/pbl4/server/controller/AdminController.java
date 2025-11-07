package com.pbl4.server.controller;


import com.pbl4.server.service.DashboardService; // Import service
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
@RestController
@RequestMapping("/api/admin")
public class AdminController {
	@Autowired
    private DashboardService dashboardService;
	@GetMapping("/metrics")
    public ResponseEntity<?> getAdminMetrics() {
        // 1. Việc kiểm tra quyền ADMIN sẽ được SecurityConfig xử lý
        //    cho toàn bộ "/api/admin/**"
        
        try {
            Map<String, Long> metrics = dashboardService.getAdminMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi tải số liệu."));
        }
    }

}
