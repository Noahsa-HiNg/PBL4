package com.pbl4.server.controller;

import com.pbl4.server.service.DashboardService;
import com.pbl4.server.service.CameraService; // BỔ SUNG
import com.pbl4.server.service.ClientService;
import com.pbl4.server.service.UserService; // BỔ SUNG
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable; // BỔ SUNG
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.EntityNotFoundException; // BỔ SUNG
import pbl4.common.model.Client;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private UserService userService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private CameraService cameraService; 
    @GetMapping("/metrics")
    public ResponseEntity<?> getAdminMetrics() {
        // SecurityConfig sẽ xử lý quyền ADMIN cho "/api/admin/**"
        
        try {
            Map<String, Long> metrics = dashboardService.getAdminMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi tải số liệu chung."));
        }
    }

    @GetMapping("/users/{id}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable Long id) {
        try {
            userService.getUserById(id.intValue()); 
            Map<String, Long> stats = dashboardService.getUserStats(id);
            return ResponseEntity.ok(stats);
            
        } catch (EntityNotFoundException e) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND)
                     .body(Map.of("message", "Không tìm thấy User với ID: " + id));
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(Map.of("message", "Lỗi khi tải thống kê User."));
        }
    }

    @GetMapping("/cameras/{id}/stats")
    public ResponseEntity<?> getCameraStats(@PathVariable int id) {
        try {
            cameraService.getCameraById(id); 

            Map<String, Long> stats = dashboardService.getCameraStats(id);
            return ResponseEntity.ok(stats);
            
        } catch (EntityNotFoundException e) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND)
                     .body(Map.of("message", "Không tìm thấy Camera với ID: " + id));
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(Map.of("message", "Lỗi khi tải thống kê Camera."));
        }
    }
    @GetMapping("/users/{id}/clients")
    public ResponseEntity<?> getClientsForUser(@PathVariable int id) {
        
        try {
            List<Client> clients = clientService.getClientsByUserId((long) id); 
            
            return ResponseEntity.ok(clients);

        } catch (EntityNotFoundException e) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND)
                     .body(Map.of("message", "Không tìm thấy User với ID: " + id));
        } catch (Exception e) {
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(Map.of("message", "Lỗi khi tải danh sách Clients."));
        }
    }
}