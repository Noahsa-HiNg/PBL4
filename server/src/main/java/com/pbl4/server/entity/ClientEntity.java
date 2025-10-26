package com.pbl4.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table(name = "Clients", uniqueConstraints = {
    // Đảm bảo constraint bạn vừa thêm bằng SQL
    @UniqueConstraint(columnNames = {"user_id", "machine_id"}) 
})
public class ClientEntity implements Serializable {
    private static final long serialVersionUID = 2L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Liên kết Nhiều-1: Client này thuộc về 1 User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // ID duy nhất của máy (từ SQL ta vừa thêm)
    @Column(name = "machine_id", nullable = false, length = 100)
    private String machineId; 

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "last_heartbeat")
    private Timestamp lastHeartbeat;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "capture_interval_seconds", nullable = false)
    private int captureIntervalSeconds;

    @Column(name = "compression_quality", nullable = false)
    private int compressionQuality;

    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    // 1 Client quản lý nhiều Camera
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.EAGER) // EAGER để tải camera ngay
    private Set<CameraEntity> cameras;

    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Timestamp lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }
    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }
    public int getCaptureIntervalSeconds() { return captureIntervalSeconds; }
    public void setCaptureIntervalSeconds(int captureIntervalSeconds) { this.captureIntervalSeconds = captureIntervalSeconds; }
    public int getCompressionQuality() { return compressionQuality; }
    public void setCompressionQuality(int compressionQuality) { this.compressionQuality = compressionQuality; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    public Set<CameraEntity> getCameras() { return cameras; }
    public void setCameras(Set<CameraEntity> cameras) { this.cameras = cameras; }
}