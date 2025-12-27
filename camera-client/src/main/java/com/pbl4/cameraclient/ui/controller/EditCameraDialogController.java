package com.pbl4.cameraclient.ui.controller;

import com.pbl4.cameraclient.model.CameraDTO;
import com.pbl4.cameraclient.network.ApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.io.IOException;

public class EditCameraDialogController {

    @FXML private TextField cameraNameField;
    @FXML private TextField ipAddressField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField urlField; // <-- THÊM DÒNG NÀY

    @FXML private Button saveButton;
    @FXML private Label errorLabel;

    private CameraDTO cameraToEdit;
    private ApiClient apiClient;

    @FXML
    public void initialize() {
        this.apiClient = ApiClient.getInstance();
    }

    /**
     * Cập nhật hàm initData
     */
    public void initData(CameraDTO camera) {
        this.cameraToEdit = camera;

        // Điền thông tin cũ vào các trường
        cameraNameField.setText(camera.getCameraName());
        ipAddressField.setText(camera.getIpAddress());
        
        usernameField.setText(camera.getUsername() != null ? camera.getUsername() : "");
        passwordField.setText(camera.getPassword() != null ? camera.getPassword() : "");
        urlField.setText(camera.getOnvifUrl() != null ? camera.getOnvifUrl() : ""); // <-- THÊM DÒNG NÀY
    }

    /**
     * Cập nhật hàm handleSave
     */
    @FXML
    private void handleSave() {
        // 1. Lấy thông tin mới
        String newCameraName = cameraNameField.getText().trim();
        String newIpAddress = ipAddressField.getText().trim();
        String newUsername = usernameField.getText();
        String newPassword = passwordField.getText();
        String newUrl = urlField.getText().trim(); // <-- THÊM DÒNG NÀY

        // 2. Validate
        if (newCameraName.isEmpty() || newIpAddress.isEmpty()) {
            showError("Tên,IP và url không được để trống.");
            return;
        }

        // 3. Tạo DTO mới để gửi
        CameraDTO updatedCameraData = new CameraDTO();
        updatedCameraData.setId(cameraToEdit.getId());
        updatedCameraData.setCameraName(newCameraName);
        updatedCameraData.setIpAddress(newIpAddress);
        updatedCameraData.setUsername(newUsername);
        updatedCameraData.setPassword(newPassword);
        updatedCameraData.setUrl(newUrl.isEmpty() ? null : newUrl); // <-- THÊM DÒNG NÀY (Nếu rỗng thì gửi null)

        saveButton.setDisable(true);
        showError(null);

        // 4. GỌI API LÊN SERVER (trong 1 luồng riêng)
        new Thread(() -> {
            try {
                // Giả sử CameraDTO của bạn đã có setUrl()
                apiClient.updateCamera(cameraToEdit.getId(), updatedCameraData);

                Platform.runLater(this::closeDialog);
                
            } catch (Exception e) {
                System.err.println("Lỗi khi cập nhật camera: " + e.getMessage());
                Platform.runLater(() -> {
                    showError("Lỗi: " + e.getMessage());
                    saveButton.setDisable(false);
                });
            }
        }).start();
    }

    // ... (Các hàm showError và closeDialog giữ nguyên) ...
    private void showError(String message) {
        Platform.runLater(() -> {
            if (message == null || message.isEmpty()) {
                errorLabel.setVisible(false);
                errorLabel.setManaged(false);
            } else {
                errorLabel.setText(message);
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        });
    }

    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}