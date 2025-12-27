package com.pbl4.cameraclient.ui.controller;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos; // Import Pos
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import com.pbl4.cameraclient.service.AuthService;
import com.pbl4.cameraclient.service.CameraService;
import com.pbl4.cameraclient.service.SessionManager;
import com.pbl4.cameraclient.service.SettingsService;
import com.pbl4.cameraclient.service.ClientService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.cameraclient.model.CameraDTO;
import com.pbl4.cameraclient.model.ClientDTO;
import com.pbl4.cameraclient.network.ApiClient;
import com.pbl4.cameraclient.network.AppConfig;
import com.pbl4.cameraclient.network.WebSocketManager;

public class MainViewController {

    // --- FXML Bindings ---
    @FXML private BorderPane rootPane;
    @FXML private HBox topHeader;
    @FXML private VBox leftSidebar;
    @FXML private Button addCameraButton;
    @FXML private Button selectSaveDirButton;
    @FXML private VBox mainContentContainer;
    @FXML private ScrollPane cameraScrollPane;
    @FXML private TilePane cameraDisplayPane;
    @FXML private VBox rightSidebar;
    @FXML
    private TextArea notificationArea;

    
    @FXML
    private Button emailButton1;

    @FXML
    private Button emailButton2;
    
    private final Map<Integer, ImageView> cameraImageViews = new HashMap<>();
    private final Map<Integer, VBox> cameraCards = new HashMap<>();
    private final Map<Integer, CameraDTO> cameraInfoMap = new HashMap<>();
    private final Map<Integer, Label> cameraNameLabels = new HashMap<>();
    private final Map<Integer, Button> cameraActionButtons = new HashMap<>();
    //
    private final Map<Integer, MenuButton> cameraMenuButtons = new HashMap<>();
    private final Map<Integer, MenuItem> cameraEditMenuItems = new HashMap<>();
    private final Map<Integer, MenuItem> cameraDeleteMenuItems = new HashMap<>();  
    private final Map<Integer, Boolean> cameraStreamingState = new HashMap<>();
    
    private final Map<Integer, ProgressIndicator> cameraLoadingIndicators = new HashMap<>();
    
    @FXML private Label clientNameLabel;
    @FXML private Label ipAddressLabel;
    @FXML private Label statusLabelRight; // Tên mới cho status label
    @FXML private Label imageSizeLabel;
    @FXML private Label intervalLabel;
    @FXML private Label qualityLabel;
    @FXML private Label lastHeartbeatLabel;
    @FXML 
    private Label emailLabel1;
    @FXML 
    private Label emailLabel2;

    // Kích thước mong muốn cho stream video
    private final int PREF_STREAM_WIDTH = 292;
    private final int PREF_STREAM_HEIGHT = 180;
    private Image placeholderImage = null;
    private Image cameraOfflineIcon = null;
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SettingsService settingsService = new SettingsService();
    private final CameraService cameraService = new CameraService();
    private ClientDTO currentClientInfo;
    private WebSocketManager webSocketManager;
    private String jwtToken;
    private HostServices hostServices;
    @FXML
    public void initialize() {
        System.out.println("MainViewController Initialized.");
        placeholderImage = createPlaceholderImage(PREF_STREAM_WIDTH, PREF_STREAM_HEIGHT, Color.rgb(220, 220, 220));

        cameraDisplayPane.setHgap(20);
        cameraDisplayPane.setVgap(20);

        setupEventHandlers();
        this.jwtToken = AuthService.getInstance().getJwtToken(); 

        if (this.jwtToken != null) {
            startWebSocket();
        } else {
            System.err.println("Chưa đăng nhập, không thể khởi động WebSocket.");
            // Có thể hiển thị lỗi hoặc yêu cầu đăng nhập
        }
    }

    private void handleSettingsClick() {
        System.out.println("Mở trang cài đặt...");
    }
    public void log(String message) {
        if (notificationArea != null) {
            String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            notificationArea.appendText("[" + time + "] " + message + "\n");
        }
    }

