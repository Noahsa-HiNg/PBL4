package com.pbl4.server.config;

import com.pbl4.server.websocket.MyWebSocketHandler; // Bạn sẽ tạo cái này ở Bước 3
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket // Bật tính năng WebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MyWebSocketHandler myWebSocketHandler; // Inject Handler của bạn

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myWebSocketHandler, "/ws/updates") // Đăng ký handler tại URL này
                .setAllowedOrigins("http://127.0.0.1:5500"); // Chỉ cho phép client này kết nối
    }
}