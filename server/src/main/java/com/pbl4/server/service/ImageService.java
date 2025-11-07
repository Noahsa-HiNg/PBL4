package com.pbl4.server.service;

import com.pbl4.server.entity.CameraEntity;
import com.pbl4.server.entity.ClientEntity;
import com.pbl4.server.entity.ImageEntity;
import com.pbl4.server.repository.CameraRepository;
import com.pbl4.server.repository.ImageRepository;
import com.pbl4.server.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException; // Dùng exception cụ thể
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page; // Cho phân trang
import org.springframework.data.domain.Pageable; // Cho phân trang
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
// Xóa import List và Collectors vì Page<> tự xử lý
// import java.util.List;
// import java.util.stream.Collectors;

@Service
public class ImageService {

    private final Path fileStorageLocation;
    private final ImageRepository imageRepository;
    private final CameraRepository cameraRepository;
    private final ClientRepository clientRepository;
    private final UserRepository  userRepository;
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

    /**
     * Lưu file ảnh vào cấu trúc thư mục và lưu metadata vào DB.
     * Trả về DTO 'Image'.
     */
    @Transactional
    public Image store(MultipartFile file, int cameraId, Timestamp capturedAt) {
        // 1. Tìm Camera và Client ID
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
            return toDto(savedEntity);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFileName, e);
        }
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
    @Transactional
    public void deleteImage(Long id) {
        ImageEntity imageEntity = imageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Image not found with id " + id));

        // 1. Xóa file vật lý trước
        try {
            Path filePath = this.fileStorageLocation.resolve(imageEntity.getRelativePath()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log lỗi nhưng vẫn tiếp tục xóa trong DB
            System.err.println("Could not delete file: " + imageEntity.getRelativePath() + " - " + e.getMessage());
        }

        // 2. Xóa bản ghi trong CSDL
        imageRepository.delete(imageEntity);
    }
    
    /**
     * Hàm tiện ích private để chuyển đổi từ ImageEntity sang Image DTO.
     */
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
        
        // entity.getUploadedAt() là Timestamp (từ CSDL)
        // dto.setUploadedAt() mong đợi LocalDateTime (theo báo lỗi)
        if (entity.getUploadedAt() != null) {
            // Chuyển đổi Timestamp sang LocalDateTime
            dto.setUploadedAt(entity.getUploadedAt().toLocalDateTime()); 
        }
        dto.setMetadata(entity.getMetadata());
        return dto;
    }
    
    private void updateClientStatusToActive(int clientId) {
        clientRepository.findById(clientId).ifPresent(client -> {
            client.setStatus("ACTIVE"); 
            client.setLastImageReceived(new Timestamp(System.currentTimeMillis())); 
            client.setLastPingAttempt(null); 
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
    
}