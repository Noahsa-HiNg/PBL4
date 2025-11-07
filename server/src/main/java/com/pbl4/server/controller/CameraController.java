package com.pbl4.server.controller;

import com.pbl4.server.dto.AddCameraRequest;
import com.pbl4.server.dto.CameraDTO;
import com.pbl4.server.dto.UpdateCameraActiveRequest;
import com.pbl4.server.service.CameraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pbl4.common.model.Camera;
import com.pbl4.server.service.CameraService;
import com.pbl4.server.service.UserService; // BỔ SUNG để lấy User ID

import jakarta.persistence.EntityNotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // BỔ SUNG
import org.springframework.security.core.context.SecurityContextHolder; // BỔ SUNG
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @PostMapping("/add")
    public ResponseEntity<?> addCamera(
            @RequestBody AddCameraRequest request,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            CameraDTO newCamera = cameraService.addCamera(request, username);
            // Trả về 201 Created và thông tin camera mới
            return ResponseEntity.status(HttpStatus.CREATED).body(newCamera);
        
        } catch (EntityNotFoundException e) {
            // User hoặc Client không tìm thấy
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            // User không sở hữu client
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) { // <-- BẮT LỖI XUNG ĐỘT
            // Camera (IP+User) đã tồn tại
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Các lỗi khác (ví dụ: lỗi CSDL)
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi server khi thêm camera.", "error", e.getMessage()));
        }
    }
    @PutMapping("/{id}/status") 
    public ResponseEntity<?> updateCameraStatus(
            @PathVariable int id,
            @RequestBody UpdateCameraActiveRequest request,
            Authentication authentication) {
        
        try {
            String username = authentication.getName();
            cameraService.updateCameraActiveStatus(id, request.isActive(), username,request.getMachineId());
            return ResponseEntity.ok(Map.of("message", "Camera " + id + " status updated to " + request.isActive()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Error updating camera status", "error", e.getMessage()));
        }
    }
}