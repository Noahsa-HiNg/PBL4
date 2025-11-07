package com.pbl4.server.service;

import com.pbl4.server.dto.AddCameraRequest;
import com.pbl4.server.dto.CameraDTO;
import com.pbl4.server.entity.CameraEntity;
import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.CameraRepository;
import com.pbl4.server.repository.ClientRepository;
import com.pbl4.server.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import pbl4.common.model.Camera; // DTO

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CameraService {

    private final CameraRepository cameraRepository;
    private final ClientRepository clientRepository; 
    private final UserRepository userRepository;// Cần để tìm Client
    

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
    @Transactional
    public CameraDTO addCamera(AddCameraRequest request, String authUsername) {
        // 1. Xác thực: User (từ token) có sở hữu Client (từ request) không?
        UserEntity user = userRepository.findByUsername(authUsername)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + authUsername));
        
        ClientEntity client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client (ID: " + request.getClientId() + ") not found"));
        
        if (!client.getUser().equals(user)) {
            throw new SecurityException("User " + authUsername + " không sở hữu Client " + request.getClientId());
        }

        // 2. Kiểm tra Xung đột: Camera (vật lý) đã tồn tại chưa?
        // Kiểm tra xem (IP + Username) đã tồn tại ở BẤT KỲ đâu trong bảng Cameras chưa
        Optional<CameraEntity> existingPhysicalCamera = cameraRepository.findByIpAddressAndUsername(
            request.getIpAddress(), request.getUsername()
        );
        Optional<CameraEntity> existingPhysicalCamera2 = cameraRepository.findByIpAddressAndClient_Id(
                request.getIpAddress(), request.getClientId()
            );
        

        if (existingPhysicalCamera.isPresent() || existingPhysicalCamera2.isPresent()) {
            // Nếu tìm thấy -> Báo lỗi xung đột
            throw new IllegalStateException("Xung đột: Camera (IP: " + request.getIpAddress() + 
                    ", User: " + request.getUsername() + ") đã được thêm vào hệ thống.");
        }

        // 3. Tạo mới Camera (vì không xung đột)
        CameraEntity newCamera = new CameraEntity();
        newCamera.setClient(client); // Gán camera cho client này
        newCamera.setCameraName(request.getCameraName());
        newCamera.setIpAddress(request.getIpAddress());
        newCamera.setUsername(request.getUsername());
        newCamera.setPassword(request.getPassword()); // (Nên mã hóa mật khẩu camera nếu có thể)
        newCamera.setOnvifUrl(request.getOnvifUrl());
        newCamera.setActive(false); // Trạng thái ban đầu là offline
        newCamera.setCreatedAt(Timestamp.from(Instant.now()));
        
        CameraEntity savedCamera = cameraRepository.save(newCamera);
        
        // Trả về DTO của camera vừa tạo
        return new CameraDTO(savedCamera); 
    }

}