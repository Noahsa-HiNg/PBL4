package com.pbl4.cameraclient.service;

import pbl4.common.model.Camera;
import pbl4.common.model.Client;
import java.util.List;

// Lớp này dùng để đóng gói dữ liệu mà server trả về sau khi đồng bộ
public class SyncResponse {
    private Client client;
    private List<Camera> cameras;

    // Constructors
    public SyncResponse() {}

    public SyncResponse(Client client, List<Camera> cameras) {
        this.client = client;
        this.cameras = cameras;
    }

    // Getters and Setters
    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<Camera> getCameras() {
        return cameras;
    }

    public void setCameras(List<Camera> cameras) {
        this.cameras = cameras;
    }
}