package com.pbl4.cameraclient.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;

import javax.swing.JOptionPane;

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
 // Chọn một cổng ngẫu nhiên ít người dùng (từ 49152 đến 65535)
    private static final int SINGLE_INSTANCE_PORT = 56789; 
    private static ServerSocket instanceSocket;

    public static void main(String[] args) {
        try {
            // 1. Cố gắng chiếm dụng cổng này
            instanceSocket = new ServerSocket(SINGLE_INSTANCE_PORT);
            
            // Nếu dòng trên chạy qua mà không lỗi, nghĩa là chưa có ai chạy
            // -> Tiếp tục khởi chạy JavaFX
            launch(args);

        } catch (IOException e) {
            // 2. Nếu vào đây, nghĩa là cổng đã bị chiếm (App đang chạy rồi)
            System.err.println("Ứng dụng đang chạy! Không thể mở thêm.");
            
            // Hiển thị thông báo (Dùng JOptionPane của Java Swing cho nhanh gọn)
            JOptionPane.showMessageDialog(null, 
                "Ứng dụng Camera Client đang chạy rồi!\nVui lòng kiểm tra thanh taskbar.", 
                "Cảnh báo", 
                JOptionPane.WARNING_MESSAGE);

            // 3. Tắt ngay lập tức
            System.exit(1);
        }
    }
}