package com.pbl4.cameraclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// DTO Client gửi lên server khi thêm camera
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddCameraRequest {
    private String cameraName;
    private String ipAddress;
    private String onvifUrl;
    private String username;
    private String password;
    private int clientId; // ID của Client (máy tính) sở hữu camera này

    // Cần constructor rỗng cho Jackson (nếu cần)
    public AddCameraRequest() {}

    // Getters and Setters (Bắt buộc cho Jackson)
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