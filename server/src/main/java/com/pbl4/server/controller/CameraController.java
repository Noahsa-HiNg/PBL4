package com.pbl4.server.controller;

import com.pbl4.server.service.CameraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pbl4.common.model.Camera;
import com.pbl4.server.service.CameraService;
import com.pbl4.server.service.UserService; // BỔ SUNG để lấy User ID
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // BỔ SUNG
import org.springframework.security.core.context.SecurityContextHolder; // BỔ SUNG
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cameras")
public class CameraController {

    private final CameraService cameraService;
    private final UserService userService;

    public CameraController(CameraService cameraService, UserService userService) {
        this.cameraService = cameraService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Camera> createCamera(@RequestBody Camera camera) {
        return new ResponseEntity<>(cameraService.createCamera(camera), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Camera>> getAllCameras(
            @RequestParam(required = false) Integer clientId) {
        
        // 1. LẤY THÔNG TIN USER ĐANG ĐĂNG NHẬP
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }

        Long userId = userService.getUserIdByUsername(username);
        if (userId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        List<Camera> cameras;
        
        // 2. PHÂN LUỒNG LỌC
        if (clientId != null) {
            // Lọc theo Client ID (chỉ những Client thuộc sở hữu của User này)
            // Bạn cần sử dụng phương thức getCamerasByClientId() đã sửa đổi trong Service
            cameras = cameraService.getCamerasByClientId(clientId, userId); 
        } else {
            // Lọc mặc định: Lấy TẤT CẢ Camera mà User đó sở hữu
            cameras = cameraService.getCamerasByUserId(userId);
        }

        return ResponseEntity.ok(cameras);
    }

    // ... (Thêm các endpoint getById, update, delete tương tự ClientController)
}