package com.pbl4.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "Images")
public class ImageEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // SỬA LẠI TÊN TRƯỜNG VÀ TÊN CỘT
    @Column(name = "image_name", nullable = false)
    private String imageName; // <-- Sửa từ 'fileName' thành 'imageName'

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size_kb")
    private Double fileSizeKb;

    @Column(name = "captured_at", nullable = false)
    private Timestamp capturedAt;

    @Column(name = "uploaded_at")
    private Timestamp uploadedAt;

    @Column(columnDefinition = "json")
    private String metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_id", nullable = false)
    private CameraEntity camera;
    
    public ImageEntity() {}

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    // SỬA LẠI TÊN GETTER/SETTER
    public String getImageName() { // <-- Sửa từ 'getFileName'
        return imageName;
    }

    public void setImageName(String imageName) { // <-- Sửa từ 'setFileName'
        this.imageName = imageName;
    }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Double getFileSizeKb() { return fileSizeKb; }
    public void setFileSizeKb(Double fileSizeKb) { this.fileSizeKb = fileSizeKb; }

    public Timestamp getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Timestamp capturedAt) { this.capturedAt = capturedAt; }

    public Timestamp getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public CameraEntity getCamera() { return camera; }
    public void setCamera(CameraEntity camera) { this.camera = camera; }
}