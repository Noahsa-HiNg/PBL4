package com.pbl4.server.service;

import com.pbl4.server.entity.CameraEntity;
import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.CameraRepository;
import com.pbl4.server.repository.ClientRepository;
import com.pbl4.server.repository.ImageRepository;
import com.pbl4.server.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pbl4.common.model.Camera; // DTO

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CameraService {

    private final CameraRepository cameraRepository;
    private final ClientRepository clientRepository; 
    private final UserRepository userRepository;// Cần để tìm Client
    @Autowired
    private ImageRepository imageRepository;

    public CameraService(CameraRepository cameraRepository,UserRepository userRepository, ClientRepository clientRepository) {
        this.cameraRepository = cameraRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }
    
    public Camera createCamera(Camera cameraDto) {
        // Tìm ClientEntity tương ứng
        ClientEntity clientEntity = clientRepository.findById(cameraDto.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        
        CameraEntity cameraEntity = toEntity(cameraDto);
        cameraEntity.setClient(clientEntity); // Thiết lập mối quan hệ
        
        CameraEntity savedEntity = cameraRepository.save(cameraEntity);
        return toDto(savedEntity);
    }
    public List<Camera> getCamerasByUserId(Long userId) {
        // Chuyển đổi Long userId sang int
        int userIdInt = userId.intValue();
        
        // Gọi phương thức Repository đã lọc theo User ID
        List<CameraEntity> entities = cameraRepository.findByClientUserId(userIdInt);
        
        // Chuyển đổi Entity sang DTO và trả về
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
public List<Camera> getCamerasByClientId(int clientId, Long currentUserId) {
        
        List<CameraEntity> entities = cameraRepository.findByClientId(clientId);
        
        // LƯU Ý BẢO MẬT: Nếu bạn không dùng truy vấn kép, bạn phải kiểm tra trong Service 
        // xem Client này có thuộc về currentUserId hay không.
        
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    public List<Camera> getAllCameras() {
        return cameraRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }
    
    // ... (Thêm các hàm getById, update, delete tương tự như ClientService)

    // --- Helper Methods for Mapping ---
    private Camera toDto(CameraEntity entity) {
        Camera dto = new Camera();
        dto.setId(entity.getId());
        dto.setCameraName(entity.getCameraName());
        if (entity.getClient() != null) {
            dto.setClientId(entity.getClient().getId());
        }
        
        // ... sao chép các trường khác
        return dto;
    }
    
    private CameraEntity toEntity(Camera dto) {
        CameraEntity entity = new CameraEntity();
        entity.setCameraName(dto.getCameraName());
        // ... sao chép các trường khác
        return entity;
    }
    @Transactional
    public void updateCameraActiveStatus(int cameraId, boolean active, String username, String machineId) { // <-- SỬA CHỮ KÝ HÀM
        // 1. Tìm User (từ token)
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        
        // 2. Tìm Client (dùng cả username và machineId để xác thực)
        ClientEntity client = clientRepository.findByMachineIdAndUser(machineId, user)
                .orElseThrow(() -> new SecurityException("Client " + machineId + " không thuộc về user " + username));

        // 3. Tìm Camera
        CameraEntity camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Camera not found: " + cameraId));

        // 4. Kiểm tra Quyền sở hữu (chính xác hơn)
        // (Kiểm tra xem camera này có thuộc VỀ ĐÚNG client vừa xác thực không)
        if (camera.getClient() == null || !camera.getClient().equals(client)) {
            throw new SecurityException("User " + username + " (Client " + machineId + ") không có quyền chỉnh sửa camera ID " + cameraId);
        }

        // 5. Cập nhật trạng thái
        camera.setActive(active); // Cập nhật cột is_active
        cameraRepository.save(camera);
        System.out.println("Cập nhật trạng thái Camera ID " + camera.getId() + " (is_active) thành: " + active);
    }

}