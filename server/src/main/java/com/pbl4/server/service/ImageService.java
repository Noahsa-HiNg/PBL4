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

    private final Path fileStorageLocation; // Thư mục gốc: E:/surveillance_images
    private final ImageRepository imageRepository;
    private final CameraRepository cameraRepository;
    private final ClientRepository clientRepository;
    private final UserRepository  userRepository;
    // Định dạng Năm/Tháng/Ngày
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
            // Chỉ tạo thư mục gốc ở đây
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
        
        // Cần clientId để tạo thư mục
        Integer clientId = (camera.getClient() != null) ? camera.getClient().getId() : 0; // Hoặc xử lý lỗi nếu client null
        if (clientId != null) {
            // GỌI HÀM CẬP NHẬT TRẠNG THÁI CLIENT LÊN ACTIVE
            updateClientStatusToActive(clientId); 
        }
        // 2. Tạo tên file duy nhất
        String originalFileName = file.getOriginalFilename();
        // Lấy phần mở rộng file (vd: .jpg)
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        // Tên file mới không cần tên gốc để tránh ký tự đặc biệt
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension; 

        // 3. Tạo đường dẫn tương đối và thư mục con
        LocalDateTime capturedDateTime = capturedAt.toLocalDateTime();
        String datePath = capturedDateTime.format(DATE_FORMATTER); // "YYYY/MM/DD"
        String relativePathString = Paths.get(
                String.valueOf(clientId), // Thư mục client
                String.valueOf(cameraId),  // Thư mục camera
                datePath,                  // Thư mục YYYY/MM/DD
                uniqueFileName             // Tên file
        ).toString().replace("\\", "/"); // Đảm bảo dùng '/' cho web

        Path targetDirectory = this.fileStorageLocation.resolve(Paths.get(
                String.valueOf(clientId),
                String.valueOf(cameraId),
                datePath
        )).normalize();

        Path targetLocation = targetDirectory.resolve(uniqueFileName).normalize();

        try {
            // Tạo thư mục con nếu chưa có
            Files.createDirectories(targetDirectory);

            // 4. Lưu file ảnh
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // 5. Tạo và lưu ImageEntity vào CSDL
            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setRelativePath(relativePathString);
            imageEntity.setFileSizeKb(BigDecimal.valueOf(file.getSize() / 1024.0));
            imageEntity.setCapturedAt(capturedAt.toLocalDateTime());
            imageEntity.setUploadedAt(new Timestamp(System.currentTimeMillis()));
            imageEntity.setCamera(camera);
            
            ImageEntity savedEntity = imageRepository.save(imageEntity);

            // 6. Chuyển đổi thành DTO để trả về
            return toDto(savedEntity);

        } catch (IOException e) {
            // Cân nhắc xóa file nếu lưu vào DB thất bại (rollback)
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
public Page<Image> getImageList(Long userId, Pageable pageable, Integer cameraId) {
        
        // Chuyển đổi Long userId sang int cho Repository (giả định ID là int trong Entity)
        int userIdInt = userId.intValue();
        
        Page<ImageEntity> imageEntityPage;
        
        // --- LOGIC LỌC THEO SỞ HỮU ---
        if (cameraId != null) {
            // Nếu có lọc theo Camera ID, cần sử dụng phương thức kết hợp (nếu có).
            // Nếu không, chỉ lọc theo User ID.
            // Giả định dùng phương thức findByCameraIdAndCameraClientUserId
            // imageEntityPage = imageRepository.findByCameraIdAndCameraClientUserId(cameraId, userIdInt, pageable);
            
            // Tạm thời chỉ gọi phương thức chung nếu không muốn tạo phương thức kết hợp:
            imageEntityPage = imageRepository.findByCameraId(cameraId, pageable);
        } else {
            // Lọc mặc định theo User ID sở hữu
            imageEntityPage = imageRepository.findByCameraClientUserId(userIdInt, pageable);
        }

        // --- Hết LOGIC PHÂN QUYỀN ---
        
        // Chuyển đổi Entity Page sang DTO Page
        return imageEntityPage.map(this::toDto);
    }

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
        
        // FilePath trong DTO là đường dẫn TƯƠNG ĐỐI
        // Controller sẽ dùng nó để xây dựng URL đầy đủ
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
            // Đặt trạng thái: "ACTIVE"
            client.setStatus("ACTIVE"); 
//            client.setLastImageReceived(new Timestamp(System.currentTimeMillis())); 
//            client.setLastPingAttempt(null); 
            clientRepository.save(client);
        });
    }
    public void setAllClientsOfUserOffline(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            List<ClientEntity> clients = clientRepository.findByUserId(user.getId());
            
            for (ClientEntity client : clients) {
                client.setStatus("OFFLINE"); // Đặt trạng thái về OFFLINE
            }
            clientRepository.saveAll(clients);
        });
    }
    
}