package com.pbl4.cameraclient.ui.controller;

import com.pbl4.cameraclient.model.AddCameraRequest;
import com.pbl4.cameraclient.model.CameraDTO;
import com.pbl4.cameraclient.model.DiscoveredCamera;
import com.pbl4.cameraclient.network.ApiClient;
import com.pbl4.cameraclient.service.CameraDiscoveryService;
import com.pbl4.cameraclient.service.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField; // THÊM IMPORT
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField; // THÊM IMPORT
import javafx.scene.layout.BorderPane;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.awt.Event;
import java.io.IOException;
import java.util.List;

public class AddCameraDialogController {

    @FXML private BorderPane dialogRootPane;
    @FXML private ListView<DiscoveredCamera> discoveredListView;
    @FXML private ProgressIndicator discoveryProgress;
    @FXML private Label statusScanLabel;
    @FXML private Button closeButton;

    // --- CÁC TRƯỜNG MỚI ĐƯỢC THÊM ---
    @FXML private TextField cameraNameField;
    @FXML private TextField ipAddressField;
    @FXML private TextField onvifUrlField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button addCameraButton;
    @FXML private Button rescanButton;// THAY THẾ cho manualAddButton
    
    
    // ---------------------------------
    private Task<List<DiscoveredCamera>> currentDiscoveryTask;

