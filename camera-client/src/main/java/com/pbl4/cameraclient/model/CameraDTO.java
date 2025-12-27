package com.pbl4.cameraclient.model;
import pbl4.common.model.Camera;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CameraDTO {
    private int id;
    private String cameraName;
    private String ipAddress;
    private String onvifUrl;
    private String username;
    private String password;
    // Không trả về password trừ khi CỰC KỲ cần thiết
    private boolean isActive;

    public CameraDTO(Camera entity) {
        this.id = entity.getId();
        this.cameraName = entity.getCameraName();
        this.password =entity.getPassword();
        this.ipAddress = entity.getIpAddress();
        this.onvifUrl = entity.getOnvifUrl();
        this.username = entity.getUsername();
        this.isActive = entity.isActive();
    }
    public CameraDTO() {
        // Constructor rỗng cần cho Jackson Deserialization
    }
    
    // Getters...
    public int getId() { return id; }
    public String getCameraName() { return cameraName; }
    public String getPassword() { return password; }
    public String getIpAddress() { return ipAddress; }
    public String getOnvifUrl() { return onvifUrl; }
    public String getUsername() { return username; }
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
    
    

    @JsonProperty("active") // Có annotation này ở getter không?
    public boolean isActive() {
        return isActive;
    }

    @JsonProperty("active") // Có annotation này ở setter không?
    public void setActive(boolean active) {
        this.isActive = active;
    }
}