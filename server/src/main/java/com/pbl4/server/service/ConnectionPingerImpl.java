package com.pbl4.server.service;

import org.springframework.stereotype.Service;

/**
 * Lớp thực thi cho ConnectionPinger. 
 * Trong thực tế, logic này sẽ sử dụng WebSockets, MQTT, hoặc HTTP/UDP để giao tiếp 
 * với Client App để yêu cầu phản hồi (Heartbeat Response).
 */
@Service
public class ConnectionPingerImpl implements ConnectionPinger {

    @Override
    public void sendPing(String ipAddress, int clientId) {
//        System.out.println("----------------------------------------------------------------");
//        System.out.println("[PING SENT] Sending active check to Client ID: " + clientId + " at IP: " + ipAddress);
//        System.out.println("Client expected to call PUT /api/clients/ping-response/{id} within 3 minutes.");
//        System.out.println("----------------------------------------------------------------");

        // --- LOGIC GỬI TÍN HIỆU THỰC TẾ ---
        
        // Ví dụ 1: Gửi lệnh qua WebSocket (nếu Client có kết nối WS với Server)
        // WebSocketManager.sendMessage(clientId, "PING_CHECK");

        // Ví dụ 2: Gọi một API UDP hoặc HTTP tới IP/Port của Client
        // httpService.sendPingRequest(ipAddress, clientId);
        
        // Hiện tại, ta chỉ sử dụng System.out để mô phỏng việc gửi tín hiệu.
    }
}