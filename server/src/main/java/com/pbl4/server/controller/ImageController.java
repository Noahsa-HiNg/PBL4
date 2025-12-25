package com.pbl4.server.controller;

import com.pbl4.server.service.ImageService;
import com.pbl4.server.service.UserService;
import com.pbl4.server.websocket.MyWebSocketHandler;

import jakarta.persistence.EntityNotFoundException; // Import for error handling
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.web.PageableDefault; // Import PageableDefault
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pbl4.common.model.Image; // DTO

import java.io.FileNotFoundException; // Import for error handling
import java.net.MalformedURLException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import org.slf4j.Logger; // Thêm import
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort; // <-- THÊM IMPORT NÀY
import org.springframework.format.annotation.DateTimeFormat; // <-- THÊM IMPORT NÀY
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;
    private final UserService userService;
    private final Path fileStorageLocation; 
    private final MyWebSocketHandler webSocketHandler;

    public ImageController(ImageService imageService,UserService userService,MyWebSocketHandler webSocketHandler, @Value("${file.upload-dir}") String uploadDir) {
        this.imageService = imageService;
        this.userService = userService;
        this.webSocketHandler = webSocketHandler;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        System.out.println("DEBUG: fileStorageLocation được khởi tạo là: " + this.fileStorageLocation.toString());
    }

    private String buildFileUrl1(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String formattedPath = relativePath.replace("\\", "/"); 
        
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/view") 
                .queryParam("path", formattedPath) 
                .toUriString();
    }
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
                                       @RequestParam("cameraId") int cameraId,
                                       @RequestParam("capturedAt") long capturedAtMillis) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty.");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        try {
        	Timestamp capturedAt = new Timestamp(capturedAtMillis);
            Image savedDto = imageService.store(file, cameraId, capturedAt);
            savedDto.setFilePath(buildFileUrl1(savedDto.getFilePath())); 
            if (webSocketHandler != null && !"anonymousUser".equals(currentUsername)) {
                String jsonMessage = String.format(
                    "{\"type\": \"NEW_SNAPSHOT\", \"message\": \"Ảnh mới đã được tải lên.\", \"image\": %s}",
                    "{\"id\":" + savedDto.getId() + ", \"camera_id\":" + savedDto.getCameraId() + ", \"url\":\"" + savedDto.getFilePath() + "\"}"
                );
                webSocketHandler.sendMessageToUser(currentUsername, jsonMessage);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
        } catch (EntityNotFoundException e) { 
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
             System.err.println("Upload failed: " + e.getMessage());
             e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload file: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<Page<Image>> getImages(
            @RequestParam(required = false) Integer cameraId,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime start,
            
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime end,
            @PageableDefault(size = 30, sort = "capturedAt", direction = Sort.Direction.DESC) 
            Pageable pageable) { 
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
        }

        Page<Image> imagePage;
        try {
            Long userId = userService.getUserIdByUsername(username); 
            
            if (userId == null) {
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
            imagePage = imageService.getImageList(userId, pageable, cameraId, start, end);
            imagePage.getContent().forEach(dto -> dto.setFilePath(buildFileUrl1(dto.getFilePath())));
            
            return ResponseEntity.ok(imagePage);
        
        } catch (EntityNotFoundException e) { 
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); 
        } catch (Exception e) {
             System.err.println("Error fetching images: " + e.getMessage());
             e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);
    @GetMapping("/view") 
    public ResponseEntity<Resource> getImage(
      
        @RequestParam("path") String relativePath 
    ) {
        logger.info("ImageController: Nhận yêu cầu xem ảnh: '{}'", relativePath);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.debug("ImageController: Thông tin xác thực hiện tại: {}", authentication);
        try {
            Path filePath = fileStorageLocation.resolve(relativePath).normalize();
            System.out.println("DEBUG: Đang cố gắng truy cập file: " + filePath.toString());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                logger.debug("ImageController: Tìm thấy và trả về file: {}", filePath.toString());
                String contentType = determineContentType(filePath); 

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
             
                throw new FileNotFoundException("File not found: " + relativePath); 
            }
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL for path: " + relativePath + " - " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (FileNotFoundException e) {
             System.err.println(e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) { 
             System.err.println("Error reading file: " + relativePath + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) { 
        try {
            imageService.deleteImage(id); 
            return ResponseEntity.noContent().build(); 
        } catch (EntityNotFoundException e) {
             return ResponseEntity.notFound().build();
        } catch (Exception e) {
             System.err.println("Error deleting image ID " + id + ": " + e.getMessage());
             e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @DeleteMapping
    public ResponseEntity<?> deleteBatchImages(@RequestBody BatchDeleteRequest deleteRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(currentUsername)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("message", "Bạn cần đăng nhập để thực hiện việc này.")); 
        }

        try {
 
            List<Long> idsToDelete = deleteRequest.getPhotoIds();
            
            if (idsToDelete == null || idsToDelete.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                     .body(Map.of("message", "Danh sách ID không được rỗng."));
            }

          
            imageService.deleteImages(idsToDelete, currentUsername);
            return ResponseEntity.ok(Map.of("message", "Đã xóa thành công " + idsToDelete.size() + " ảnh."));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(Map.of("message", "Không có quyền xóa một hoặc nhiều ảnh đã chọn."));
        
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa hàng loạt: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "Lỗi server khi xóa ảnh."));
        }
    }

  
     private String determineContentType(Path path) {
         String filename = path.getFileName().toString().toLowerCase();
         if (filename.endsWith(".png")) {
             return MediaType.IMAGE_PNG_VALUE;
         } else if (filename.endsWith(".gif")) {
             return MediaType.IMAGE_GIF_VALUE;
         } else {
             return MediaType.IMAGE_JPEG_VALUE; 
         }
     }
}