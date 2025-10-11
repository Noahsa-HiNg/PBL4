package com.pbl4.server.controller;

import com.pbl4.server.service.ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pbl4.common.model.Image; // Chỉ làm việc với DTO

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;
    private final Path fileStorageLocation;

    public ImageController(ImageService imageService, @Value("${file.upload-dir}") String uploadDir) {
        this.imageService = imageService;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
                                       @RequestParam("cameraId") int cameraId,
                                       @RequestParam("capturedAt") Timestamp capturedAt) {
        try {
            // Service đã trả về DTO, chỉ cần xây dựng lại URL
            Image savedDto = imageService.store(file, cameraId, capturedAt);
            // Xây dựng URL đầy đủ cho DTO trước khi trả về
            savedDto.setFilePath(buildFileUrl(savedDto.getFilePath())); 
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload file: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Image>> listAllImages() {
        // Service đã trả về danh sách DTO
        List<Image> dtos = imageService.getAllImages();
        // Xây dựng URL đầy đủ cho từng DTO trong danh sách
        dtos.forEach(dto -> dto.setFilePath(buildFileUrl(dto.getFilePath())));
        return ResponseEntity.ok(dtos);
    }
    
    @GetMapping("/view/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path file = fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Hàm tiện ích private chỉ có trong Controller để xây dựng URL
    private String buildFileUrl(String filename) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/view/")
                .path(filename)
                .toUriString();
    }
}