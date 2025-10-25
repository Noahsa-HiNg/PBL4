package com.pbl4.server.controller;

import com.pbl4.server.security.JwtTokenProvider; // Import class bạn đã tạo ở Bước 2
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        // Filter (JwtAuthenticationFilter) đã xử lý token
        // và đưa thông tin vào SecurityContextHolder
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not logged in"));
        }

        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                            .findFirst()
                            .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                            .orElse("UNKNOWN");

        // Trả về thông tin cơ bản
        return ResponseEntity.ok(Map.of("username", username, "role", role));
    }
}