package pbl4.common.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Lớp này ánh xạ tới bảng 'Cameras' trong cơ sở dữ liệu.
 * Lưu trữ thông tin về các Camera IP.
 */
public class Camera implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int clientId;
    private String cameraName;
    private String ipAddress;
    private String onvifUrl;
    private String username;
    private String password;
    private boolean active; // Ánh xạ từ is_active TINYINT(1)
    private Timestamp createdAt;

    public Camera() {
    }

    public Camera(int id, int clientId, String cameraName, String ipAddress, String onvifUrl, String username, String password, boolean active, Timestamp createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.cameraName = cameraName;
        this.ipAddress = ipAddress;
        this.onvifUrl = onvifUrl;
        this.username = username;
        this.password = password;
        this.active = active;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getOnvifUrl() {
        return onvifUrl;
    }

    public void setOnvifUrl(String onvifUrl) {
        this.onvifUrl = onvifUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Camera{" +
                "id=" + id +
                ", clientId=" + clientId +
                ", cameraName='" + cameraName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}