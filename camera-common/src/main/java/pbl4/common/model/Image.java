package pbl4.common.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
/**
 * Lớp này ánh xạ tới bảng 'Images' trong cơ sở dữ liệu.
 * Lưu trữ metadata của các hình ảnh được chụp.
 */
public class Image implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id; // Dùng long cho BIGINT
    private int cameraId;
    private String imageName;
    private String relativePath;
    private double fileSizeKb; // Dùng double cho DECIMAL
    private LocalDateTime capturedAt;
    private LocalDateTime uploadedAt;
    private String metadata; // Kiểu JSON trong DB sẽ được xử lý như một chuỗi String

    public Image() {
    }

    public Image(long id, int cameraId, String imageName, String relativePath, double fileSizeKb, LocalDateTime  capturedAt, LocalDateTime  uploadedAt, String metadata) {
        this.id = id;
        this.cameraId = cameraId;
        this.imageName = imageName;
        this.relativePath = relativePath;
        this.fileSizeKb = fileSizeKb;
        this.capturedAt = capturedAt;
        this.uploadedAt = uploadedAt;
        this.metadata = metadata;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getFilePath() {
        return relativePath;
    }

    public void setFilePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public double getFileSizeKb() {
        return fileSizeKb;
    }

    public void setFileSizeKb(double fileSizeKb) {
        this.fileSizeKb = fileSizeKb;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "Image{" +
                "id=" + id +
                ", cameraId=" + cameraId +
                ", relativePath='" + relativePath + '\'' +
                ", capturedAt=" + capturedAt +
                '}';
    }
}