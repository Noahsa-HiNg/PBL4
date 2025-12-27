package com.pbl4.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.server.entity.CameraEntity;
import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.entity.ImageEntity;
import com.pbl4.server.repository.CameraRepository;
import com.pbl4.server.repository.ImageRepository;
import com.pbl4.server.repository.UserRepository;
import com.pbl4.server.websocket.MyWebSocketHandler;

import jakarta.persistence.EntityNotFoundException; // Dùng exception cụ thể
import org.springframework.transaction.annotation.Transactional;
<<<<<<< Updated upstream
import org.springframework.beans.factory.annotation.Autowired;
=======
>>>>>>> Stashed changes
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page; // Cho phân trang
import org.springframework.data.domain.Pageable; // Cho phân trang
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import pbl4.common.model.Image; // DTO
import com.pbl4.server.repository.ClientRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.LocalDateTime; // Dùng LocalDateTime để lấy Năm/Tháng/Ngày
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;
// Xóa import List và Collectors vì Page<> tự xử lý
// import java.util.List;
// import java.util.stream.Collectors;

@Service
@Transactional
public class ImageService {

    private final Path fileStorageLocation;
    private final ImageRepository imageRepository;
    private final CameraRepository cameraRepository;
    private final ClientRepository clientRepository;
    private final UserRepository  userRepository;
    private MyWebSocketHandler webSocketHandler; 
    private ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public ImageService(ImageRepository imageRepository,
                        CameraRepository cameraRepository,
                        @Value("${file.upload-dir}") String uploadDir,
                        ClientRepository clientRepository,UserRepository userRepository) {
        this.imageRepository = imageRepository;
        this.cameraRepository = cameraRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
       
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create the root directory for uploads.", e);
        }
    }
    @Transactional
    public Image store(MultipartFile file, int cameraId, Timestamp capturedAt) {
        CameraEntity camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Error: Camera not found with id " + cameraId));
        Integer clientId = (camera.getClient() != null) ? camera.getClient().getId() : 0; 
        if (clientId != null) {
            updateClientStatusToActive(clientId); 
        }
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension; 

        LocalDateTime capturedDateTime = capturedAt.toLocalDateTime();
        String datePath = capturedDateTime.format(DATE_FORMATTER); 
        String relativePathString = Paths.get(
                String.valueOf(clientId), 
                String.valueOf(cameraId), 
                datePath,                 
                uniqueFileName             
        ).toString().replace("\\", "/"); 

        Path targetDirectory = this.fileStorageLocation.resolve(Paths.get(
                String.valueOf(clientId),
                String.valueOf(cameraId),
                datePath
        )).normalize();

        Path targetLocation = targetDirectory.resolve(uniqueFileName).normalize();

        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setRelativePath(relativePathString);
            imageEntity.setFileSizeKb(BigDecimal.valueOf(file.getSize() / 1024.0));
            imageEntity.setCapturedAt(capturedAt.toLocalDateTime());
            imageEntity.setUploadedAt(new Timestamp(System.currentTimeMillis()));
            imageEntity.setCamera(camera);
            
            ImageEntity savedEntity = imageRepository.save(imageEntity);
            try {
                String ownerUsername = savedEntity.getCamera().getClient().getUser().getUsername();
                Image imageDto = toDto(savedEntity);
                imageDto.setFilePath(buildFileUrl(imageDto.getFilePath())); 
                Map<String, Object> wsMessage = new HashMap<>();
                wsMessage.put("type", "NEW_IMAGE"); 
                wsMessage.put("data", imageDto);    
                String jsonMessage = objectMapper.writeValueAsString(wsMessage);
                webSocketHandler.sendMessageToUser(ownerUsername, jsonMessage);

            } catch (Exception e) {
                System.err.println("Không thể gửi thông báo WebSocket (ảnh mới): " + e.getMessage());
            }
            return toDto(savedEntity);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFileName, e);
        }
    }
    private String buildFileUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String formattedPath = relativePath.replace("\\", "/"); 
       
        return ServletUriComponentsBuilder.fromCurrentContextPath() 
                .path("/api/images/view") 
                .queryParam("path", formattedPath)
                .toUriString();
    }

    /**
     * Lấy danh sách ảnh CÓ PHÂN TRANG.
     * Trả về Page<Image> DTO.
     */
    @Transactional(readOnly = true)
    public Page<Image> getAllImages(Pageable pageable) {
        Page<ImageEntity> entityPage = imageRepository.findAll(pageable);
        // Page<> có sẵn hàm map để chuyển đổi
        return entityPage.map(this::toDto); 
    }
