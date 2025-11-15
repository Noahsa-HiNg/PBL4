package com.pbl4.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.server.dto.AddCameraRequest;
import com.pbl4.server.dto.CameraDTO;
import com.pbl4.server.entity.CameraEntity;
import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.CameraRepository;
import com.pbl4.server.repository.ClientRepository;
import com.pbl4.server.repository.ImageRepository;
import com.pbl4.server.repository.UserRepository;
import com.pbl4.server.websocket.MyWebSocketHandler;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pbl4.common.model.Camera; // DTO

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CameraService {

    private final CameraRepository cameraRepository;
    private final ClientRepository clientRepository; 
    private final UserRepository userRepository;
    
    private final ImageService imageService;
    @Autowired
    private ImageRepository imageRepository;
    

    @Autowired
    private MyWebSocketHandler webSocketHandler; // Handler WebSocket

    @Autowired
    private ObjectMapper objectMapper; // Chuyển đổi JSON

    public CameraService(CameraRepository cameraRepository,UserRepository userRepository, ClientRepository clientRepository,ImageService imageService) {
        this.cameraRepository = cameraRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.imageService = imageService;
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
    
    @Transactional
    public Camera getCameraById(int cameraId) {
        CameraEntity cameraEntity = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Camera not found with id: " + cameraId));
        return toDto(cameraEntity);
    }
    private Camera toDto(CameraEntity entity) {
        Camera dto = new Camera();
        dto.setId(entity.getId());
        dto.setCameraName(entity.getCameraName());
        if (entity.getClient() != null) {
            dto.setClientId(entity.getClient().getId());
        }
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
    @Transactional // Đảm bảo tất cả các thao tác DB là một giao dịch
    public CameraDTO updateCamera(Integer id, CameraDTO cameraDTO) throws Exception {
        
        // 1. Tìm Camera cũ trong Database (Sử dụng JpaRepository)
        CameraEntity existingCamera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy camera với ID: " + id));
        existingCamera.setCameraName(cameraDTO.getCameraName());
        existingCamera.setIpAddress(cameraDTO.getIpAddress());
        existingCamera.setUsername(cameraDTO.getUsername());
        existingCamera.setPassword(cameraDTO.getPassword());
        existingCamera.setOnvifUrl(cameraDTO.getOnvifUrl());
        CameraEntity savedCamera = cameraRepository.save(existingCamera);

        // 4. GỬI THÔNG BÁO WEBSOCKET
        // Tạo nội dung tin nhắn
        Map<String, Object> messagePayload = new HashMap<>();
        messagePayload.put("type", "CAMERA_UPDATED");
        messagePayload.put("camera", convertToDto(savedCamera)); // Gửi DTO đã cập nhật
        
        String jsonMessage = objectMapper.writeValueAsString(messagePayload);

        // Lấy "username" để gửi
        // Dựa trên repository, cấu trúc của bạn là:
        // CameraEntity -> ClientEntity -> UserEntity
        
        String username = null;
        if (savedCamera.getClient() != null && savedCamera.getClient().getUser() != null) {
            // Giả sử UserEntity mới là nơi chứa username
            username = savedCamera.getClient().getUser().getUsername(); // <-- ĐIỀU CHỈNH
        } else {
             throw new Exception("Không thể gửi WebSocket: Camera " + id + " không có Client hoặc User liên kết.");
        }
        
        if (username != null) {
            webSocketHandler.sendMessageToUser(username, jsonMessage);
        } else {
            System.err.println("Không thể gửi WebSocket: User của Camera ID " + id + " không có username.");
        }

        // 5. Trả về DTO cho Controller
        return convertToDto(savedCamera);
    }
    
    private CameraDTO convertToDto(CameraEntity entity) {
        CameraDTO dto = new CameraDTO();
        dto.setId(entity.getId());
        dto.setCameraName(entity.getCameraName());
        dto.setIpAddress(entity.getIpAddress());
        dto.setUsername(entity.getUsername());
        // QUAN TRỌNG: Không bao giờ gửi mật khẩu về client
        dto.setPassword(entity.getPassword()); // hoặc một chuỗi "*****"
        dto.setUrl(entity.getOnvifUrl());
        // ...
        return dto;
    }
    
    /**
     * Lớp Exception tùy chỉnh để trả về 404
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }
//    @Transactional
//    public void deleteCamera(Integer id) throws Exception {
//        CameraEntity camera = cameraRepository.findById(id)
//            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy camera với ID: " + id));
//
//        String username = null;
//        if (camera.getClient() != null && camera.getClient().getUser() != null) {
//            username = camera.getClient().getUser().getUsername();
//        }
//
//        // Xóa các ảnh liên quan trước (nếu có)
////        imageRepository.deleteByCameraId(id);
//        cameraRepository.delete(camera);
//
//        System.out.println(">>> Đã xóa camera ID: " + id);
//        if (username != null) {
//            try {
//                Map<String, Object> msg = Map.of(
//                    "type", "CAMERA_DELETED",
//                    "id", id
//                );
//                String json = objectMapper.writeValueAsString(msg);
//                webSocketHandler.sendMessageToUser(username, json);
//            } catch (Exception e) {
//                System.err.println("WebSocket lỗi: " + e.getMessage());
//            }
//        }
//    }
	public void deleteCamera(int cameraId, Long currentUserId) throws IOException {
	        
	        // 1. Kiểm tra quyền sở hữu (Tìm camera theo ID và ID chủ sở hữu)
	        CameraEntity camera = cameraRepository.findByIdAndClientUserId(cameraId, currentUserId.intValue())
	                .orElseThrow(() -> new EntityNotFoundException("Access Denied: Camera not found or not owned by user."));
	
	        // 2. Xóa tất cả ảnh liên quan (gọi ImageService)
	        // (Bước này sẽ xóa cả file và metadata)
	        imageService.deleteAllImagesForCamera(cameraId);
	            
	        // 3. Xóa Camera (chỉ khi xóa ảnh thành công)
	        cameraRepository.delete(camera);
	        String username = null;
	        if (camera.getClient() != null && camera.getClient().getUser() != null) {
	            username = camera.getClient().getUser().getUsername();
	        }
	      if (username != null) {
          try {
              Map<String, Object> msg = Map.of(
                  "type", "CAMERA_DELETED",
                  "id", cameraId
              );
              String json = objectMapper.writeValueAsString(msg);
              webSocketHandler.sendMessageToUser(username, json);
          } catch (Exception e) {
              System.err.println("WebSocket lỗi: " + e.getMessage());
          }
      }
	    }
}
    
