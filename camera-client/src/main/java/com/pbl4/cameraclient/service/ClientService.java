package com.pbl4.cameraclient.service;

import pbl4.common.model.Camera;
import pbl4.common.model.Client;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientService {

    /**
     * Giả lập việc gọi API để đồng bộ thông tin client và lấy danh sách camera.
     * @return một đối tượng SyncResponse chứa dữ liệu mẫu.
     */
    public SyncResponse initializeAndSync() {
        System.out.println("--- MOCK SYNC ---");
        System.out.println("Đang giả lập việc đồng bộ client với server...");

        // Giả vờ có độ trễ mạng
        try {
            Thread.sleep(1000); // Chờ 1 giây
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // --- TẠO DỮ LIỆU MẪU (MOCK DATA) ---
        // 1. Dữ liệu Client
        Client mockClient = new Client();
        mockClient.setId(1);
        mockClient.setClientName("My Test Laptop");
        mockClient.setCaptureIntervalSeconds(15); // Cấu hình 15 giây chụp 1 lần
        mockClient.setCompressionQuality(90);
        mockClient.setImageWidth(1280);
        mockClient.setImageHeight(720);

        // 2. Dữ liệu Camera
        List<Camera> mockCameras = new ArrayList<>();
        
        Camera cam1 = new Camera();
        cam1.setId(101);
        cam1.setCameraName("Camera Sảnh chính");
        cam1.setIpAddress("192.168.1.108");
        cam1.setActive(true);
        
        Camera cam2 = new Camera();
        cam2.setId(102);
        cam2.setCameraName("Camera Bãi xe");
        cam2.setIpAddress("192.168.1.109");
        cam2.setActive(false);

        mockCameras.add(cam1);
        mockCameras.add(cam2);
        
        System.out.println("Đồng bộ giả lập thành công! Trả về 1 client và 2 camera.");
        System.out.println("--------------------");

        return new SyncResponse(mockClient, mockCameras);
    }
}