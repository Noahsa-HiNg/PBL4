package com.pbl4.cameraclient.ui.controller;

import com.pbl4.cameraclient.model.ClientRegisterResponse;
import com.pbl4.cameraclient.network.AppConfig;
import com.pbl4.cameraclient.service.AuthService;
import com.pbl4.cameraclient.service.CameraService;
import com.pbl4.cameraclient.service.ClientService;
import com.pbl4.cameraclient.model.ClientDTO;
import com.pbl4.cameraclient.service.SessionManager;

import javafx.application.Platform;
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
import javafx.scene.input.MouseEvent; // <--- Phải là cái này
import javafx.event.ActionEvent;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

public class LoginController {

	@FXML
	private TextField usernameField;
	@FXML
	private PasswordField passwordField;
	@FXML
	private Button loginButton;
	@FXML
	private Label statusLabel;

	// Khởi tạo service
	private final AuthService authService = AuthService.getInstance();
	private final ClientService clientService = new ClientService();
	private final CameraService cameraService = new CameraService();

	@FXML
    void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ tài khoản và mật khẩu.");
            return;
        }

        statusLabel.setText("Đang đăng nhập...");
        loginButton.setDisable(true); // Vô hiệu hóa nút bấm

        // Tạo Task
        Task<Boolean> loginTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                // QUAN TRỌNG: Nếu AuthService.login bắt exception và trả về false, 
                // bạn cần sửa AuthService để nó "ném" exception ra ngoài khi lỗi mạng.
                return authService.login(username, password);
            }
        };

        // 1. TRƯỜNG HỢP: Code chạy xong không bị lỗi Exception (Đã kết nối được tới Server)
        loginTask.setOnSucceeded(e -> {
            boolean isLoggedIn = loginTask.getValue();
            if (isLoggedIn) {
                statusLabel.setText("Đăng nhập thành công!");
                registerClientAndLoadCameras();
            } else {
                // Kết nối được nhưng Server trả về false -> Sai thông tin
                statusLabel.setText("Tài khoản hoặc mật khẩu không chính xác!");
                loginButton.setDisable(false);
            }
        });

        // 2. TRƯỜNG HỢP: Gặp lỗi kỹ thuật (Mất mạng, Server sập, Time out...)
        loginTask.setOnFailed(e -> {
            Throwable exception = loginTask.getException();
            String errorMessage;

            // Phân loại lỗi để thông báo
            if (exception instanceof java.net.ConnectException) {
                errorMessage = "Không thể kết nối đến Server. Vui lòng kiểm tra mạng hoặc Server.";
            } else if (exception instanceof java.net.SocketTimeoutException) {
                errorMessage = "Kết nối bị quá hạn (Timeout). Server không phản hồi.";
            } else if (exception instanceof java.io.IOException) {
                errorMessage = "Lỗi đường truyền";
            } else {
                errorMessage = "Lỗi không xác định";
                exception.printStackTrace(); // In lỗi ra console để dev sửa
            }

            statusLabel.setText(errorMessage);
            loginButton.setDisable(false);
        });

        new Thread(loginTask).start();
    }

	private void openWebpage(String urlString) {
		try {
			Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
			if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
				desktop.browse(new URI(urlString));
			} else {
				statusLabel.setText("Lỗi: Hệ thống không hỗ trợ mở trình duyệt.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			statusLabel.setText("Lỗi: Không thể mở đường dẫn web.");
		}
	}

	@FXML
	public void handleForgotPassword(MouseEvent event) {
		String ip = AppConfig.getServerIp();
		int port = AppConfig.getServerPort();
	    // SỬA LẠI ĐỊA CHỈ SERVER CỦA BẠN
	    String URL = "http://" + ip + ":" + port + "/forgot-password.html";
		// Thay link bên dưới bằng link đăng ký thực tế của bạn
		openWebpage(URL);
	}

	/**
	 * Xử lý khi nhấn vào Button "Sign Up"
	 */
	@FXML
	public void handleSignUp(ActionEvent event) {
		String ip = AppConfig.getServerIp();
		int port = AppConfig.getServerPort();
	    // SỬA LẠI ĐỊA CHỈ SERVER CỦA BẠN
	    String URL = "http://" + ip + ":" + port + "/register.html";
		// Thay link bên dưới bằng link đăng ký thực tế của bạn
		openWebpage(URL);
	}

	private void registerClientAndLoadCameras() {
		Task<ClientRegisterResponse> registerTask = new Task<>() {
			@Override
			protected ClientRegisterResponse call() throws Exception {
				// Gọi phương thức trong ClientService đã viết
				return clientService.registerClientAndGetCameras();
			}
		};

		registerTask.setOnSucceeded(e -> {
			ClientRegisterResponse response = registerTask.getValue();

			// Kiểm tra response và danh sách camera không null
			if (response != null && response.getCameras() != null) {
				statusLabel.setText(response.getMessage());

				// **THAY ĐỔI QUAN TRỌNG**: Tải MainView TRƯỚC để lấy controller của nó
				loadMainViewAndStartStreams(response); // Gọi hàm trợ giúp mới

			} else {
				String errorMsg = "Đăng ký client thất bại.";
				if (response != null && response.getMessage() != null) {
					errorMsg += " Thông báo từ server: " + response.getMessage();
				}
				statusLabel.setText(errorMsg);
				loginButton.setDisable(false);
			}
		});

		registerTask.setOnFailed(e -> {
			Throwable ex = registerTask.getException();
			String errorMsg = "Lỗi khi đăng ký client";
			if (ex != null) {
				errorMsg += ": " + ex.getMessage();
				ex.printStackTrace(); // In stack trace để debug
			}
			statusLabel.setText(errorMsg);
			loginButton.setDisable(false);
		});

		new Thread(registerTask).start();
	}

	private void loadMainViewAndStartStreams(ClientRegisterResponse clientResponse) {
		try {
			ClientDTO clientInfo = clientResponse.getClient();
			SessionManager.getInstance().setClientInfo(clientInfo);
			Stage stage = (Stage) loginButton.getScene().getWindow();
			// Đảm bảo đường dẫn FXML đúng
			URL fxmlLocation = getClass().getResource("/com/pbl4/cameraclient/ui/view/MainView.fxml");
			if (fxmlLocation == null) {
				throw new IOException("Không tìm thấy file FXML: MainView.fxml");
			}
			FXMLLoader loader = new FXMLLoader(fxmlLocation);
			Parent mainViewRoot = loader.load();

			// Lấy controller của MainView
			MainViewController mainViewController = loader.getController();

			// Đặt controller này cho CameraService để nó có thể cập nhật UI
			cameraService.setUiController(mainViewController);

			// Yêu cầu CameraService bắt đầu kết nối tới các camera
			// CameraService sẽ tự tạo các luồng stream
			cameraService.connectToCameras(clientInfo, clientResponse.getCameras());

			mainViewController.displayClientInfo(clientResponse.getClient());
			mainViewController.setCameraInfoList(clientResponse.getCameras());

			// Hiển thị giao diện chính
			Scene mainScene = new Scene(mainViewRoot);
			stage.setScene(mainScene);
			stage.setTitle("Bảng điều khiển Camera");

			// QUAN TRỌNG: Dừng các luồng stream khi người dùng đóng cửa sổ
			stage.setOnCloseRequest(event -> {
				System.out.println("Cửa sổ chính đóng, dừng stream...");
				cameraService.stopAllStreams();
				Platform.exit(); // Thoát ứng dụng JavaFX một cách an toàn
				System.exit(0); // Đảm bảo tiến trình thoát hoàn toàn
			});

			stage.setResizable(true); // Cho phép thay đổi kích thước cửa sổ
			stage.centerOnScreen(); // Hiển thị cửa sổ giữa màn hình

		} catch (IOException e) {
			e.printStackTrace();
			statusLabel.setText("Lỗi: Không thể tải giao diện chính. " + e.getMessage());
			// Nếu lỗi xảy ra sau khi đã bắt đầu stream (ít khả năng), cần dừng lại
			cameraService.stopAllStreams();
			loginButton.setDisable(false); // Cho phép thử lại đăng nhập
		} catch (Exception e) {
			// Bắt các lỗi khác có thể xảy ra khi khởi tạo MainViewController
			e.printStackTrace();
			statusLabel.setText("Lỗi khởi tạo giao diện chính: " + e.getMessage());
			cameraService.stopAllStreams();
			loginButton.setDisable(false); // Cho phép thử lại đăng nhập
		}
	}
}