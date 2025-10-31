package com.pbl4.server.dto;

import com.pbl4.server.entity.ClientEntity;
import java.sql.Timestamp;

// DTO chứa thông tin chi tiết của Client trả về cho Client App
public class ClientDTO {

    private int id;
    private String clientName;
    private String ipAddress;
    private String status;
    private Timestamp lastHeartbeat; // Sử dụng Timestamp hoặc kiểu dữ liệu phù hợp
    private Integer imageWidth;
    private Integer imageHeight;
    private int captureIntervalSeconds;
    private int compressionQuality;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    // Không cần user_id hoặc machine_id ở đây trừ khi client cần

    // Constructor để chuyển đổi từ Entity sang DTO
    public ClientDTO(ClientEntity entity) {
        this.id = entity.getId();
        this.clientName = entity.getClientName();
        this.ipAddress = entity.getIpAddress();
        this.status = entity.getStatus();
        this.lastHeartbeat = entity.getLastHeartbeat();
        this.imageWidth = entity.getImageWidth();
        this.imageHeight = entity.getImageHeight();
        this.captureIntervalSeconds = entity.getCaptureIntervalSeconds();
        this.compressionQuality = entity.getCompressionQuality();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }

    // Getters (Bắt buộc để Jackson tạo JSON)
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

    // Setters có thể cần nếu bạn muốn dùng DTO này cho việc cập nhật
    // public void setId(int id) { this.id = id; }
    // ...
}