<<<<<<< Updated upstream
    public Page<Image> getImageList(
            Long userId, 
            Pageable pageable, 
            Integer cameraId,
            LocalDateTime start, 
            LocalDateTime end    
    ) {
            
        int userIdInt = userId.intValue();
        Page<ImageEntity> imageEntityPage;
        boolean hasDateFilter = (start != null && end != null);
        if (start == null) {
            start = LocalDateTime.now().minusYears(100);
        }
        if (end == null) {
            end = LocalDateTime.now().plusDays(1); 
        }
        if (cameraId != null) {
            if (hasDateFilter) {
                imageEntityPage = imageRepository.findByCameraIdAndCapturedAtBetweenOrderByCapturedAtDesc(
                    cameraId, start, end, pageable
                );
            } else {
                imageEntityPage = imageRepository.findByCameraIdOrderByCapturedAtDesc(
                    cameraId, pageable
                );
            }
        } else {
            if (hasDateFilter) {
                imageEntityPage = imageRepository.findByCameraClientUserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
                    userIdInt, start, end, pageable
                );
            } else {
                imageEntityPage = imageRepository.findByCameraClientUserIdOrderByCapturedAtDesc(
                    userIdInt, pageable
                );
            }
        }
            
        return imageEntityPage.map(this::toDto);
    }
=======

    /**
     * Lấy danh sách ảnh CÓ PHÂN TRANG theo Camera ID.
     * Trả về Page<Image> DTO.
     */
    @Transactional(readOnly = true)
    public Page<Image> getImagesByCameraId(int cameraId, Pageable pageable) {
        // Cần thêm findByCameraId(int cameraId, Pageable pageable) vào ImageRepository
        Page<ImageEntity> entityPage = imageRepository.findByCameraId(cameraId, pageable);
        return entityPage.map(this::toDto);
    }

    /**
     * Xóa ảnh theo ID (bao gồm xóa file vật lý).
     */
>>>>>>> Stashed changes
    @Transactional
    public void deleteImage(Long id) {
        ImageEntity imageEntity = imageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with id " + id));
        try {
            Path filePath = this.fileStorageLocation.resolve(imageEntity.getRelativePath()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Could not delete file: " + imageEntity.getRelativePath() + " - " + e.getMessage());
        }
        imageRepository.delete(imageEntity);
    }
    
    private Image toDto(ImageEntity entity) {
        if (entity == null) return null;

        Image dto = new Image();
        dto.setId(entity.getId());
        if (entity.getCamera() != null) {
            dto.setCameraId(entity.getCamera().getId());
        }
        dto.setFilePath(entity.getRelativePath()); 
        
        dto.setFileSizeKb(entity.getFileSizeKb().doubleValue());
        if (entity.getCapturedAt() != null) {
            dto.setCapturedAt(entity.getCapturedAt());
        }
        if (entity.getUploadedAt() != null) {
            dto.setUploadedAt(entity.getUploadedAt().toLocalDateTime()); 
        }
        dto.setMetadata(entity.getMetadata());
        return dto;
    }
    
    private void updateClientStatusToActive(int clientId) {
        clientRepository.findById(clientId).ifPresent(client -> {
            client.setStatus("ACTIVE"); 
            clientRepository.save(client);
        });
    }
    public void setAllClientsOfUserOffline(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            List<ClientEntity> clients = clientRepository.findByUserId(user.getId());
            
            for (ClientEntity client : clients) {
                client.setStatus("OFFLINE"); 
            }
            clientRepository.saveAll(clients);
        });
    }
    @Transactional
    public void deleteImages(List<Long> imageIds, String username) throws AccessDeniedException {
        
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }

        List<ImageEntity> imagesToDelete = imageRepository.findAllById(imageIds);
        
        if (imagesToDelete.isEmpty()) {
            return;
        }

        for (ImageEntity image : imagesToDelete) {
            
            String ownerUsername = image.getCamera().getClient().getUser().getUsername();
            
            if (!ownerUsername.equals(username)) {
                throw new AccessDeniedException("User " + username + " không có quyền xóa ảnh " + image.getId());
            }

            try {
                Path filePath = this.fileStorageLocation.resolve(image.getRelativePath()).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("Lỗi khi xóa file vật lý: " + image.getRelativePath() + " - " + e.getMessage());
            }
        }

        imageRepository.deleteAllInBatch(imagesToDelete);
    }
    public void deleteAllImagesForCamera(int cameraId) throws IOException {
        List<ImageEntity> images = imageRepository.findByCameraId(cameraId);
        for (ImageEntity image : images) {
            deleteImageFile(image.getRelativePath());
        }
        imageRepository.deleteAllByCameraId(cameraId);
    }
    private void deleteImageFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return; 
        }
        
        try {
            Path filePath = this.fileStorageLocation.resolve(relativePath).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Không thể xóa file: " + relativePath + ". Lỗi: " + e.getMessage());
        }
    }
    
}