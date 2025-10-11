package com.pbl4.cameraclient.ui.controller;

import pbl4.common.model.Camera;
import com.pbl4.cameraclient.service.ClientService;
import com.pbl4.cameraclient.service.SyncResponse;
import javafx.concurrent.Task;
import javafx.fxml.FXML;

import java.util.List;

public class MainViewController {

    private final ClientService clientService = new ClientService();

    // Phương thức này được JavaFX tự động gọi sau khi file FXML được nạp
    @FXML
    public void initialize() {
        System.out.println("MainViewController has been initialized. Starting client sync...");
        syncClientData();
    }

    private void syncClientData() {
        // Tạo Task để chạy việc đồng bộ trên luồng nền
        Task<SyncResponse> syncTask = new Task<>() {
            @Override
            protected SyncResponse call() throws Exception {
                // Gọi service giả lập
                return clientService.initializeAndSync();
            }
        };

        // Xử lý khi task thành công
        syncTask.setOnSucceeded(event -> {
            SyncResponse response = syncTask.getValue();
            
            System.out.println("Sync Succeeded. Updating UI...");
            System.out.println("Client Info: " + response.getClient());
            
            List<Camera> cameras = response.getCameras();
            System.out.println("Cameras received (" + cameras.size() + "):");
            for (Camera cam : cameras) {
                System.out.println("  -> " + cam);
            }

            // TODO: Hiển thị danh sách camera này lên giao diện (ListView/TableView)
        });

        // Xử lý khi task thất bại
        syncTask.setOnFailed(event -> {
            System.err.println("Client sync failed!");
            syncTask.getException().printStackTrace();
            // TODO: Hiển thị thông báo lỗi trên giao diện
        });

        // Khởi chạy task trên một luồng mới
        new Thread(syncTask).start();
    }
}