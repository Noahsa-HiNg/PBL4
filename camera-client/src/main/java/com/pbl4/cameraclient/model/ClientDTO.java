package com.pbl4.cameraclient.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.sql.Timestamp; // Hoặc dùng kiểu Date/LocalDateTime nếu bạn muốn chuyển đổi

@JsonIgnoreProperties(ignoreUnknown = true) // Quan trọng
public class ClientDTO {

    private int id;
    private String clientName;
    private String ipAddress;
    private String status;
    private Timestamp lastHeartbeat;
    private Integer imageWidth;
    private Integer imageHeight;
    private int captureIntervalSeconds;
    private int compressionQuality;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructor rỗng BẮT BUỘC cho Jackson
    public ClientDTO() {}

    // Getters (Bắt buộc để code của bạn truy cập dữ liệu)
    public int getId() { return id; }
    public String getClientName() { return clientName; }
    public String getIpAddress() { return ipAddress; }
    public String getStatus() { return status; }
    public Timestamp getLastHeartbeat() { return lastHeartbeat; }
    public Integer getImageWidth() { return imageWidth; }
    public Integer getImageHeight() { return imageHeight; }
    public int getCaptureIntervalSeconds() { return captureIntervalSeconds; }
    public int getCompressionQuality() { return compressionQuality; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    // Setters (Bắt buộc để Jackson gán giá trị từ JSON)
    public void setId(int id) { this.id = id; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setStatus(String status) { this.status = status; }
    public void setLastHeartbeat(Timestamp lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }
    public void setCaptureIntervalSeconds(int captureIntervalSeconds) { this.captureIntervalSeconds = captureIntervalSeconds; }
    public void setCompressionQuality(int compressionQuality) { this.compressionQuality = compressionQuality; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}