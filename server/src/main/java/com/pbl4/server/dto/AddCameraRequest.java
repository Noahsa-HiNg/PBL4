package com.pbl4.server.dto;

// DTO chứa thông tin client gửi lên khi thêm camera mới
public class AddCameraRequest {
    private String cameraName;
    private String ipAddress;
    private String onvifUrl;
    private String username;
    private String password;
    private int clientId; // Client (máy tính) mà camera này sẽ thuộc về

    // Getters và Setters (Bắt buộc)
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
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }
}