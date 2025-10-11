package com.pbl4.server.controller;

import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService; // Đây là service bạn sẽ sử dụng để xác thực người dùng

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // 1. Xác thực người dùng
        UserEntity user = userService.authenticate(username, password);

        if (user != null) {
            // 2. Tạo JWT Token
            String jwtToken = userService.generateJwtToken(user); // Giả định service có phương thức này

            // 3. Trả về Response thành công
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwtToken);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } else {
            // 4. Trả về Response lỗi nếu xác thực thất bại
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}