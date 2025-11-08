package com.pbl4.server.controller;

import com.pbl4.server.dto.ClientRegisterRequest;
import com.pbl4.server.dto.ClientRegisterResponse;
import com.pbl4.server.service.ClientService;
import com.pbl4.server.service.UserService;

import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;


import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pbl4.common.model.Client;

import java.util.List;
import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final UserService userService;

    public ClientController(ClientService clientService, UserService userService) {
        this.clientService = clientService;
        this.userService = userService;
    }
    @PostMapping("/register")
    public ResponseEntity<ClientRegisterResponse> registerClient(
            @RequestBody ClientRegisterRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {

        // 1. Lấy username của user đang đăng nhập từ token
    	String username = authentication.getName();

        // 2. Lấy địa chỉ IP thật của máy client đang gọi API
        String remoteIpAddress = servletRequest.getRemoteAddr();

        // 3. Gọi Service để xử lý logic nghiệp vụ
        ClientRegisterResponse response = clientService.registerOrGetClient(request, username, remoteIpAddress);

        // 4. Trả về kết quả cho client
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() instanceof Long) {
             Long userId = (Long) authentication.getPrincipal(); 
             
             // 2. Gọi Service với userId
             return new ResponseEntity<>(clientService.createClient(client, userId), HttpStatus.CREATED);
        }
        
        // Xử lý lỗi nếu user không được xác thực hoặc principal không phải ID
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

//    @PutMapping("/{id}")
//    public ResponseEntity<Client> updateClient(@PathVariable int id, @RequestBody Client clientDetails) {
//        
//        // 1. XÁC THỰC VÀ LẤY USER ID
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        String username = authentication.getName();
//        
//        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(username)) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
//        }
//
//        Long currentUserId = userService.getUserIdByUsername(username);
//
//        if (currentUserId == null) {
//             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null); 
//        }
//
//        try {
//            // 2. GỌI SERVICE CẬP NHẬT (Service phải kiểm tra quyền sở hữu)
//            // Truyền ID người dùng đang đăng nhập vào Service để kiểm tra quyền
//            Client updatedClient = clientService.updateClient(id, clientDetails, currentUserId);
//            
//            
//            // 3. THÔNG BÁO WEBSOCKET ĐẾN CLIENT APP
//            if (webSocketHandler != null) {
//                // Lấy username sở hữu Client (để gửi tin)
//                String ownerUsername = clientService.getUsernameByClientId(id); 
//
//                // Chuẩn bị JSON Payload (gửi lại Client DTO đã cập nhật)
//                String jsonMessage = String.format(
//                    "{\"type\": \"CLIENT_CONFIG_UPDATE\", \"clientId\": %d, \"config\": %s}",
//                    id, // Client ID
//                    updatedClient.toString() // Cần ObjectMapper an toàn hơn
//                );
//                
//                // Gửi tin nhắn đến người dùng sở hữu Client này
//                if (ownerUsername != null) {
//                    webSocketHandler.sendMessageToUser(ownerUsername, jsonMessage);
//                }
//            }
//
//            return ResponseEntity.ok(updatedClient);
//            
//        } catch (SecurityException e) {
//            // Lỗi phân quyền (User cố cập nhật Client không phải của mình)
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // 403 FORBIDDEN
//        } catch (RuntimeException e) {
//            // Lỗi Client không tìm thấy, hoặc lỗi khác
//            System.err.println("Error updating client: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); 
//        }
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable int id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
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