    private void setupEventHandlers() {
        addCameraButton.setOnAction(event -> {
            System.out.println("Nút 'Add Camera' đã được nhấn! Mở dialog...");
            openAddCameraDialog(); // Gọi hàm mở dialog
        });
        selectSaveDirButton.setOnAction(event -> {
            // Lấy cửa sổ (Stage) hiện tại từ rootPane
            Window ownerWindow = rootPane.getScene().getWindow();
            
            System.out.println("Mở cửa sổ chọn thư mục lưu ảnh...");

            // 1. Gọi hàm và lưu kết quả vào biến selectedPath
            String selectedPath = settingsService.askForSnapshotSavePath(ownerWindow);

            // 2. Kiểm tra kết quả và log
            if (selectedPath != null) {
                log("Đường dẫn lưu ảnh là: " + selectedPath);
            }
        });
        //
    }
    @FXML
    private void handleEmail1Click() {
        if (emailLabel1 != null) {
            // Lấy text trực tiếp từ Label trên giao diện
            String email = emailLabel1.getText();
            System.out.println("Nhấn nút email 1! Gửi tới: " + email);
            
            // Gọi hàm mở Outlook
            openEmailClient(email);
        } else {
            System.err.println("Lỗi: Chưa gán fx:id=\"emailLabel1\" cho Label trong file FXML!");
        }
    }

