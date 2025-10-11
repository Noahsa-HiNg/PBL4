package com.pbl4.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pbl4.common.model.User;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // Giữ lại API GET /ping
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = Map.of(
            "status", "OK",
            "message", "Hello from PBL4 Server!"
        );
        return ResponseEntity.ok(response);
    }
    
    // Giữ lại API POST /echo
    @PostMapping("/echo")
    public ResponseEntity<User> echoUser(@RequestBody User user) {
        System.out.println("Received user from client: " + user.toString());
        return ResponseEntity.ok(user);
    }
    
    /**
     * BỔ SUNG: API POST mới để nhận một chuỗi bất kỳ.
     * @param message Chuỗi được gửi trong body của request.
     * @return Trả về một chuỗi xác nhận.
     */
    @PostMapping("/echo-string")
    public ResponseEntity<String> echoString(@RequestBody String message) {
        // @RequestBody sẽ lấy toàn bộ nội dung trong body của request và gán vào biến 'message'
        System.out.println("Server received a string: '" + message + "'");
        
        // Trả lại một chuỗi xác nhận, bao gồm cả chuỗi đã nhận được
        return ResponseEntity.ok("Server successfully received: '" + message + "'");
    }
}