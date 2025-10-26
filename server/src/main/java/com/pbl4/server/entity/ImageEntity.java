package com.pbl4.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp; // Dùng Timestamp cho captured_at
import java.time.LocalDateTime; // Hoặc dùng LocalDateTime

@Entity
@Table(name = "Images")
public class ImageEntity implements Serializable {
    private static final long serialVersionUID = 4L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_id", nullable = false)
    private CameraEntity camera;

    @Column(name = "relative_path", nullable = false, length = 1000)
    private String relativePath;

    @Column(name = "file_size_kb", precision = 10, scale = 2)
    private BigDecimal fileSizeKb;

    // Khớp với DATETIME trong SQL
    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt; 

    @Column(name = "uploaded_at")
    private Timestamp uploadedAt;

    @Column(columnDefinition = "json")
    private String metadata;
    
    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CameraEntity getCamera() { return camera; }
    public void setCamera(CameraEntity camera) { this.camera = camera; }
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
    public BigDecimal getFileSizeKb() { return fileSizeKb; }
    public void setFileSizeKb(BigDecimal fileSizeKb) { this.fileSizeKb = fileSizeKb; }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
    public Timestamp getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}