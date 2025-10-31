package com.pbl4.server.controller;

import com.pbl4.server.service.ClientService;
import com.pbl4.server.service.UserService;

import org.springframework.security.core.context.SecurityContextHolder;

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

    @GetMapping("/{id}") // Sửa đổi API GET ONE để đảm bảo phân quyền
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
    public ResponseEntity<Client> updateClient(@PathVariable int id, @RequestBody Client clientDetails) {
        return ResponseEntity.ok(clientService.updateClient(id, clientDetails));
    }

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
    
}