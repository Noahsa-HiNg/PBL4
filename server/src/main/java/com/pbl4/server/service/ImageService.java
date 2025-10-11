package com.pbl4.server.service;

import com.pbl4.server.entity.CameraEntity;
import com.pbl4.server.entity.ImageEntity;
import com.pbl4.server.repository.CameraRepository;
import com.pbl4.server.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pbl4.common.model.Camera;
import pbl4.common.model.Image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private final Path fileStorageLocation;
    private final ImageRepository imageRepository;
    private final CameraRepository cameraRepository;

    public ImageService(ImageRepository imageRepository,
                        CameraRepository cameraRepository,
                        @Value("${file.upload-dir}") String uploadDir) {
        this.imageRepository = imageRepository;
        this.cameraRepository = cameraRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create the directory for uploads.", e);
        }
    }

    /**
     * SỬA ĐỔI 1: Phương thức store bây giờ trả về DTO 'Image'.
     */
    public Image store(MultipartFile file, int cameraId, Timestamp capturedAt) {
        CameraEntity camera = cameraRepository.findById(cameraId)
                .orElseThrow(() -> new RuntimeException("Error: Camera not found with id " + cameraId));

        String originalFileName = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation);

            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setImageName(originalFileName);
            imageEntity.setFilePath(uniqueFileName);
            imageEntity.setFileSizeKb(file.getSize() / 1024.0);
            imageEntity.setCapturedAt(capturedAt);
            imageEntity.setUploadedAt(new Timestamp(System.currentTimeMillis()));
            imageEntity.setCamera(camera);
            
            ImageEntity savedEntity = imageRepository.save(imageEntity);

            // Chuyển đổi Entity đã lưu thành DTO trước khi trả về
            return toDto(savedEntity);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + originalFileName, e);
        }
    }

    /**
     * Phương thức này là phương thức chính để lấy danh sách ảnh.
     * Nó trả về một danh sách các DTO.
     */
    public List<Image> getAllImages() {
        List<ImageEntity> entities = imageRepository.findAll();
        return entities.stream()
                       .map(this::toDto)
                       .collect(Collectors.toList());
    }
    
    // SỬA ĐỔI 2: Xóa phương thức 'getAllImageEntities' vì không cần thiết nữa.
    
    /**
     * Hàm tiện ích private để chuyển đổi từ ImageEntity sang Image DTO.
     */
    private Image toDto(ImageEntity entity) {
        Image dto = new Image();
        dto.setId(entity.getId());
        if (entity.getCamera() != null) {
            dto.setCameraId(entity.getCamera().getId());
        }
        dto.setImageName(entity.getImageName());
        dto.setFilePath(entity.getFilePath()); // Tên file sẽ được Controller xử lý thành URL
        dto.setFileSizeKb(entity.getFileSizeKb());
        dto.setCapturedAt(entity.getCapturedAt());
        dto.setUploadedAt(entity.getUploadedAt());
        dto.setMetadata(entity.getMetadata());
        return dto;
    }
}