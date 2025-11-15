package com.pbl4.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.server.dto.AddCameraRequest;
import com.pbl4.server.dto.CameraDTO;
import com.pbl4.server.dto.UpdateCameraActiveRequest;
import com.pbl4.server.service.CameraService;
import com.pbl4.server.service.CameraService.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pbl4.common.model.Camera;
import com.pbl4.server.service.CameraService;
import com.pbl4.server.service.UserService; // BỔ SUNG để lấy User ID
import com.pbl4.server.websocket.MyWebSocketHandler;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // BỔ SUNG
import org.springframework.security.core.context.SecurityContextHolder; // BỔ SUNG
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.pbl4.server.service.DashboardService;
@RestController
@RequestMapping("/api/cameras")
public class CameraController {

    private final CameraService cameraService;
    private final UserService userService;
    @Autowired
    private MyWebSocketHandler webSocketHandler;
    
    @Autowired
    private ObjectMapper objectMapper;
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
        if (clientId != null) {
            cameras = cameraService.getCamerasByClientId(clientId, userId); 
        } else {
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
            return ResponseEntity.status(HttpStatus.CREATED).body(newCamera);
        
        } catch (EntityNotFoundException e) {
         
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
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
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCamera(@PathVariable Integer id, @RequestBody CameraDTO cameraDTO) {
        try {

            CameraDTO updatedCamera = cameraService.updateCamera(id, cameraDTO);

            return ResponseEntity.ok(updatedCamera);
            
        } catch (ResourceNotFoundException e) { 
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi server: " + e.getMessage());
        }
    }
//    @DeleteMapping("/{id}")
//    public ResponseEntity<?> deleteCamera(@PathVariable Integer id) {
//        try {
//        	System.out.println("kiem tra 1");
//            // 1. Gọi service để thực hiện logic (Sẽ tạo ở bước 4)
//            cameraService.deleteCamera(id);
//            
//            System.out.println("kiem tra 2");
//            
//            // 2. Trả về 200 OK (không cần nội dung)
//            return ResponseEntity.ok().build();
//            
//        } catch (ResourceNotFoundException e) {
//            // 3. Trả về 404 nếu không tìm thấy camera
//            return ResponseEntity.status(404).body(e.getMessage());
//        } catch (Exception e) {
//            // 4. Trả về 500 nếu có lỗi server khác
//            return ResponseEntity.status(500).body("Lỗi server: " + e.getMessage());
//        }
//    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCamera(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Long currentUserId = userService.getUserIdByUsername(username);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated."));
        }

        String usernameToNotify; // Biến để giữ username

        try {
            // --- Logic CSDL ---
            // GỌI HÀM SERVICE ĐÃ SỬA:
            usernameToNotify = cameraService.deleteCamera(id, currentUserId);
            // Nếu dòng trên chạy xong, CSDL ĐÃ ĐƯỢC COMMIT (xóa thành công)
            
        } catch (EntityNotFoundException e) {
            // Lỗi quyền hoặc không tìm thấy
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Bất kỳ lỗi CSDL nào khác (ví dụ: IOException từ imageService)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error during database operation: " + e.getMessage()));
        }

        // --- Logic Mạng (WebSocket) ---
        // Chỉ chạy nếu CSDL đã xóa thành công
//        if (usernameToNotify != null) {
//            try {
//                Map<String, Object> msg = Map.of(
//                    "type", "CAMERA_DELETED",
//                    "id", id
//                );
//                String json = objectMapper.writeValueAsString(msg);
//                webSocketHandler.sendMessageToUser(usernameToNotify, json);
//            
//            } catch (Exception wsError) {
//                // Lỗi WebSocket không làm ảnh hưởng đến kết quả DELETE
//                System.err.println("Database delete OK, but WebSocket send failed: " + wsError.getMessage());
//            }
//        }
        
        // Trả về 204 No Content (Xóa thành công)
        return ResponseEntity.noContent().build();
    }
}