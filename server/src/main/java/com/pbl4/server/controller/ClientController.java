package com.pbl4.server.controller;

import com.pbl4.server.service.ClientService;
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

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
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

    @GetMapping
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable int id) {
        return ResponseEntity.ok(clientService.getClientById(id));
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
}