package pbl4.common.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Lớp này ánh xạ tới bảng 'Clients' trong cơ sở dữ liệu.
 * Dùng để lưu trữ thông tin và cấu hình của các máy client.
 */
public class Client implements Serializable {
    private static final long serialVersionUID = 1L; // Cần thiết cho Serializable

    private int id;
    private int userId;
    private String clientName;
    private String ipAddress;
    private String MachineId;
    private String status;
    private Timestamp lastHeartbeat;
    private int imageWidth;
    private int imageHeight;
    private int captureIntervalSeconds;
    private int compressionQuality;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructor không tham số
    public Client() {
    }

    // Constructor đầy đủ tham số
    public Client(int id,int userId, String clientName, String ipAddress,String MachineId, String status, Timestamp lastHeartbeat, int imageWidth, int imageHeight, int captureIntervalSeconds, int compressionQuality, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.userId = userId;
        this.clientName = clientName;
        this.ipAddress = ipAddress;
        this.MachineId =MachineId;
        this.status = status;
        this.lastHeartbeat = lastHeartbeat;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.captureIntervalSeconds = captureIntervalSeconds;
        this.compressionQuality = compressionQuality;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public String getMachineId() {
        return ipAddress;
    }

    public void setMachineId(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Timestamp lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public int getCaptureIntervalSeconds() {
        return captureIntervalSeconds;
    }

    public void setCaptureIntervalSeconds(int captureIntervalSeconds) {
        this.captureIntervalSeconds = captureIntervalSeconds;
    }

    public int getCompressionQuality() {
        return compressionQuality;
    }

    public void setCompressionQuality(int compressionQuality) {
        this.compressionQuality = compressionQuality;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", userId=" + userId +
                ", clientName='" + clientName + '\'' +
                ", status='" + status + '\'' +
                ", lastHeartbeat=" + lastHeartbeat +
                '}';
    }
}