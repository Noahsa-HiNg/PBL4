package com.pbl4.server.dto;

import com.pbl4.server.entity.CameraEntity;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CameraDTO {
    private int id;
    private String cameraName;
    private String ipAddress;
    private String onvifUrl;
    private String username;
    private String password; // Gửi username/password của camera để client kết nối
    private boolean isActive;
<<<<<<< Updated upstream
    
=======
>>>>>>> Stashed changes

    // Constructor để dễ dàng chuyển đổi từ Entity sang DTO
    public CameraDTO(CameraEntity entity) {
        this.id = entity.getId();
        this.cameraName = entity.getCameraName();
        this.ipAddress = entity.getIpAddress();
        this.onvifUrl = entity.getOnvifUrl();
        this.username = entity.getUsername();
        this.password = entity.getPassword(); // Lấy mật khẩu camera
        this.isActive = entity.isActive();
    }
    public CameraDTO() {
        // Constructor rỗng cần cho Jackson Deserialization
    }

    // Getters (Bắt buộc để Jackson tạo JSON)
    public int getId() { return id; }
    public String getCameraName() { return cameraName; }
    public String getIpAddress() { return ipAddress; }
    public String getOnvifUrl() { return onvifUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public boolean isActive() { return isActive; } // Jackson tự chuyển thành "active": true/false
<<<<<<< Updated upstream
    public void setId(int id) {
        this.id = id;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    /**
     * Đặt tên đăng nhập (RTSP) cho camera.
     * @param username Tên đăng nhập
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Đặt mật khẩu (RTSP) cho camera.
     * @param password Mật khẩu
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Đặt URL (RTSP) đầy đủ cho camera.
     * @param url URL
     */
    public void setUrl(String url) {
        this.onvifUrl = url;
    }
=======
>>>>>>> Stashed changes
}
