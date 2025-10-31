package com.pbl4.server.service;

public interface ConnectionPinger {

    /**
     * Gửi tín hiệu Ping/Kiểm tra kết nối tới Client.
     * @param ipAddress Địa chỉ IP của Client (hoặc địa chỉ giao tiếp khác).
     * @param clientId ID của Client để Server có thể theo dõi phản hồi.
     */
    void sendPing(String ipAddress, int clientId);
    
    // Lưu ý: Phương thức này chỉ gửi tín hiệu. Server sẽ chờ Client phản hồi qua API /ping-response/{id}.
}