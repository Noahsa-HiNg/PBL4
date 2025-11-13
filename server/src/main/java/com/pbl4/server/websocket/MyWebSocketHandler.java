package com.pbl4.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper; // Thêm import
import com.pbl4.server.security.JwtTokenProvider; // Thêm import
import com.pbl4.server.service.ClientService;

import org.springframework.beans.factory.annotation.Autowired; // Thêm import
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set; // <-- Import Set
import java.util.Collections;
@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    
	private final Map<String, Set<WebSocketSession>> sessionsByUsername = new ConcurrentHashMap<>();
    private final Map<String, String> userBySessionId = new ConcurrentHashMap<>();
    @Autowired
    private ClientService clientService;
    // === INJECT CÁC DEPENDENCY CẦN THIẾT ===
    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper; // Để đọc JSON từ client

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Khi client vừa kết nối, chúng ta CHƯA biết họ là ai.
        // Chỉ ghi log và chờ client gửi tin nhắn xác thực.
        System.out.println("WebSocket: Client " + session.getId() + " đã kết nối. Đang chờ xác thực...");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Client ngắt kết nối, dọn dẹp cả 2 map
    	String username = userBySessionId.remove(session.getId());
        if (username != null) {
            Set<WebSocketSession> userSessions = sessionsByUsername.get(username);
            if (userSessions != null) {
                // Xóa session cụ thể này khỏi Set
                userSessions.remove(session); 
                // Nếu đây là session cuối cùng, xóa cả key username
                if (userSessions.isEmpty()) {
                    sessionsByUsername.remove(username);
                }
            }
            System.out.println("WebSocket: User " + username + " (Session " + session.getId() + ") đã ngắt kết nối.");
        }else {
            System.out.println("WebSocket: Client " + session.getId() + " (chưa xác thực) đã ngắt kết nối.");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        try {
            // 1. Đọc tin nhắn JSON
            Map<String, String> msg = objectMapper.readValue(payload, Map.class);
            String type = msg.get("type");
            if ("AUTH".equals(type)) {
                
                // Nếu session này đã xác thực rồi thì bỏ qua
                if (userBySessionId.containsKey(session.getId())) return; 

                String token = msg.get("token");
                if (token != null && tokenProvider.validateToken(token)) {
                    // 3. Token hợp lệ -> Lấy username và LƯU LẠI
                    String username = tokenProvider.getUsernameFromJWT(token);
                    
                    Set<WebSocketSession> userSessions = sessionsByUsername.computeIfAbsent(
                            username, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())
                        );
                    userSessions.add(session);
                    userBySessionId.put(session.getId(), username);
                    
                    System.out.println("WebSocket: User '" + username + "' đã xác thực thành công.");
                    session.sendMessage(new TextMessage("{\"type\": \"AUTH_SUCCESS\"}"));
                } else {
                    // Token không hợp lệ -> Đóng kết nối
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Token không hợp lệ"));
                }
            } else {
                // Xử lý các tin nhắn khác nếu cần
                System.out.println("WebSocket: Nhận được tin nhắn khác từ " + userBySessionId.get(session.getId()) + ": " + payload);
            }

        } catch (Exception e) {
            System.err.println("Lỗi xử lý tin nhắn WebSocket: " + e.getMessage());
            session.close(CloseStatus.BAD_DATA.withReason("Tin nhắn không đúng định dạng JSON"));
        }
    }

    /**
     * Hàm quan trọng: Gửi tin nhắn TỪ Server XUỐNG một user cụ thể
     * HÀM NÀY BÂY GIỜ ĐÃ CHẠY ĐÚNG!
     */
    public void sendMessageToUser(String username, String jsonMessage) {
        // 1. Lấy TẤT CẢ sessions của user
        Set<WebSocketSession> userSessions = sessionsByUsername.get(username);
        
        if (userSessions == null || userSessions.isEmpty()) {
            System.out.println("WebSocket: Không tìm thấy session nào đang hoạt động cho user: " + username);
            return;
        }
        for (WebSocketSession session : userSessions) {
            if (session.isOpen()) {
                try {
                    System.out.println("WebSocket: Đang gửi tin cho '" + username + "' (Session: " + session.getId() + ")");
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    System.err.println("Lỗi khi gửi WebSocket cho " + username + " (Session: " + session.getId() + "): " + e.getMessage());
                }
            }
        }
    }
    public void sendMessageToUserByClientId(int clientId, String jsonMessage) {
        String username = clientService.getUsernameByClientId(clientId); 
        
        if (username != null) {
            sendMessageToUser(username, jsonMessage); 
        }
    }
    public boolean isUserConnected(String username) {
        Set<WebSocketSession> userSessions = sessionsByUsername.get(username);

        if (userSessions == null || userSessions.isEmpty()) {
            return false; 
        }
        for (WebSocketSession session : userSessions) {
            if (session.isOpen()) {
                return true; 
            }
        }

        return false;
    }
}