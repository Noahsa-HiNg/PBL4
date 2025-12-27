package com.pbl4.cameraclient.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientRegisterResponse {
    // THAY ĐỔI Ở ĐÂY
    private ClientDTO client;
    private String message;
    private List<CameraDTO> cameras; // Đảm bảo CameraDTO ở client cũng đúng

    // Constructor rỗng (BẮT BUỘC)
    public ClientRegisterResponse() {}

    // Getters và Setters (BẮT BUỘC)
    public ClientDTO getClient() { return client; }
    public void setClient(ClientDTO client) { this.client = client; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<CameraDTO> getCameras() { return cameras; }
    public void setCameras(List<CameraDTO> cameras) { this.cameras = cameras; }
}
