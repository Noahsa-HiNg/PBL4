package com.pbl4.server.service;

import com.pbl4.server.entity.CameraEntity;
import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.repository.CameraRepository;
import com.pbl4.server.repository.ClientRepository;
import org.springframework.stereotype.Service;
import pbl4.common.model.Camera; // DTO

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CameraService {

    private final CameraRepository cameraRepository;
    private final ClientRepository clientRepository; // Cần để tìm Client

    public CameraService(CameraRepository cameraRepository, ClientRepository clientRepository) {
        this.cameraRepository = cameraRepository;
        this.clientRepository = clientRepository;
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
        
        // 1. Kiểm tra quyền sở hữu (Security check)
        // Lý tưởng: Bạn nên viết truy vấn kép: findByClientIdAndClientUserId(clientId, currentUserId)
        
        // Giả sử bạn sử dụng truy vấn kép đó để đảm bảo người dùng chỉ xem được camera của client họ sở hữu
        // List<CameraEntity> entities = cameraRepository.findByClientIdAndClientUserId(clientId, currentUserId.intValue());
        
        // Nếu không có truy vấn kép, hãy dùng truy vấn cũ (nhưng nên kiểm tra quyền sở hữu ở tầng Service)
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
}