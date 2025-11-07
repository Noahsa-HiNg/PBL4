package com.pbl4.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper; // Thêm import
import com.pbl4.server.security.JwtTokenProvider; // Thêm import
import org.springframework.beans.factory.annotation.Autowired; // Thêm import
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    // SỬA LẠI MAPS:
    // Map này lưu các session đã được xác thực
    // Key: username, Value: Session
    private final Map<String, WebSocketSession> sessionsByUsername = new ConcurrentHashMap<>();
    
    // Map này giúp tra cứu ngược để dọn dẹp khi client ngắt kết nối
    // Key: sessionId, Value: username
    private final Map<String, String> userBySessionId = new ConcurrentHashMap<>();

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
            sessionsByUsername.remove(username);
            System.out.println("WebSocket: User " + username + " (Session " + session.getId() + ") đã ngắt kết nối.");
        } else {
            System.out.println("WebSocket: Client " + session.getId() + " (chưa xác thực) đã ngắt kết nối.");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Đây là nơi xử lý tin nhắn TỪ CLIENT GỬI LÊN
        String payload = message.getPayload();
        
        try {
            // 1. Đọc tin nhắn JSON
            Map<String, String> msg = objectMapper.readValue(payload, Map.class);
            String type = msg.get("type");

            // 2. Xử lý tin nhắn "AUTH"
            // Client phải gửi tin này ngay sau khi kết nối
            if ("AUTH".equals(type)) {
                
                // Nếu session này đã xác thực rồi thì bỏ qua
                if (userBySessionId.containsKey(session.getId())) return; 

                String token = msg.get("token");
                if (token != null && tokenProvider.validateToken(token)) {
                    // 3. Token hợp lệ -> Lấy username và LƯU LẠI
                    String username = tokenProvider.getUsernameFromJWT(token);
                    
                    sessionsByUsername.put(username, session);
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
        // 1. Tìm session của user
        WebSocketSession session = sessionsByUsername.get(username);
        
        // 2. Kiểm tra xem họ có đang kết nối không
        if (session != null && session.isOpen()) {
            try {
                // 3. Gửi tin nhắn
                System.out.println("WebSocket: Đang gửi tin cho '" + username + "': " + jsonMessage);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (IOException e) {
                System.err.println("Lỗi khi gửi WebSocket cho " + username + ": " + e.getMessage());
            }
        } else {
            System.out.println("WebSocket: Không tìm thấy session hoặc session đã đóng cho user: " + username);
        }
    }
}