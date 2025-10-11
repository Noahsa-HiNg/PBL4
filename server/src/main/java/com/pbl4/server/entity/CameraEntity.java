package com.pbl4.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Cameras")
public class CameraEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "camera_name", nullable = false)
    private String cameraName;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "onvif_url")
    private String onvifUrl;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @OneToMany(mappedBy = "camera", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageEntity> images = new ArrayList<>();
    
    public CameraEntity() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCameraName() { return cameraName; }
    public void setCameraName(String cameraName) { this.cameraName = cameraName; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getOnvifUrl() { return onvifUrl; }
    public void setOnvifUrl(String onvifUrl) { this.onvifUrl = onvifUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public ClientEntity getClient() { return client; }
    public void setClient(ClientEntity client) { this.client = client; }
    public List<ImageEntity> getImages() { return images; }
    public void setImages(List<ImageEntity> images) { this.images = images; }
}