    private final CameraDiscoveryService discoveryService = new CameraDiscoveryService();
    private MainViewController mainViewController;
    
    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    @FXML
    public void initialize() {
        // Cấu hình ListView để hiển thị DiscoveredCamera
        discoveredListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DiscoveredCamera item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Dùng toString() của DiscoveredCamera (Giả sử nó đã được override)
                    setText(item.toString()); 
                }
            }
        });

        // --- TỰ ĐỘNG ĐIỀN KHI CHỌN CAMERA ---
        discoveredListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    populateFieldsFromDiscoveredCamera(newValue);
                }
            }
        );
        // -------------------------------------

        // --- XỬ LÝ NÚT THÊM CAMERA (THAY VÌ NÚT "THÊM THỦ CÔNG") ---
        addCameraButton.setOnAction(event -> {
            handleAddCamera();
        });
        rescanButton.setOnAction(event -> {
            stopDiscoveryTask(); // Hủy task cũ nếu đang chạy

            // Xóa danh sách cũ và hiển thị đang quét
            discoveredListView.setItems(null);
            statusScanLabel.setText("Đang quét lại camera...");
            statusScanLabel.setVisible(true);
            discoveryProgress.setVisible(true);

            // Gọi lại quét mới
            startDiscovery();
        });
        dialogRootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                    if (newWindow instanceof Stage) {
                        Stage stage = (Stage) newWindow;
                        // Khi bấm nút X trên thanh tiêu đề
                        stage.setOnCloseRequest(event -> {
                            System.out.println("Cửa sổ đóng bằng nút X -> Dừng quét.");
                            stopDiscoveryTask();
                        });
                    }
                });
            }
        });
        

        // -----------------------------------------------------
    }

    /**
     * Tự động điền thông tin vào các trường từ camera được phát hiện.
     */
    private void populateFieldsFromDiscoveredCamera(DiscoveredCamera camera) {
        if (camera != null) {
            ipAddressField.setText(camera.getIpAddress());
            
            // Giả sử model DiscoveredCamera có getOnvifUrl()
            // Dựa trên CameraDiscoveryService, đây là 'path' hoặc '/'
            // Nếu phương thức là getPath(), hãy thay đổi "getOnvifUrl()" thành "getPath()"
            onvifUrlField.setText(camera.getOnvifServiceUrl()); 
            
            // Đặt tên mặc định là IP nếu trường tên trống
            if (cameraNameField.getText() == null || cameraNameField.getText().trim().isEmpty()) {
                 cameraNameField.setText(camera.getIpAddress());
            }
        }
    }

    /**
     * Xử lý logic khi nhấn nút "Thêm camera".
     */
    private void handleAddCamera() {
        // 1. Lấy Client ID từ session
        int clientId;
        if (SessionManager.getInstance().getClientInfo() != null) {
            clientId = SessionManager.getInstance().getClientInfo().getId();
        } else {
            showError("Lỗi: Không tìm thấy thông tin Client. Vui lòng đăng nhập lại.");
            return;
        }

        // 2. Lấy dữ liệu từ form
        String name = cameraNameField.getText().trim();
        String ip = ipAddressField.getText().trim();
        String user = usernameField.getText();
        String pass = passwordField.getText(); // Mật khẩu không cần trim
        String onvif = onvifUrlField.getText().trim();

        if (name.isBlank() || ip.isBlank() ||  onvif.isBlank()) {
            showError("Tên, IP, và onvif không được để trống.");
            return;
        }

        // 3. Tạo Request DTO (DTO client gửi đi)
        AddCameraRequest request = new AddCameraRequest();
        request.setClientId(clientId);
        request.setCameraName(name);
        request.setIpAddress(ip);
        request.setUsername(user);
        request.setPassword(pass);
        request.setOnvifUrl(onvif);

        // 4. Gọi API trên luồng nền (background thread)
        setLoading(true, "Đang thêm camera...");

        Task<CameraDTO> saveTask = new Task<>() {
            @Override
            protected CameraDTO call() throws Exception {
                // Gọi API client (ném IOException nếu lỗi)
                return ApiClient.getInstance().addCamera(request);
            }
        };

        saveTask.setOnSucceeded(e -> {
            CameraDTO newCamera = saveTask.getValue();
            mainViewController.log("Thêm camera thành công: " + newCamera.getCameraName());
            
            // Yêu cầu MainView làm mới
            if (mainViewController != null) {
                mainViewController.addNewCameraToView(SessionManager.getInstance().getClientInfo(),newCamera);
            }
            
            handleCloseButton(null); // Đóng cửa sổ dialog
        });

        saveTask.setOnFailed(e -> {
            setLoading(false, "");
            Throwable ex = saveTask.getException();
            String errorMessage = "Lỗi không xác định";
            if (ex instanceof IOException) {
                errorMessage = ex.getMessage();
                // Phân tích lỗi từ server (ví dụ lỗi 409)
                if (errorMessage.contains("409")) {
                    showError("Lỗi: Camera (IP/User) này đã tồn tại trên hệ thống.");
                } else {
                    showError("Lỗi API: " + errorMessage);
                }
            } else {
                showError("Lỗi: " + ex.getMessage());
            }
            ex.printStackTrace();
        });

        new Thread(saveTask).start();
    }
    


    /**
     * Bắt đầu quá trình quét mạng (được gọi từ MainViewController).
     */
    public void startDiscovery() {
        // [MỚI] Đảm bảo task cũ đã dừng
        stopDiscoveryTask();

        rescanButton.setDisable(true);
        statusScanLabel.setVisible(true);
        discoveryProgress.setVisible(true);

        // [MỚI 2] Gán vào biến toàn cục thay vì biến cục bộ
        currentDiscoveryTask = discoveryService.createDiscoveryTask();

        currentDiscoveryTask.setOnSucceeded(e -> {
            List<DiscoveredCamera> cameras = currentDiscoveryTask.getValue();
            discoveredListView.setItems(FXCollections.observableArrayList(cameras));
            discoveryProgress.setVisible(false);
            statusScanLabel.setVisible(false);
            rescanButton.setDisable(false);
        });

        currentDiscoveryTask.setOnFailed(e -> {
            // Kiểm tra xem lỗi là do người dùng hủy hay lỗi thật
            if (!currentDiscoveryTask.isCancelled()) {
                statusScanLabel.setText("Lỗi khi quét: " + currentDiscoveryTask.getException().getMessage());
            } else {
                statusScanLabel.setText("Đã hủy quét.");
            }
            discoveryProgress.setVisible(false);
            rescanButton.setDisable(false);
        });

        new Thread(currentDiscoveryTask).start();
    }
    private void stopDiscoveryTask() {
        if (currentDiscoveryTask != null && currentDiscoveryTask.isRunning()) {
            System.out.println("Đang hủy task tìm kiếm camera...");
            currentDiscoveryTask.cancel();
        }
    }
    /**
     * Xử lý sự kiện đóng cửa sổ.
     */
    @FXML
    private void handleCloseButton(ActionEvent event) {
        // [MỚI] Gọi hàm dừng task
        stopDiscoveryTask();
        
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
    private void showError(String message) {
        Label errorLabel = new Label(message);
        errorLabel.setStyle("-fx-background-color: rgba(255,0,0,0.8); -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 8;");
        
        Popup popup = new Popup();
        popup.getContent().add(errorLabel);
        popup.setAutoHide(true);
        popup.show(statusScanLabel.getScene().getWindow());
    }
    /** Bật/tắt trạng thái loading */
    private void setLoading(boolean isLoading, String message) {
        discoveryProgress.setVisible(isLoading);
        statusScanLabel.setVisible(isLoading);
        if (isLoading) {
            statusScanLabel.setText(message);
        }
        // Vô hiệu hóa các nút khi đang tải
        addCameraButton.setDisable(isLoading);
        rescanButton.setDisable(isLoading);
        discoveredListView.setDisable(isLoading);
    }
}