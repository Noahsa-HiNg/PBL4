package com.pbl4.cameraclient.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * Lớp chính để khởi chạy ứng dụng JavaFX Client.
 */
public class MainApp extends Application {

    /**
     * Phương thức này được JavaFX gọi khi ứng dụng khởi động.
     * @param primaryStage Cửa sổ chính của ứng dụng.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Tạo một đối tượng FXMLLoader để nạp file FXML.
            // Đường dẫn bắt đầu bằng "/" nghĩa là tìm từ gốc của thư mục resources.
            URL fxmlLocation = getClass().getResource("/com/pbl4/cameraclient/ui/view/LoginView.fxml");
            FXMLLoader loader = new FXMLLoader(fxmlLocation);

            // Nạp FXML để tạo ra cấu trúc giao diện (Parent là lớp cha của các layout pane).
            Parent root = loader.load();

            // Tạo một Scene (khung cảnh) chứa giao diện vừa nạp.
            Scene scene = new Scene(root);

            // Thiết lập các thông tin cho cửa sổ chính (Stage).
            primaryStage.setTitle("Camera Client Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Không cho phép thay đổi kích thước cửa sổ

            // Hiển thị cửa sổ.
            primaryStage.show();

        } catch (IOException e) {
            // In ra lỗi nếu không thể nạp file FXML.
            e.printStackTrace();
        }
    }

    /**
     * Phương thức main truyền thống để khởi chạy ứng dụng.
     */
    public static void main(String[] args) {
        launch(args);
    }
}