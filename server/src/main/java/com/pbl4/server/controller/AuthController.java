package com.pbl4.server.controller;

import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.security.JwtTokenProvider; 
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

class LoginRequest {
    private String username;
    private String password;
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
        // 1. Xác thực username/password từ request
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        // 4. Trả về Token cho client (khớp với LoginResponse của client)
        // Client JavaFX của bạn đang mong đợi 1 trường tên là "token"
        return ResponseEntity.ok(Map.of("token", jwt));
    }
    @Autowired // Cần thiết để lấy thông tin chi tiết User
    private UserService userService1; 

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not logged in"));
        }

        String username = authentication.getName();
        
        UserEntity userEntity = userService1.findByUsername(username); 
        
        if (userEntity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found in database."));
        }
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
    
}