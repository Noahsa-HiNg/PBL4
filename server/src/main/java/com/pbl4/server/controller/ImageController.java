package com.pbl4.server.controller;

import com.pbl4.server.service.ImageService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pbl4.common.model.Image; // DTO

import java.io.FileNotFoundException; // Import for error handling
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
// Remove unused List import
// import java.util.List;

@RestController
@RequestMapping("/api/images")
// @CrossOrigin(origins = "*") // Consider global CORS config in SecurityConfig/WebConfig
public class ImageController {

    private final ImageService imageService;
    private final Path fileStorageLocation; // Root storage directory (e.g., E:/surveillance_images)

    public ImageController(ImageService imageService, @Value("${file.upload-dir}") String uploadDir) {
        this.imageService = imageService;
        // Normalize the base storage path
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Handles image upload. Service saves file to structured dir and returns DTO with relative path.
     * Controller builds the full URL for the response.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
                                       @RequestParam("cameraId") int cameraId,
                                       @RequestParam("capturedAt") Timestamp capturedAt) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File cannot be empty.");
        }
        try {
            // Service returns DTO with the relative path stored
            Image savedDto = imageService.store(file, cameraId, capturedAt);
            
            // Build the full, accessible URL using the relative path
            savedDto.setFilePath(buildFileUrl(savedDto.getFilePath())); 
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDto);
        } catch (EntityNotFoundException e) { // Catch specific error from service
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
             // Log the exception details for debugging
             System.err.println("Upload failed: " + e.getMessage());
             e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not upload file: " + e.getMessage());
        }
    }

    /**
     * Gets a paginated list of images, optionally filtered by camera ID.
     * Builds full URLs for each image in the current page.
     */
    @GetMapping
    public ResponseEntity<Page<Image>> getImages(
            @RequestParam(required = false) Integer cameraId,
            // Default: page 0, 20 items/page, sort by capturedAt descending
            @PageableDefault(size = 20, sort = "capturedAt") Pageable pageable) { 
        
        Page<Image> imagePage;
        try {
            if (cameraId != null) {
                // Call the paginated service method for a specific camera
                imagePage = imageService.getImagesByCameraId(cameraId, pageable);
            } else {
                // Call the paginated service method for all images
                imagePage = imageService.getAllImages(pageable);
            }

            // Build full URLs for the file paths in the current page's content
            imagePage.getContent().forEach(dto -> dto.setFilePath(buildFileUrl(dto.getFilePath())));
            
            return ResponseEntity.ok(imagePage);
        
        } catch (EntityNotFoundException e) { // Handle case where client/camera doesn't exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); 
        } catch (Exception e) {
             System.err.println("Error fetching images: " + e.getMessage());
             e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Serves an image file based on its relative path.
     * This acts as a secure gateway to the private storage location.
     */
    @GetMapping("/view/{relativePath:.+}") // Accepts multi-level paths like "1/1/2025/10/26/image.jpg"
    public ResponseEntity<Resource> getImage(@PathVariable String relativePath) {
        try {
            // Combine the root storage location with the relative path
            Path filePath = fileStorageLocation.resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determine content type dynamically if possible, default to JPEG
                String contentType = determineContentType(filePath);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                // Use a more specific exception if needed
                throw new FileNotFoundException("File not found: " + relativePath); 
            }
        } catch (MalformedURLException e) {
            System.err.println("Malformed URL for path: " + relativePath + " - " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (FileNotFoundException e) {
             System.err.println(e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) { // Catch other potential IO errors
             System.err.println("Error reading file: " + relativePath + " - " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Deletes an image record from the database and its corresponding file.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) { 
        try {
            imageService.deleteImage(id); // Service handles DB and file deletion
            return ResponseEntity.noContent().build(); // 204 No Content is standard for successful DELETE
        } catch (EntityNotFoundException e) {
             return ResponseEntity.notFound().build();
        } catch (Exception e) {
             System.err.println("Error deleting image ID " + id + ": " + e.getMessage());
             e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // --- Helper Methods ---

    /**
     * Builds the full, accessible URL for an image given its relative path.
     */
    private String buildFileUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null; // Or return a default placeholder image URL
        }
        // Ensure forward slashes for URL compatibility
        String formattedPath = relativePath.replace("\\", "/"); 
        
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/view/") // Base path for the getImage endpoint
                .path(formattedPath)       // Append the relative path
                .toUriString();
    }
    
    /**
     * Simple helper to determine content type based on file extension.
     */
     private String determineContentType(Path path) {
         String filename = path.getFileName().toString().toLowerCase();
         if (filename.endsWith(".png")) {
             return MediaType.IMAGE_PNG_VALUE;
         } else if (filename.endsWith(".gif")) {
             return MediaType.IMAGE_GIF_VALUE;
         } else {
             // Default to JPEG for .jpg, .jpeg, or unknown
             return MediaType.IMAGE_JPEG_VALUE; 
         }
     }
}