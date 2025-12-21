package com.pbl4.server.service;

import com.pbl4.server.repository.UserRepository;
import com.pbl4.server.repository.ClientRepository;
import com.pbl4.server.repository.ImageRepository;
import com.pbl4.server.dto.DailyImageStats;
import com.pbl4.server.dto.UserRankingDto;
import com.pbl4.server.repository.CameraRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final CameraRepository cameraRepository;
    private final ImageRepository imageRepository;

    public DashboardService(UserRepository userRepository, ClientRepository clientRepository, CameraRepository cameraRepository,ImageRepository imageRepository) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.cameraRepository = cameraRepository;
        this.imageRepository = imageRepository;
    }

    public Map<String, Long> getAdminMetrics() {
        // Truy vấn tất cả các số liệu cần thiết
        long totalUsers = userRepository.count();
        long totalClients = clientRepository.count();
        long totalCameras = cameraRepository.count();
        long totalImages = imageRepository.count(); // Tổng số ảnh hệ thống
        
        long activeCameras = cameraRepository.countByIsActive(true);
        long inactiveCameras = cameraRepository.countByIsActive(false);

        // Sử dụng HashMap để dễ dàng thêm mới
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("totalUsers", totalUsers);
        metrics.put("totalClients", totalClients);
        metrics.put("totalCameras", totalCameras);
        metrics.put("totalImages", totalImages); 
        metrics.put("activeCameras", activeCameras);
        metrics.put("inactiveCameras", inactiveCameras);
        
        return metrics;
    }
    public Map<String, Long> getUserStats(Long userId) {
        int userIdInt = userId.intValue();

        long totalClients = clientRepository.countByUserId(userIdInt);
        long totalCameras = cameraRepository.countByClientUserId(userIdInt);
        long totalImages = imageRepository.countByCameraClientUserId(userIdInt);

        return Map.of(
            "totalClients", totalClients,
            "totalCameras", totalCameras,
            "totalImages", totalImages 
        );
        
    }
    public Map<String, Long> getCameraStats(int cameraId) {
        long totalImages = imageRepository.countByCameraId(cameraId);
        
        return Map.of(
            "cameraId", (long) cameraId,
            "totalImages", totalImages
        );
    }
    public Map<String, Object> getSystemRankings(int limit) {
        Pageable topLimit = PageRequest.of(0, limit);
        List<UserRankingDto> topUsersByImages = userRepository.findTopUsersByImageCount(topLimit);
        List<UserRankingDto> topUsersByCameras = userRepository.findTopUsersByCameraCount(topLimit);
        long totalUserUploadedImages = topUsersByImages.stream()
                .mapToLong(UserRankingDto::getValue)
                .sum(); 

        Map<String, Object> rankings = new HashMap<>();
        rankings.put("topUsersByImages", topUsersByImages);
        rankings.put("topUsersByCameras", topUsersByCameras);
        
        return rankings;
    }
    public List<DailyImageStats> getWeeklyChartData() {
        return imageRepository.findLast7DaysStats();
    }
}