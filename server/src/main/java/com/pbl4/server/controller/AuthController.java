package com.pbl4.server.controller;

import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.security.JwtTokenProvider; // Import class bạn đã tạo ở Bước 2
import com.pbl4.server.service.DashboardService;
import pbl4.common.model.User;
import com.pbl4.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * Lớp DTO (Data Transfer Object) để nhận JSON từ client.
 * Client (JavaFX) của bạn đã có lớp này, server cũng cần nó.
 */
class LoginRequest {
    private String username;
    private String password;

    // Cần Getters để Jackson (ObjectMapper) đọc dữ liệu
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
}


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // --- PHẦN THÊM VÀO CHO VIỆC ĐĂNG NHẬP ---

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtTokenProvider tokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DashboardService dashboardService;
    /**
     * Đây là Endpoint Đăng nhập DUY NHẤT.
     * Nó nhận JSON (username, password) và trả về JSON (token).
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
    	System.out.println("SERVER RECEIVED: User=[" + loginRequest.getUsername() + "], Pass=[" + loginRequest.getPassword() + "]");

        // ==== ĐOẠN CODE TEST TỐI THƯỢNG ====
        String rawPassFromClient = loginRequest.getPassword();
        String hardcodedGoodPass = "password123"; // Đây là chuỗi chuẩn
        String hashFromDb = "$2a$10$P.ET/O0nF.2v0/AWE1iVROvXog.l.fGCuW0uWq.dG53s/n.pWk.KW";
        
        System.out.println("==== DEBUG: Client Pass Length: " + rawPassFromClient.length());
        
        // Test 1: So sánh mật khẩu TỪ CLIENT (Chúng ta biết đây là 'false')
        boolean isMatch_Client = passwordEncoder.matches(rawPassFromClient, hashFromDb);
        System.out.println("==== BCrypt Test Result (from Client): " + isMatch_Client + " ====");
        
        // Test 2: So sánh mật khẩu CHUẨN (hardcode)
        boolean isMatch_Hardcoded = passwordEncoder.matches(hardcodedGoodPass, hashFromDb);
        System.out.println("==== BCrypt Test Result (Hardcoded): " + isMatch_Hardcoded + " ====");
        
        System.out.println("==== Injected Encoder Class: " + passwordEncoder.getClass().getName() + " ====");
        
        // ==== KẾT THÚC TEST ====

        // 1. Xác thực username/password từ request
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 2. Nếu xác thực thành công, set vào SecurityContext (cho request này)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Tạo JWT Token
        String jwt = tokenProvider.generateToken(authentication);

        // 4. Trả về Token cho client (khớp với LoginResponse của client)
        // Client JavaFX của bạn đang mong đợi 1 trường tên là "token"
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    // --- KẾT THÚC PHẦN THÊM VÀO ---


    /**
     * Endpoint này (bạn đã viết) dùng để client kiểm tra xem
     * token của mình có hợp lệ không và lấy thông tin user.
     */
    @Autowired // Cần thiết để lấy thông tin chi tiết User
    private UserService userService1; 

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Kiểm tra xác thực (Giữ nguyên)
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not logged in"));
        }

        String username = authentication.getName();
        
        // 2. TRUY VẤN CƠ SỞ DỮ LIỆU ĐỂ LẤY EMAIL VÀ ID
        // Bạn cần đảm bảo UserService có phương thức findByUsername
        UserEntity userEntity = userService1.findByUsername(username); 
        
        if (userEntity == null) {
            // Nếu Token hợp lệ nhưng user không còn trong DB
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found in database."));
        }

        // 3. Xây dựng phản hồi với thông tin chi tiết
        String role = authentication.getAuthorities().stream()
                            .findFirst()
                            .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                            .orElse("VIEWER");

        Map<String, Object> response = new HashMap<>();
        response.put("id", userEntity.getId()); // Bổ sung ID (rất quan trọng cho Frontend)
        response.put("username", username);
        response.put("email", userEntity.getEmail()); // BỔ SUNG EMAIL
        response.put("role", role);

        return ResponseEntity.ok(response);
    }
    @Autowired // <-- Bổ sung UserService
    private UserService userService;
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User registrationRequest) {
        try {
            // 1. Dùng Service để tạo User mới.
            // Service sẽ xử lý: kiểm tra trùng username, mã hóa mật khẩu, và lưu DB.
            User createdUser = userService1.createUser(registrationRequest);

            // 2. Trả về thông báo thành công. KHÔNG trả về mật khẩu hash.
            // Dùng Map để định dạng JSON phản hồi.
            Map<String, Object> response = Map.of(
                "message", "User registered successfully!",
                "username", createdUser.getUsername(),
                "id", createdUser.getId()
            );

            return new ResponseEntity<>(response, HttpStatus.CREATED); // Mã 201 CREATED
            
        } catch (RuntimeException e) {
            // Xử lý lỗi khi username đã tồn tại (nếu Service ném ra RuntimeException)
            if (e.getMessage().contains("Username already exists")) {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
            }
            // Xử lý các lỗi Service khác (ví dụ: DB lỗi)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }
    @GetMapping("/admin/metrics")
    public ResponseEntity<?> getAdminMetrics() {
        // 1. Kiểm tra quyền ADMIN (Bắt buộc)
        // Giả định logic kiểm tra role đã có sẵn hoặc được xử lý trong SecurityConfig
        
        try {
            Map<String, Long> metrics = dashboardService.getAdminMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi khi tải số liệu."));
        }
    }
}