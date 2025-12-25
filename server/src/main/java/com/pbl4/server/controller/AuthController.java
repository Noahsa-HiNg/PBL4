package com.pbl4.server.controller;

import com.pbl4.server.dto.ChangePasswordRequest;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.security.JwtTokenProvider; 
import pbl4.common.model.User;
import com.pbl4.server.service.UserService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
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
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
   
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        return ResponseEntity.ok(Map.of("token", jwt));
    }
    @Autowired 
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
        response.put("id", userEntity.getId()); 
        response.put("username", username);
        response.put("email", userEntity.getEmail());
        response.put("role", role);

        return ResponseEntity.ok(response);
    }
    @Autowired // <-- Bổ sung UserService
    private UserService userService;
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User userDto) { // Dùng DTO 'User' của common
        try {
            UserEntity registeredUser = userService.registerUser(userDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt tài khoản."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
    @GetMapping("/verify-email")
    public void verifyEmail(@RequestParam("token") String token, HttpServletResponse response) throws IOException {
        try {
            userService.verifyEmail(token);
            response.sendRedirect("/verify_success.html"); 

        } catch (RuntimeException e) {
            response.sendRedirect("/login.html?error=" + e.getMessage());
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            userService.createPasswordResetToken(email);
            return ResponseEntity.ok(Map.of("message", "Link đặt lại mật khẩu đã được gửi đến email của bạn."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String token = body.get("token");
            String newPassword = body.get("newPassword");
            
            
            userService.resetPassword(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Mật khẩu đã được đặt lại thành công."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
       
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            userService.changePassword(username, request);

            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công!"));

        } catch (BadCredentialsException e) {
            // Trả về lỗi 400 nếu sai mật khẩu cũ
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi hệ thống"));
        }
    }
    @PostMapping("/verify-email-change") // Hoặc GetMapping tùy bạn
    public ResponseEntity<?> verifyEmailChange(@RequestParam String token) {
        try {
            // Gọi hàm verifyEmailChange (Hàm này check bảng email_change_tokens)
            userService.verifyEmailChange(token); 
            return ResponseEntity.ok(Map.of("message", "Đổi email thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}