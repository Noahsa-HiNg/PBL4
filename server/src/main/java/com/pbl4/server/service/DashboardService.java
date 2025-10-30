package com.pbl4.server.service;

import com.pbl4.server.repository.UserRepository;
import com.pbl4.server.repository.ClientRepository;
import com.pbl4.server.repository.CameraRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final CameraRepository cameraRepository;

    public DashboardService(UserRepository userRepository, ClientRepository clientRepository, CameraRepository cameraRepository) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.cameraRepository = cameraRepository;
    }

    public Map<String, Long> getAdminMetrics() {
        // Truy vấn tất cả các số liệu cần thiết
        long totalUsers = userRepository.count();
        long totalClients = clientRepository.count();
        long totalCameras = cameraRepository.count();
        
        // Truy vấn số liệu theo trạng thái
        long activeCameras = cameraRepository.countByIsActive(true);
        long inactiveCameras = cameraRepository.countByIsActive(false);

        // Trả về Map (JSON)
        return Map.of(
            "totalUsers", totalUsers,
            "totalClients", totalClients,
            "totalCameras", totalCameras,
            "activeCameras", activeCameras,
            "inactiveCameras", inactiveCameras
        );
    }
    public Map<String, Long> getUserStats(Long userId) {
        // Chuyển đổi Long userId sang int
        int userIdInt = userId.intValue();

        // Đếm Clients thuộc sở hữu
        long totalClients = clientRepository.countByUserId(userIdInt);
        
        // Đếm Cameras thuộc sở hữu (thông qua Client)
        long totalCameras = cameraRepository.countByClientUserId(userIdInt);

        // Trả về Map (JSON)
        return Map.of(
            "totalClients", totalClients,
            "totalCameras", totalCameras
        );
    }
}