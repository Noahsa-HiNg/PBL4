package com.pbl4.server.controller;

import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pbl4.common.model.User;
import com.pbl4.server.service.DashboardService;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final DashboardService dashboardService;
    public UserController(UserService userService, DashboardService dashboardService) {
        this.userService = userService;
        this.dashboardService = dashboardService; // Tiêm dependency
    }
    // API để tạo user mới
//    @PostMapping
//    public ResponseEntity<User> createUser(@RequestBody User user) {
//        User createdUser = userService.createUser(user);
//        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
//    }

    // API để lấy tất cả user
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // API để lấy một user theo ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable int id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable int id, @RequestBody User userDetails) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
     
        String currentUserRole = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", "")) 
                .orElse("VIEWER");
        Long currentUserId = userService.getUserIdByUsername(username);
        

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        try {
            User updatedUser = userService.updateUser(id, userDetails, currentUserId,currentUserRole);
            return ResponseEntity.ok(updatedUser);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // 403 FORBIDDEN
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // API để xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable int id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam("keyword") String keyword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra nếu chưa xác thực (dù SecurityConfig đã làm, đây là lớp bảo vệ cuối)
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401
        }
        
        // 2. TRÍCH XUẤT ROLE
        String currentUserRole = authentication.getAuthorities().stream()
            .findFirst()
            .map(auth -> auth.getAuthority().replace("ROLE_", "")) // Loại bỏ tiền tố "ROLE_"
            .orElse("VIEWER"); // Mặc định là VIEWER nếu không tìm thấy

        // 3. ÁP DỤNG KIỂM TRA PHÂN QUYỀN (ADMIN ONLY)
        if (!currentUserRole.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 FORBIDDEN
        }
        
        // 4. Thực thi logic nếu là ADMIN
        try {
            List<User> users = userService.searchUsers(keyword);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            System.err.println("Error searching users: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable("id") int targetUserId) {
        
        // --- LOGIC KIỂM TRA QUYỀN ADMIN ---
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserRole = authentication.getAuthorities().stream()
            .findFirst()
            .map(auth -> auth.getAuthority().replace("ROLE_", ""))
            .orElse("VIEWER");

        // YÊU CẦU QUYỀN ADMIN ĐỂ XEM THỐNG KÊ CỦA BẤT KỲ USER NÀO
        if (!currentUserRole.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access Denied: Requires ADMIN role.")); // 403 FORBIDDEN
        }
        
        // --- THỰC HIỆN LOGIC ---
        try {
            // Gọi Service để lấy số liệu thống kê
            Map<String, Long> stats = dashboardService.getUserStats((long) targetUserId);
            
            // 200 OK với JSON: {"totalClients": 5, "totalCameras": 12}
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error fetching user stats for ID " + targetUserId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error fetching stats."));
        }
    }
}