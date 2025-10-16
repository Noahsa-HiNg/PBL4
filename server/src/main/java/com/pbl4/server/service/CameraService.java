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