package com.pbl4.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.CameraRepository;
import com.pbl4.server.repository.ClientRepository;
import com.pbl4.server.repository.UserRepository;
import com.pbl4.server.security.JwtTokenProvider;
import com.pbl4.server.service.ClientService;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Collections;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    // --- CHỈ CÒN 2 TRẠNG THÁI ---
    private static final String STATUS_ONLINE = "ACTIVE"; 
    private static final String STATUS_OFFLINE = "OFFLINE";
    @Autowired
    private UserRepository userRepository;
    private final Map<String, Set<WebSocketSession>> sessionsByUsername = new ConcurrentHashMap<>();
    //private final Map<String, WebSocketSession> appSessionByUsername = new ConcurrentHashMap<>();
    private final Map<String, String> userBySessionId = new ConcurrentHashMap<>();
    private final Map<String, Integer> clientIdBySessionId = new ConcurrentHashMap<>();
    
    @Autowired
    private ClientService clientService;
    
    private final ClientRepository clientRepository;
    private final CameraRepository cameraRepository;
    
    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    public MyWebSocketHandler(ClientRepository clientRepository, CameraRepository cameraRepository) {
        this.clientRepository = clientRepository;
        this.cameraRepository = cameraRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket: Session " + session.getId() + " đã kết nối. Đang chờ xác thực...");
    }

    @Override
    @Transactional
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        
       
        Integer clientId = clientIdBySessionId.remove(sessionId);
        
        if (clientId != null) {

            System.out.println("WebSocket: APP Client (ID: " + clientId + ") đã ngắt kết nối.");
            clientRepository.findById(clientId).ifPresent(client -> {
                client.setStatus(STATUS_OFFLINE);
                clientRepository.save(client);
                cameraRepository.updateAllByClientId(client.getId(), false);
                System.out.println("WebSocket: Đã set Client ID " + clientId + " sang OFFLINE.");
            });
        }
        String username = userBySessionId.remove(sessionId);
        if (username != null) {
            Set<WebSocketSession> userSessions = sessionsByUsername.get(username);
            if (userSessions != null) {
                userSessions.remove(session);
                if (userSessions.isEmpty()) {
                    sessionsByUsername.remove(username);
                }
            }
            if (clientId == null) {
                 System.out.println("WebSocket: WEB Client (User: " + username + ") đã ngắt kết nối.");
            }
        }
    }
    @Override
    @Transactional // Giữ Transactional ở đây là ổn
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        try {
            // Dùng Map<String, Object> để linh hoạt
            Map<String, Object> msg = objectMapper.readValue(payload, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            String type = (String) msg.get("type");

            if ("AUTH".equals(type)) {
                if (userBySessionId.containsKey(session.getId())) return; 

                String token = (String) msg.get("token");
                if (token != null && tokenProvider.validateToken(token)) {
                    String username = tokenProvider.getUsernameFromJWT(token);
                    
                    // --- SỬA LOGIC TÌM KIẾM ---
                    
                    // 1. Lưu session vào các map chung
                    Set<WebSocketSession> userSessions = sessionsByUsername.computeIfAbsent(
                            username, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())
                        );
                    userSessions.add(session);
                    userBySessionId.put(session.getId(), username);

                    String clientType = (String) msg.get("clientType");
                    
                    if ("APP".equals(clientType)) {
                        String machineId = (String) msg.get("machineId");
                        if (machineId == null) {
                            session.close(CloseStatus.POLICY_VIOLATION.withReason("APP Client phải gửi machineId"));
                            return;
                        }

                        // 2. Tìm UserEntity
                        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
                        if (userOpt.isEmpty()) {
                            session.close(CloseStatus.POLICY_VIOLATION.withReason("User không tồn tại"));
                            return;
                        }

                        // 3. Tìm ClientEntity CỤ THỂ
                        Optional<ClientEntity> clientOpt = clientRepository.findByMachineIdAndUser(machineId, userOpt.get());

                        if (clientOpt.isPresent()) {
                            ClientEntity client = clientOpt.get();
                            client.setStatus(STATUS_ONLINE);
                            clientRepository.save(client);

                            // 4. Lưu session này là của client_id nào
                            clientIdBySessionId.put(session.getId(), client.getId());
                            
                            System.out.println("WebSocket: APP Client '" + client.getClientName() + "' (ID: " + client.getId() + ") đã xác thực và set ONLINE.");
                        } else {
                            System.err.println("LỖI: APP Client (user: " + username + ") đã xác thực nhưng không tìm thấy Client với machineId: " + machineId);
                        }

                    } else {
                        System.out.println("WebSocket: WEB Client '" + username + "' đã xác thực.");
                    }
                    
                    session.sendMessage(new TextMessage("{\"type\": \"AUTH_SUCCESS\"}"));
                
                } else {
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Token không hợp lệ"));
                }
            } 
        } catch (Exception e) {
            // Lỗi này chính là lỗi bạn gặp (IncorrectResultSizeDataAccessException)
            // Giờ nó sẽ không xảy ra nữa, nhưng chúng ta vẫn bắt các lỗi khác
            System.err.println("Lỗi xử lý logic WebSocket cho type 'AUTH': " + e.getMessage());
            e.printStackTrace(); 
            // Không đóng kết nối vội, có thể chỉ là lỗi parse
        }
    }

    public void sendMessageToUser(String username, String jsonMessage) {
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