package com.pbl4.server.dto;

import java.util.List;

// DTO chứa kết quả trả về cho client sau khi gọi API register
public class ClientRegisterResponse {
    // THAY ĐỔI: Thay vì chỉ ID, trả về cả đối tượng ClientDTO
    private ClientDTO client;
    private String message;
    private List<CameraDTO> cameras;

    // Constructor rỗng (nếu client cần)
    // public ClientRegisterResponse() {}

    // Sửa Constructor
    public ClientRegisterResponse(ClientDTO client, String message, List<CameraDTO> cameras) {
        this.client = client;
        this.message = message;
        this.cameras = cameras;
    }

    // Sửa Getter
    public ClientDTO getClient() { return client; }
    public String getMessage() { return message; }
    public List<CameraDTO> getCameras() { return cameras; }

    // Setters (nếu client cần)
    // public void setClient(ClientDTO client) { this.client = client; }
    // public void setMessage(String message) { this.message = message; }
    // public void setCameras(List<CameraDTO> cameras) { this.cameras = cameras; }
}