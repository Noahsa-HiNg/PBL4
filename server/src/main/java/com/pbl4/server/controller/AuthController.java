package com.pbl4.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
// Import UserService và User DTO nếu bạn muốn trả về thông tin chi tiết
// import com.pbl4.server.service.UserService;
// import pbl4.common.model.User;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Inject UserService nếu cần lấy User DTO
    // private final UserService userService;
    // public AuthController(UserService userService) { this.userService = userService; }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not logged in"));
        }

        String username = authentication.getName();
        // Lấy role từ Authorities (nếu UserDetailsServiceImpl đã cung cấp)
        String role = authentication.getAuthorities().stream()
                        .findFirst()
                        .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                        .orElse("UNKNOWN"); // Hoặc giá trị mặc định

        // Tùy chọn: Gọi userService để lấy User DTO đầy đủ
        // User userDto = userService.findByUsername(username);
        // return ResponseEntity.ok(userDto);

        // Chỉ trả về thông tin cơ bản
        return ResponseEntity.ok(Map.of("username", username, "role", role));
    }

    // KHÔNG CẦN VIẾT HÀM XỬ LÝ CHO POST /api/auth/login VÀ POST /api/auth/logout
}