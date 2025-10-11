// Trong camera-client/src/main/java/com/pbl4/client/config/AppConfig.java
package com.pbl4.client.config;

import pbl4.common.model.Camera;
import pbl4.common.model.Client;

// Singleton để giữ cấu hình toàn cục
public class AppConfig {
    private static AppConfig instance;

    private String serverApiUrl;
    private String imageStoragePath;
    
    // Sử dụng trực tiếp các đối tượng model để lưu cấu hình
    private Client clientInfo;
    private Camera cameraInfo;

    private AppConfig() {}

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    // Getters and Setters
    public String getServerApiUrl() {
        return serverApiUrl;
    }

    public void setServerApiUrl(String serverApiUrl) {
        this.serverApiUrl = serverApiUrl;
    }

    public String getImageStoragePath() {
        return imageStoragePath;
    }

    public void setImageStoragePath(String imageStoragePath) {
        this.imageStoragePath = imageStoragePath;
    }

    public Client getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Client clientInfo) {
        this.clientInfo = clientInfo;
    }

    public Camera getCameraInfo() {
        return cameraInfo;
    }

    public void setCameraInfo(Camera cameraInfo) {
        this.cameraInfo = cameraInfo;
    }
}