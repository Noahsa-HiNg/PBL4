package com.pbl4.server.service;

import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.repository.ClientRepository;
import com.pbl4.server.repository.CameraRepository; // Cần thiết để tắt camera
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClientMonitorService {

    // --- HẰNG SỐ CẤU HÌNH TRẠNG THÁI VÀ THỜI GIAN ---
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_PING_SENT = "PING_SENT";
    private static final String STATUS_OFFLINE = "OFFLINE";
    
    private static final long SUSPENDED_TIMEOUT_SECONDS = 60; // 1 phút (ACTIVE -> SUSPENDED)
    private static final long PING_RESPONSE_TIMEOUT_SECONDS = 180; // 3 phút cố định chờ phản hồi Ping

    private final ClientRepository clientRepository;
    private final CameraRepository cameraRepository; // Đã thêm
    private final ConnectionPinger connectionPinger;
    
    private final ClientService clientService;


    @Autowired
    public ClientMonitorService(ClientRepository clientRepository, ConnectionPinger connectionPinger, ClientService clientService, Optional<CameraRepository> cameraRepositoryOptional) {
        this.clientRepository = clientRepository;
        this.connectionPinger = connectionPinger;
        this.clientService = clientService;
        // Xử lý Optional cho CameraRepository (đảm bảo không bị lỗi nếu không có @Autowired)
        this.cameraRepository = cameraRepositoryOptional.orElse(null);
    }

    /**
     * Helper: Tính toán mốc thời gian dựa trên số giây trước đó.
     */
    private Timestamp calculateTimestampThreshold(long seconds) {
        LocalDateTime timeThreshold = LocalDateTime.now().minusSeconds(seconds);
        return Timestamp.from(timeThreshold.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Chạy định kỳ mỗi 30 giây để kiểm tra và cập nhật trạng thái Client.
     */
    @Scheduled(fixedRate = 30000) 
    public void checkClientStatusAndPing() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // =========================================================
        // 1. ACTIVE -> SUSPENDED (1 phút không có ảnh)
        // =========================================================
        Timestamp suspendedThreshold = calculateTimestampThreshold(SUSPENDED_TIMEOUT_SECONDS);
        
        List<ClientEntity> toSuspend = clientRepository.findByStatusAndLastImageReceivedBefore(
            STATUS_ACTIVE, suspendedThreshold);
        
        toSuspend.forEach(client -> {
            client.setStatus(STATUS_SUSPENDED);
            System.out.println("Client ID " + client.getId() + " changed to SUSPENDED.");
        });
        clientRepository.saveAll(toSuspend);

        
        // =========================================================
        // 2. SUSPENDED/PING_SENT -> PING_SENT/OFFLINE
        // =========================================================
        
        // Lấy tất cả Clients đang SUSPENDED hoặc PING_SENT
        List<ClientEntity> clientsToCheck = clientRepository.findByStatusIn(List.of(STATUS_SUSPENDED, STATUS_PING_SENT)); 

        for (ClientEntity client : clientsToCheck) {
            
            // --- Xử lý Clients đang chờ phản hồi PING_SENT (Kiểm tra timeout 3 phút) ---
            if (client.getStatus().equals(STATUS_PING_SENT)) {
                Timestamp offlineThreshold = calculateTimestampThreshold(PING_RESPONSE_TIMEOUT_SECONDS);

                // KIỂM TRA LỖI LOGIC: Cần đảm bảo lastPingAttempt đã được đặt
                if (client.getLastPingAttempt() != null && client.getLastPingAttempt().before(offlineThreshold)) {
                    // Đã quá 3 phút (180s) không phản hồi -> OFFLINE
                    client.setStatus(STATUS_OFFLINE);
                    client.setLastPingAttempt(null);
                    clientRepository.save(client);
                    
                    // TẮT CAMERAS
                    if (cameraRepository != null) {
                        cameraRepository.updateAllByClientId(client.getId(), false); 
                    }
                    
                    System.out.println("Client ID " + client.getId() + " PING TIMEOUT. Set to OFFLINE.");
                    continue; 
                }
            }


            // --- Xử lý Clients đang SUSPENDED (Kiểm tra xem có cần Ping không) ---
            if (client.getStatus().equals(STATUS_SUSPENDED)) {
                
                // 2.1. Lấy ngưỡng thời gian Ping ĐỘNG
                long pingIntervalSeconds = clientService.calculateDynamicPingInterval(client.getId());
                Timestamp dynamicThreshold = calculateTimestampThreshold(pingIntervalSeconds);
                
                // 2.2. Kiểm tra nếu đã quá hạn Ping (so với lần nhận ảnh cuối)
                // LƯU Ý: Nếu lastImageReceived là NULL (không nên xảy ra), sẽ có NPE. Giả định nó không NULL.
                boolean isOverdue = client.getLastImageReceived().before(dynamicThreshold);
                
                if (isOverdue) {
                    // Lần đầu hết hạn -> GỬI PING
                    client.setLastPingAttempt(now); 
                    client.setStatus(STATUS_PING_SENT); // Đặt trạng thái chờ phản hồi
                    clientRepository.save(client);
                    
                    connectionPinger.sendPing(client.getIpAddress(), client.getId()); 
                    //System.out.println("Client ID " + client.getId() + " PING SENT. Ngưỡng: " + pingIntervalSeconds + "s.");
                }
            }
        }
    }
}