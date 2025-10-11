package com.pbl4.cameraclient.ui.controller;

import com.pbl4.cameraclient.service.AuthService;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    // Khởi tạo service
    private final AuthService authService = new AuthService();

    @FXML
    void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        statusLabel.setText("Logging in...");
        loginButton.setDisable(true); // Vô hiệu hóa nút bấm trong khi chờ

        // Tạo một Task để chạy việc đăng nhập trên luồng nền
        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // Đây là nơi gọi service. Nó chạy trên luồng khác.
                return authService.login(username, password);
            }
        };

        // Xử lý khi task thành công (chạy trên luồng JavaFX Application Thread)
        loginTask.setOnSucceeded(e -> {
            boolean isLoggedIn = loginTask.getValue();
            if (isLoggedIn) {
                statusLabel.setText("Login successful! Loading main view...");
                // GỌI PHƯƠNG THỨC CHUYỂN MÀN HÌNH
                loadMainView();
            } else {
                statusLabel.setText("Login failed. Please try again.");
                loginButton.setDisable(false);
            }
        });

        // Xử lý khi task thất bại
        loginTask.setOnFailed(e -> {
            statusLabel.setText("An error occurred. Please try again.");
            loginButton.setDisable(false);
        });

        // Khởi chạy task trên một luồng mới
        new Thread(loginTask).start();
    }
    private void loadMainView() {
        try {
            // Lấy ra cửa sổ (Stage) hiện tại từ một thành phần bất kỳ, ví dụ: loginButton
            Stage stage = (Stage) loginButton.getScene().getWindow();

            // Nạp file FXML của màn hình chính
            URL fxmlLocation = getClass().getResource("/com/pbl4/cameraclient/ui/view/MainView.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent mainViewRoot = loader.load();
            
            // Tạo Scene mới và thay thế
            Scene mainScene = new Scene(mainViewRoot);
            stage.setScene(mainScene);
            
            // Cập nhật lại tiêu đề và kích thước cho cửa sổ
            stage.setTitle("Camera Management Dashboard");
            stage.setResizable(true);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error: Could not load main view.");
        }
}
}