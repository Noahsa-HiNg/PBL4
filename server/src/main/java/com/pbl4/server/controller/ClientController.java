package com.pbl4.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.server.dto.ClientRegisterRequest;
import com.pbl4.server.dto.ClientRegisterResponse;
import com.pbl4.server.service.ClientService;
import com.pbl4.server.service.UserService;
import com.pbl4.server.websocket.MyWebSocketHandler;

import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;


import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pbl4.common.model.Client;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final UserService userService;
    private final MyWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    public ClientController(ClientService clientService, UserService userService,MyWebSocketHandler webSocketHandler, ObjectMapper objectMapper) {
        this.clientService = clientService;
        this.userService = userService;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }
    @PostMapping("/register")
    public ResponseEntity<ClientRegisterResponse> registerClient(
            @RequestBody ClientRegisterRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
    	String username = authentication.getName();
        String remoteIpAddress = servletRequest.getRemoteAddr();
        ClientRegisterResponse response = clientService.registerOrGetClient(request, username, remoteIpAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof Long) {
             Long userId = (Long) authentication.getPrincipal(); 
             return new ResponseEntity<>(clientService.createClient(client, userId), HttpStatus.CREATED);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

//    @GetMapping
//    public ResponseEntity<List<Client>> getAllClients() {
//        return ResponseEntity.ok(clientService.getAllClients());
//    }

    @GetMapping("/{id}") 
    public ResponseEntity<Client> getClientById(@PathVariable int id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }
        
        Long userId = userService.getUserIdByUsername(username);
        if (userId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); 
        }

        try {
            // Sử dụng phương thức GET ONE đã được lọc theo ID sở hữu
            return ResponseEntity.ok(clientService.getClientById(id, userId));
        } catch (RuntimeException e) {
            // Bắt lỗi "Client not found or access denied"
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); 
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateClient(@PathVariable int id, @RequestBody Client clientDetails) {
        
        // 1. XÁC THỰC VÀ LẤY USER ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token is invalid."));
        }

        Long currentUserId = userService.getUserIdByUsername(username);

        if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not found in context."));
        }

        try {
            // 2. GỌI SERVICE CẬP NHẬT (Service đã kiểm tra quyền sở hữu)
            Client updatedClient = clientService.updateClient(id, clientDetails, currentUserId);
            
            // 3. THÔNG BÁO WEBSOCKET ĐẾN CLIENT APP
            
            String ownerUsername = clientService.getUsernameByClientId(id); 

            if (webSocketHandler != null && ownerUsername != null) {
                
                // Tạo JSON Payload (Gửi toàn bộ DTO Client đã cập nhật)
                String dtoJson = objectMapper.writeValueAsString(updatedClient);
                String jsonMessage = String.format(
                    "{\"type\": \"CONFIG_UPDATE\", \"clientId\": %d, \"config\": %s}",
                    id, dtoJson
                );
                
                // GỬI TIN ĐẾN TẤT CẢ SESSIONS CỦA USER ĐÓ
                webSocketHandler.sendMessageToUser(ownerUsername, jsonMessage);
            }
            return ResponseEntity.ok(updatedClient);
            
        } catch (EntityNotFoundException e) {
            // Bắt lỗi "Access Denied" hoặc "Not Found" từ Service
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (JsonProcessingException e) {
            // Bắt lỗi khi tạo JSON cho WebSocket
            System.err.println("Client updated, but WebSocket notification failed: " + e.getMessage());
            // Trả về thành công, vì Client đã được cập nhật
            return ResponseEntity.ok(Map.of("message", "Client updated, but notification failed."));
        } catch (Exception e) {
            // Bắt các lỗi chung khác
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(@PathVariable int id) {
        
        // 1. Lấy User ID từ Token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Long currentUserId = userService.getUserIdByUsername(username);

        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated."));
        }

        try {
            // 2. Gọi Service
            clientService.deleteClient(id, currentUserId);
            
            // 3. Trả về thành công
            return ResponseEntity.noContent().build();
            
        } catch (EntityNotFoundException e) {
            // Bắt lỗi nếu User không sở hữu Client
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Bắt lỗi nếu xóa file I/O thất bại
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error deleting client: " + e.getMessage()));
        }
    }
    @GetMapping // API lấy danh sách Clients (chỉ của người dùng đang đăng nhập)
    public ResponseEntity<List<Client>> getClientsByUserId() {
        
        // 1. LẤY USER ID TỪ SECURITY CONTEXT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }

        Long userId = userService.getUserIdByUsername(username);
        if (userId == null) {
             // User được xác thực nhưng không tồn tại trong DB (lỗi cấu hình)
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); 
        }

        // 2. GỌI SERVICE đã được lọc
        List<Client> clients = clientService.getClientsByUserId(userId);
        
        return ResponseEntity.ok(clients);
    }
    @PutMapping("/ping-response/{clientId}")
    public ResponseEntity<?> recordPingResponse(@PathVariable int clientId) {
        // LƯU Ý: Thêm logic xác thực Token/Client ID
        clientService.clientPingResponded(clientId);
        return ResponseEntity.ok().build();
    }
    @PutMapping("/logout/{clientId}")
    public ResponseEntity<?> clientLogout(@PathVariable int clientId) {
        // LƯU Ý: Thêm logic xác thực Token/Client ID
        clientService.setClientOfflineAndTurnOffCameras(clientId);
        return ResponseEntity.ok().build();
    }
}