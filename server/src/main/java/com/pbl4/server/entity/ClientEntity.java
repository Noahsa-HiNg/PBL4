package com.pbl4.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "Clients")
public class ClientEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(nullable = false)
    private String status;

    @Column(name = "last_heartbeat")
    private Timestamp lastHeartbeat;

    @Column(name = "image_width")
    private int imageWidth;

    @Column(name = "image_height")
    private int imageHeight;

    @Column(name = "capture_interval_seconds", nullable = false)
    private int captureIntervalSeconds;

    @Column(name = "compression_quality", nullable = false)
    private int compressionQuality;

    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CameraEntity> cameras = new ArrayList<>();

    public ClientEntity() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Timestamp lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public int getImageWidth() { return imageWidth; }
    public void setImageWidth(int imageWidth) { this.imageWidth = imageWidth; }
    public int getImageHeight() { return imageHeight; }
    public void setImageHeight(int imageHeight) { this.imageHeight = imageHeight; }
    public int getCaptureIntervalSeconds() { return captureIntervalSeconds; }
    public void setCaptureIntervalSeconds(int captureIntervalSeconds) { this.captureIntervalSeconds = captureIntervalSeconds; }
    public int getCompressionQuality() { return compressionQuality; }
    public void setCompressionQuality(int compressionQuality) { this.compressionQuality = compressionQuality; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public List<CameraEntity> getCameras() { return cameras; }
    public void setCameras(List<CameraEntity> cameras) { this.cameras = cameras; }
}