    @FXML
    private void handleEmail2Click() {
        if (emailLabel2 != null) {
            // Lấy text trực tiếp từ Label trên giao diện
            String email = emailLabel2.getText();
            System.out.println("Nhấn nút email 1! Gửi tới: " + email);
            
            // Gọi hàm mở Outlook
            openEmailClient(email);
        } else {
            System.err.println("Lỗi: Chưa gán fx:id=\"emailLabel1\" cho Label trong file FXML!");
        }
    }
    private void openEmailClient(String emailAddress) {
        System.out.println("Đang yêu cầu mở email client gửi tới: " + emailAddress);

        if (emailAddress == null || emailAddress.trim().isEmpty()) {
            System.err.println("Lỗi: Địa chỉ email trống.");
            return;
        }

        try {
            // 1. Kiểm tra xem máy tính có hỗ trợ tính năng Desktop không
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();

                // 2. Kiểm tra xem tính năng MAIL có được hỗ trợ không
                if (desktop.isSupported(Desktop.Action.MAIL)) {
                    
                    // 3. Tạo đường dẫn URI theo chuẩn "mailto:..."
                    String uriString = "mailto:" + emailAddress + "?subject=Support%20Request";
                    
                    // Chuyển đổi string sang URI object
                    URI mailto = new URI(uriString);

                    // 4. Mở trình gửi mail mặc định của hệ thống (Outlook, Mail, Thunderbird...)
                    desktop.mail(mailto);
                    
                } else {
                    System.err.println("Hệ thống không hỗ trợ hành động gửi mail (Desktop.Action.MAIL).");
                    showAlert("Lỗi", "Hệ thống không tìm thấy ứng dụng Email nào.");
                }
            } else {
                System.err.println("Java Desktop API không được hỗ trợ trên hệ điều hành này.");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi mở trình soạn thảo email: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở ứng dụng Email: " + e.getMessage());
        }
    }
    public void updateCameraView(int cameraId, Image image) {
        Platform.runLater(() -> {
            VBox card = cameraCards.get(cameraId);
            ImageView imageView = cameraImageViews.get(cameraId);
            Label nameLabel = cameraNameLabels.get(cameraId);
            Button actionButton = cameraActionButtons.get(cameraId);
            
            // Map Menu/Item
            MenuButton menuButton = cameraMenuButtons.get(cameraId);
            MenuItem editItem = cameraEditMenuItems.get(cameraId);
            MenuItem deleteItem = cameraDeleteMenuItems.get(cameraId);
            
            // Map Loading (MỚI)
            ProgressIndicator loadingIndicator = cameraLoadingIndicators.get(cameraId);

            CameraDTO cameraInfo = cameraInfoMap.get(cameraId);
            String cameraName = (cameraInfo != null) ? cameraInfo.getCameraName() : ("Camera " + cameraId);

            boolean isStreaming = (image != null);
            cameraStreamingState.put(cameraId, isStreaming);

            // --- 1. TẠO GIAO DIỆN NẾU CHƯA CÓ ---
            if (card == null) {
                System.out.println("Tạo Card mới (StackPane + Indicator) cho Camera ID: " + cameraId);

                // A. Tạo ImageView
                imageView = new ImageView();
                imageView.setFitWidth(PREF_STREAM_WIDTH);
                imageView.setFitHeight(PREF_STREAM_HEIGHT);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.getStyleClass().add("camera-image-view");

                // B. Tạo ProgressIndicator (Vòng xoay loading) - MỚI
                loadingIndicator = new ProgressIndicator();
                loadingIndicator.setMaxSize(40, 40); // Kích thước vòng xoay
                loadingIndicator.setVisible(false);  // Mặc định ẩn
                loadingIndicator.getStyleClass().add("camera-loading-indicator");

                // C. Tạo StackPane để chồng Loading lên trên Ảnh - MỚI
                StackPane imageContainer = new StackPane();
                imageContainer.setPrefSize(PREF_STREAM_WIDTH, PREF_STREAM_HEIGHT);
                imageContainer.getChildren().addAll(imageView, loadingIndicator);

                // D. Các thành phần khác (Label, Menu, Button) - GIỮ NGUYÊN
                nameLabel = new Label(cameraName);
                nameLabel.getStyleClass().add("camera-name-label");

                menuButton = new MenuButton("⋮");
                menuButton.getStyleClass().add("camera-menu-button");
                editItem = new MenuItem("Sửa thông tin...");
                deleteItem = new MenuItem("Xóa camera...");
                deleteItem.getStyleClass().add("delete-menu-item");
                menuButton.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);
                
                editItem.setOnAction(e -> handleEditCamera(cameraId));
                deleteItem.setOnAction(e -> handleDeleteCamera(cameraId));

                HBox topBar = new HBox(nameLabel, createSpacer(), menuButton);
                topBar.setAlignment(Pos.CENTER_LEFT);
                topBar.getStyleClass().add("camera-card-top-bar");

                actionButton = new Button();
                actionButton.getStyleClass().add("camera-action-button");
                actionButton.setMaxWidth(Double.MAX_VALUE);

                // E. Tạo VBox Card
                card = new VBox(5);
                card.getStyleClass().add("camera-card");
                card.setAlignment(Pos.CENTER);
                card.setFillWidth(true);
                // Lưu ý: Add imageContainer vào đây
                card.getChildren().addAll(topBar, imageContainer, actionButton);

                // F. Lưu vào Map
                cameraCards.put(cameraId, card);
                cameraImageViews.put(cameraId, imageView);
                cameraNameLabels.put(cameraId, nameLabel);
                cameraActionButtons.put(cameraId, actionButton);
                cameraMenuButtons.put(cameraId, menuButton);
                cameraEditMenuItems.put(cameraId, editItem);
                cameraDeleteMenuItems.put(cameraId, deleteItem);
                cameraLoadingIndicators.put(cameraId, loadingIndicator); // Lưu loading

                cameraDisplayPane.getChildren().add(card);

                if (image == null && placeholderImage != null) {
                    imageView.setImage(placeholderImage);
                }
            }
            if (isStreaming && loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }

            Image imageToShow = isStreaming ? image : placeholderImage;
            if (imageView != null && imageToShow != null) {
                imageView.setImage(imageToShow);
            }

            if (nameLabel != null) {
                nameLabel.setText(cameraName);
            }

            if (actionButton != null) {
                boolean currentlyLoading = (loadingIndicator != null && loadingIndicator.isVisible());

                if (!currentlyLoading) {
                    actionButton.setDisable(false);
                    if (isStreaming) {
                        actionButton.setText("Ngừng kết nối");
                        actionButton.getStyleClass().removeAll("reconnect-button");
                        if (!actionButton.getStyleClass().contains("disconnect-button")) {
                            actionButton.getStyleClass().add("disconnect-button");
                        }
                        actionButton.setOnAction(event -> handleDisconnectButton(cameraId));
                    } else {
                        actionButton.setText("Kết nối lại");
                        actionButton.getStyleClass().removeAll("disconnect-button");
                        if (!actionButton.getStyleClass().contains("reconnect-button")) {
                            actionButton.getStyleClass().add("reconnect-button");
                        }
                        actionButton.setOnAction(event -> handleReconnectButton(cameraId));
                    }
                }
            }
        }); 
    }
    
    public void displayClientInfo(ClientDTO clientInfo) {
    	this.currentClientInfo = clientInfo;
        if (clientInfo == null) {
            System.err.println("Lỗi: Không nhận được thông tin client (clientInfo is null).");
            // Có thể hiển thị "N/A" cho tất cả các label
            clientNameLabel.setText("N/A");
            ipAddressLabel.setText("N/A");
            statusLabelRight.setText("N/A");
            imageSizeLabel.setText("N/A");
            intervalLabel.setText("N/A");
            qualityLabel.setText("N/A");
            lastHeartbeatLabel.setText("N/A");
            return;
        }

        // Cập nhật các Label, kiểm tra null cho các giá trị
        clientNameLabel.setText(clientInfo.getClientName() != null ? clientInfo.getClientName() : "N/A");
        ipAddressLabel.setText(clientInfo.getIpAddress() != null ? clientInfo.getIpAddress() : "N/A");
        statusLabelRight.setText(clientInfo.getStatus() != null ? clientInfo.getStatus() : "N/A");

        String sizeText = "N/A";
        if (clientInfo.getImageWidth() != null && clientInfo.getImageHeight() != null) {
            sizeText = clientInfo.getImageWidth() + " x " + clientInfo.getImageHeight();
        }
        imageSizeLabel.setText(sizeText);

        intervalLabel.setText(clientInfo.getCaptureIntervalSeconds() + " s");
        qualityLabel.setText(clientInfo.getCompressionQuality() + "%");

        String heartbeatText = "N/A";
        if (clientInfo.getLastHeartbeat() != null) {
            try {
                heartbeatText = DATE_FORMAT.format(new Date(clientInfo.getLastHeartbeat().getTime()));
            } catch (Exception e) {
                 System.err.println("Lỗi định dạng lastHeartbeat: " + e.getMessage());
                 heartbeatText = clientInfo.getLastHeartbeat().toString();
            }
        }
        lastHeartbeatLabel.setText(heartbeatText);
    }
    private void handleDisconnectButton(int cameraId) {
    	Button actionButton = cameraActionButtons.get(cameraId);
        System.out.println("Yêu cầu ngừng kết nối Camera ID: " + cameraId);
        actionButton.setDisable(true);
        
        if (cameraService != null) {
            // Yêu cầu service dừng stream cho camera này
            // Bạn sẽ cần TỰ THÊM phương thức này vào CameraService
        	cameraService.setUiController(this);
            cameraService.disconnectFromCamera(cameraId);
        } else {
            System.err.println("Lỗi: CameraService là null, không thể ngắt kết nối.");
        }

        setCameraLoading(cameraId, false) ;
        updateCameraView(cameraId, null);
    }
    /**
     * Xử lý khi nhấn nút "Kết nối lại".
     */
    private void handleReconnectButton(int cameraId) {
        System.out.println("Yêu cầu kết nối lại Camera ID: " + cameraId);

        // 1. Kiểm tra ClientDTO đã được lưu
        if (this.currentClientInfo == null) {
            System.err.println("Lỗi: Không có thông tin client (currentClientInfo is null) để kết nối lại.");
            showAlert("Lỗi Kết Nối", "Không thể kết nối lại: Thiếu thông tin client.");
            return;
        }

        // 2. Lấy CameraDTO từ map
        CameraDTO cameraToReconnect = cameraInfoMap.get(cameraId);
        if (cameraToReconnect == null) {
            System.err.println("Lỗi: Không tìm thấy thông tin cho Camera ID: " + cameraId);
            showAlert("Lỗi Kết Nối", "Không thể kết nối lại: Không tìm thấy thông tin camera.");
            
            return;
        }

        // 3. Kiểm tra CameraService
        if (cameraService == null) {
            System.err.println("Lỗi: CameraService là null.");
            showAlert("Lỗi Hệ Thống", "Không thể kết nối lại: Dịch vụ camera chưa sẵn sàng.");
            return;
        }

        // 4. Tạo danh sách chỉ chứa camera này và gọi service
        List<CameraDTO> singleCameraList = new ArrayList<>();
        singleCameraList.add(cameraToReconnect);

        System.out.println("Bắt đầu kết nối lại với camera: " + cameraToReconnect.getCameraName());
        
        // Đảm bảo service biết controller nào đang gọi
        cameraService.setUiController(this);
        
        // Gọi hàm kết nối (giống như khi thêm camera mới)
        cameraService.connectToCameras(this.currentClientInfo, singleCameraList);
    }
    public void setCameraLoading(int cameraId, boolean isLoading) {
        // 1. Bước 1: Đảm bảo Card tồn tại
        if (!cameraCards.containsKey(cameraId)) {
            updateCameraView(cameraId, null); 
            // updateCameraView chạy bất đồng bộ (Async), nên lệnh này chỉ "gửi yêu cầu tạo card" 
            // chứ chưa tạo xong ngay lập tức tại dòng này.
        }

        // 2. Bước 2: Đợi Card tạo xong thì mới set trạng thái (dùng runLater để xếp hàng sau bước 1)
        Platform.runLater(() -> {
            ProgressIndicator indicator = cameraLoadingIndicators.get(cameraId);
            Button btn = cameraActionButtons.get(cameraId);

            // Lúc này chắc chắn Card đã có (vì lệnh này chạy sau lệnh tạo ở trên)
            
            if (indicator != null) {
                indicator.setVisible(isLoading);
            }

            if (btn != null) {
                if (isLoading) {
                    // FORCE SET (Ghi đè bất chấp trạng thái cũ)
                    btn.setText("Đang kết nối...");
                    btn.setDisable(true); 
                    
                    // Xóa các class cũ để tránh xung đột giao diện
                    btn.getStyleClass().removeAll("reconnect-button", "disconnect-button");
                } else {
                    // Nếu tắt loading -> Mở lại nút
                    btn.setDisable(false);
                    // Gọi lại updateCameraView một lần nữa để nó tự check trạng thái stream 
                    // và quyết định hiện "Kết nối lại" hay "Ngắt kết nối"
                    updateCameraView(cameraId, null); 
                }
            }
        });
    }
    

    // --- Các hàm tiện ích (giữ nguyên) ---
    private Image createPlaceholderImage(int width, int height, Color color) {
        WritableImage img = new WritableImage(width, height);
        PixelWriter pw = img.getPixelWriter();
        int targetColor = colorToInt(color);
        for (int y = 0; y < height; y++) { for (int x = 0; x < width; x++) { pw.setArgb(x, y, targetColor); } }
        return img;
    }
    private int colorToInt(Color c) {
        int r = (int) (c.getRed() * 255); int g = (int) (c.getGreen() * 255); int b = (int) (c.getBlue() * 255); int a = (int) (c.getOpacity() * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    private void openAddCameraDialog() {
        try {
            // 1. Tải file FXML của Dialog
            URL fxmlLocation = getClass().getResource("/com/pbl4/cameraclient/ui/view/AddCameraDialog.fxml");
            if (fxmlLocation == null) {
                System.err.println("Không tìm thấy file FXML: AddCameraDialog.fxml");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent dialogRoot = loader.load();
            
            // 2. Lấy controller của Dialog
            AddCameraDialogController dialogController = loader.getController();
            dialogController.setMainViewController(this);

            // 3. Tạo một Stage (cửa sổ) mới cho Dialog
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Thêm Camera Mới");
            
            // 4. Đặt làm Modal (chặn tương tác với cửa sổ chính)
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            // Đặt cửa sổ chính (MainView) làm cửa sổ cha
            dialogStage.initOwner(rootPane.getScene().getWindow()); 

            // 5. Đặt Scene và hiển thị
            Scene scene = new Scene(dialogRoot);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false); // Không cho phép resize
            
            // 6. Hiển thị cửa sổ và BẮT ĐẦU quét
            dialogStage.show(); // Hiển thị
            dialogController.startDiscovery(); // Yêu cầu controller bắt đầu quét

        } catch (IOException e) {
            System.err.println("Lỗi khi mở cửa sổ AddCameraDialog: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void addNewCameraToView(ClientDTO clientInfo, CameraDTO newCamera) {
        if (newCamera == null) return;

        System.out.println("MainView nhận được camera mới: " + newCamera.getCameraName());

        // 1. Thêm camera mới vào Map thông tin
        cameraInfoMap.put(newCamera.getId(), newCamera);

        // 2. Yêu cầu CameraService bắt đầu stream cho camera mới
        if (cameraService != null) {
            // Tạo danh sách chỉ chứa camera mới
            List<CameraDTO> newCameraList = new ArrayList<>();
            newCameraList.add(newCamera);

            // Gọi hàm connectToCameras với danh sách
            cameraService.setUiController(this);
            cameraService.connectToCameras(clientInfo, newCameraList);
        } else {
            System.err.println("Lỗi: CameraService là null trong MainViewController.");
        }
    }
    private void showAlert(String title, String content) {
        // Đảm bảo chạy trên JavaFX Application Thread
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null); // Không hiển thị header
            alert.setContentText(content);
            
            // Đặt cửa sổ chủ để alert hiển thị đúng vị trí
            if (rootPane != null && rootPane.getScene() != null) {
                alert.initOwner(rootPane.getScene().getWindow());
            }
            
            alert.showAndWait();
        });
    }
    public void setCameraInfoList(List<CameraDTO> cameraList) {
        if (cameraList == null || cameraList.isEmpty()) {
            System.out.println("Danh sách camera trống hoặc null — không cập nhật map.");
            return;
        }

        // Xóa dữ liệu cũ (nếu bạn muốn giữ thì bỏ dòng này)
        cameraInfoMap.clear();

        // Duyệt danh sách và thêm vào map với key là id
        for (CameraDTO camera : cameraList) {
            if (camera != null) {
                cameraInfoMap.put(camera.getId(), camera);
            }
        }

        // Log kết quả
        System.out.println("Đã cập nhật " + cameraInfoMap.size() + " camera vào cameraInfoMap.");
    }
    //
    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Cho phép spacer co giãn
        return spacer;
    }
    private void handleEditCamera(int cameraId) {
        System.out.println("Yêu cầu SỬA thông tin Camera ID: " + cameraId);
        
        // --- 1. KIỂM TRA: Đang Stream (Có hình ảnh) ---
        boolean isStreaming = cameraStreamingState.getOrDefault(cameraId, false);
        if (isStreaming) {
            showAlert("Không thể Sửa", 
                      "Camera đang hoạt động. Vui lòng ngắt kết nối trước khi sửa.");
            return;
        }

        // --- 2. KIỂM TRA: Đang Kết nối (Loading) [MỚI] ---
        // Lấy indicator từ Map
        ProgressIndicator loadingIndicator = cameraLoadingIndicators.get(cameraId);
        // Nếu indicator tồn tại và đang hiện -> Tức là đang kết nối
        boolean isConnecting = (loadingIndicator != null && loadingIndicator.isVisible());
        
        if (isConnecting) {
            showAlert("Không thể Sửa", 
                      "Camera đang trong quá trình kết nối. Vui lòng đợi hoặc hủy kết nối.");
            return;
        }

        // --- 3. LOGIC MỞ DIALOG (Giữ nguyên) ---
        CameraDTO cameraInfo = cameraInfoMap.get(cameraId);
        if (cameraInfo == null) {
            showAlert("Lỗi", "Không tìm thấy thông tin camera để sửa.");
            return;
        }

        try {
            URL fxmlLocation = getClass().getResource("/com/pbl4/cameraclient/ui/view/EditCameraDialog.fxml");
            if (fxmlLocation == null) {
                 System.err.println("Không tìm thấy file FXML: EditCameraDialog.fxml");
                 showAlert("Lỗi Giao Diện", "Không thể tải file EditCameraDialog.fxml");
                 return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent dialogRoot = loader.load();
            
            // Lấy controller và truyền dữ liệu
            EditCameraDialogController dialogController = loader.getController();
            dialogController.initData(cameraInfo); // Truyền object camera vào để binding dữ liệu cũ
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Sửa thông tin Camera");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(rootPane.getScene().getWindow());
            Scene scene = new Scene(dialogRoot);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            
            dialogStage.showAndWait(); // Hiển thị và chờ người dùng thao tác xong

        } catch (IOException e) {
            System.err.println("Lỗi khi mở cửa sổ Sửa Camera: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi", "Đã xảy ra lỗi khi mở cửa sổ chỉnh sửa.");
        }
    }
    private void handleDeleteCamera(int cameraId) {
        System.out.println("Yêu cầu XÓA Camera ID: " + cameraId);
        CameraDTO cameraInfo = cameraInfoMap.get(cameraId);
        String cameraName = (cameraInfo != null) ? cameraInfo.getCameraName() : ("Camera " + cameraId);

        // 1. HIỂN THỊ HỘP THOẠI XÁC NHẬN
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận Xóa Camera");
        alert.setHeaderText("Bạn có chắc chắn muốn xóa camera này?");
        alert.setContentText(cameraName + "\n\nLưu ý: Hành động này không thể hoàn tác.");

        if (rootPane != null && rootPane.getScene() != null) {
            alert.initOwner(rootPane.getScene().getWindow());
        }

        // 2. CHỜ NGƯỜI DÙNG XÁC NHẬN
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // 3. NẾU ĐỒNG Ý -> GỌI API TRONG THREAD MỚI
                System.out.println("Người dùng xác nhận xóa Camera ID: " + cameraId);
                
                new Thread(() -> {
                    try {
                        // Gọi API Client (Sẽ tạo ở bước 2)
                    	cameraService.requestDeleteCamera(cameraId);
                        
                        // Server sẽ gửi WebSocket sau khi xóa thành công
                        // Client không cần làm gì thêm ở đây.
                        
                    } catch (IOException e) {
                        System.err.println("Lỗi khi gửi yêu cầu xóa: " + e.getMessage());
                        // Hiển thị lỗi cho người dùng (trên luồng UI)
                        Platform.runLater(() -> {
                            showAlert("Xóa Thất Bại", "Đã xảy ra lỗi khi gửi yêu cầu xóa camera: " + e.getMessage());
                        });
                    }
                }).start();
            }
        });
    }
    private void startWebSocket() {
    	// Lấy địa chỉ server từ AppConfig
    	String serverIp = AppConfig.getServerIp();
    	int serverPort = AppConfig.getServerPort();
    	String url = "ws://" + serverIp + ":" + serverPort + "/ws/updates";
        // URL này phải khớp với cấu hình server (Bước 2)

        try {
			webSocketManager = new WebSocketManager(url, this.jwtToken,ClientService.getStableMacAddress());
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        // Đặt callback xử lý tin nhắn
        webSocketManager.setOnMessageReceived(message -> {
            // Tin nhắn này từ server (ví dụ: "SETTINGS_UPDATED")
            // Chúng ta cần chạy trên luồng JavaFX
            Platform.runLater(() -> {
                handleWebSocketMessage(message);
            });
        });

        // Bắt đầu kết nối
        webSocketManager.connect();
    }
    private void handleWebSocketMessage(String jsonMessage) {
        try {
            // ObjectMapper này nên được tạo 1 lần và tái sử dụng
            ObjectMapper objectMapper = new ObjectMapper(); 
            
            // THÊM LOG NÀY:
            System.out.println("MainView: Đang xử lý tin nhắn: " + jsonMessage);

            Map<String, Object> msg = objectMapper.readValue(jsonMessage, Map.class);
            String type = (String) msg.get("type");

            // THÊM LOG NÀY:
            System.out.println("MainView: Nhận được type: '" + type + "'");

            if ("SETTINGS_UPDATED".equals(type)) {
                // ... (logic của bạn)
            } 
            else if ("CAMERA_UPDATED".equals(type)) { // Đảm bảo chuỗi này khớp 100%
                
                // THÊM LOG NÀY:
                System.out.println("MainView: Đã nhận diện CAMERA_UPDATED!");
                
                CameraDTO updatedCamera = objectMapper.convertValue(msg.get("camera"), CameraDTO.class);

                if (updatedCamera != null) {
                    System.out.println("MainView: Đang cập nhật camera ID: " + updatedCamera.getId());
                    
                    cameraInfoMap.put(updatedCamera.getId(), updatedCamera);
                    updateCameraView(updatedCamera.getId(), null);
                    log("Camera '" + updatedCamera.getCameraName() + "' đã được cập nhật.");
                    System.out.println("MainView: Đã cập nhật xong.");
                } else {
                    System.err.println("MainView: Lỗi! Dữ liệu 'camera' trong tin nhắn bị null.");
                }
            }
            else if ("CAMERA_DELETED".equals(type)) {
                System.out.println("MainView: Nhận được CAMERA_DELETED!");
                
                // Lấy ID từ tin nhắn
                Integer cameraId = (Integer) msg.get("id");

                if (cameraId != null) {
                	log("Đã xóa camera ID: " + cameraId);
                    // Chạy logic xóa trên luồng UI
                    Platform.runLater(() -> {
                        removeCameraFromUI(cameraId);
                    });
                }
            }
            else if ("CONFIG_UPDATE".equals(type)) {
            	System.out.println("MainView: Nhận được CONFIG_UPDATE!");
                
                // 1. Phân tích đối tượng "config" từ tin nhắn
                ClientDTO newConfig = objectMapper.convertValue(msg.get("config"), ClientDTO.class);

                if (newConfig != null) {
                    // 2. Chạy logic cập nhật trên luồng JavaFX
                    Platform.runLater(() -> {
                        applyNewConfiguration(newConfig);
                    });
                }
            }
            // ... (Xử lý các loại tin nhắn khác) ...

        }
        catch (Exception e) {
            System.err.println("Lỗi xử lý tin nhắn WebSocket trên MainView: " + e.getMessage());
            e.printStackTrace(); // <-- Rất quan trọng, hãy xem có lỗi gì ở đây không
        }
    }
    private void removeCameraFromUI(int cameraId) {
        System.out.println("Đang xóa Camera ID: " + cameraId + " khỏi giao diện.");
        
        // 1. Dừng luồng stream (nếu đang chạy)
        // Hàm này sẽ tự kiểm tra và dừng stream
        if (cameraService != null) {
            cameraService.stopStream(cameraId);
        }

        // 2. Xóa card khỏi giao diện
        VBox card = cameraCards.remove(cameraId);
        if (card != null) {
            cameraDisplayPane.getChildren().remove(card);
        }

        // 3. Dọn dẹp tất cả các Map để tránh rò rỉ bộ nhớ
        cameraInfoMap.remove(cameraId);
        cameraStreamingState.remove(cameraId);
        cameraImageViews.remove(cameraId);
        cameraNameLabels.remove(cameraId);
        cameraActionButtons.remove(cameraId);
        cameraMenuButtons.remove(cameraId);
        cameraEditMenuItems.remove(cameraId);
        cameraDeleteMenuItems.remove(cameraId);
    }
    private void applyNewConfiguration(ClientDTO newConfig) {
        System.out.println("Đang áp dụng cài đặt client mới...");

        // 1. Cập nhật cache của SessionManager (trung tâm)
        SessionManager.getInstance().setClientInfo(newConfig);
        
        // 2. Cập nhật cache của MainViewController (nếu có)
        this.currentClientInfo = newConfig;

        // 3. Cập nhật các Label trên UI (sidebar bên phải)
        // (Hàm này bạn đã có, dùng để hiển thị thông tin client)
        displayClientInfo(newConfig);

        // 4. Thông báo cho CameraService để cập nhật các luồng đang chạy
        if (cameraService != null) {
            // (Chúng ta sẽ tạo hàm này ở bước 2)
            cameraService.updateStreamerConfigurations(newConfig);
        }

        // 5. Hiển thị thông báo thành công cho người dùng
        log("Đã nhận và áp dụng cài đặt client mới từ server");
    }
    public Map<Integer, CameraDTO> getCameraInfoMap() {
        return this.cameraInfoMap;
    }

    /**
     * Gọi hàm này khi ứng dụng tắt
     */
    public void shutdown() {
        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
    }
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
    
    @FXML
    private void handleAccountSettings() {
        System.out.println("Mở cài đặt tài khoản...");
    }

    @FXML
    private void handleLogout() {
        System.out.println("Yêu cầu đăng xuất...");

        // 1. Hiển thị hộp thoại xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận đăng xuất");
        alert.setHeaderText(null);
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?");
        
        // Đặt cửa sổ cha để alert hiện đúng chỗ
        if (rootPane != null && rootPane.getScene() != null) {
            alert.initOwner(rootPane.getScene().getWindow());
        }

        // 2. Xử lý khi người dùng bấm OK
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                performLogout();
            }
        });
    }

    private void performLogout() {
        System.out.println("Đang thực hiện đăng xuất hệ thống...");

        // --- BƯỚC 1: DỌN DẸP TÀI NGUYÊN ---
        
        // 1. Dừng tất cả các luồng camera đang chạy
        if (cameraService != null) {
            cameraService.stopAllStreams();
        }

        // 2. Ngắt kết nối WebSocket
        shutdown(); 

        // 3. Xóa dữ liệu phiên làm việc (Token, User Info)
        // Bạn cần đảm bảo AuthService có hàm xóa token (ví dụ: logout() hoặc setToken(null))
        AuthService.getInstance().setJwtToken(null); 
        SessionManager.getInstance().setClientInfo(null);
        this.currentClientInfo = null;
        this.jwtToken = null;

        // --- BƯỚC 2: CHUYỂN HƯỚNG VỀ MÀN HÌNH LOGIN ---
        try {
            // Đường dẫn đến file FXML đăng nhập (Bạn kiểm tra lại tên file chính xác nhé)
            URL fxmlLocation = getClass().getResource("/com/pbl4/cameraclient/ui/view/LoginView.fxml");
            
            if (fxmlLocation == null) {
                System.err.println("Lỗi: Không tìm thấy file LoginView.fxml");
                showAlert("Lỗi", "Không tìm thấy giao diện đăng nhập để quay lại.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent loginRoot = loader.load();

            // Lấy Stage (cửa sổ) hiện tại
            Stage stage = (Stage) rootPane.getScene().getWindow();
            
            // Tạo Scene mới
            Scene loginScene = new Scene(loginRoot);
            
            // Cập nhật Stage
            stage.setScene(loginScene);
            stage.setTitle("Đăng nhập - Camera Client"); // Đặt lại tiêu đề
            stage.centerOnScreen(); // Căn giữa màn hình
            stage.show();
            
            System.out.println("Đăng xuất thành công. Đã trở về màn hình đăng nhập.");

        } catch (IOException e) {
            System.err.println("Lỗi khi tải màn hình đăng nhập: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi Đăng Xuất", "Không thể tải màn hình đăng nhập: " + e.getMessage());
        }
    }
}