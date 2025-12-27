package com.pbl4.cameraclient.service;

import com.pbl4.cameraclient.camera.CameraStreamer;
import com.pbl4.cameraclient.network.*;
import com.pbl4.cameraclient.model.CameraDTO;
import com.pbl4.cameraclient.model.ClientDTO;
import com.pbl4.cameraclient.ui.controller.MainViewController; // Import MainViewController
import javafx.application.Platform; // Cần thiết nếu gọi updateCameraView trực tiếp từ đây
import javafx.scene.image.Image; // Import JavaFX Image

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class CameraService {
	private static final Map<Integer, CameraStreamer> activeStreamers = new ConcurrentHashMap<>();
	// Lưu trữ các thread để quản lý chúng
	private final Map<Integer, Thread> activeThreads = new ConcurrentHashMap<>();

	// Lưu một tham chiếu tới MainViewController
	private MainViewController uiController;
	private final ApiClient apiClient = ApiClient.getInstance();
	private final ExecutorService apiExecutor = Executors.newCachedThreadPool();

	private static final int PING_TIMEOUT_MS = 1000;
	private static final int TCP_CONNECT_TIMEOUT_MS = 1500; // Timeout cho kết nối TCP (1.5 giây)
	private static final int RTSP_PORT = 554;

	private ExecutorService connectionPool;
	private static final int CONCURRENT_CONNECTIONS = 3;
	private static final int count = 0;

	private final ImageService imageUploadService = new ImageService();
	private Semaphore connectionPermits;
	private static final int DEFAULT_PERMITS = 3;
//	private final Semaphore connectionPermits = new Semaphore(CONCURRENT_CONNECTIONS);
	
		

	/**
	 * Đặt UI controller sẽ nhận các cập nhật khung hình.
	 */
	public void setUiController(MainViewController controller) {
		this.uiController = controller;
	}

	public void connectToCameras(ClientDTO clientInfo, List<CameraDTO> cameras) {
		if (this.uiController == null) {
			System.err.println("!!! UI Controller chưa được đặt...");
			return;
		}

		if (cameras == null || cameras.isEmpty()) {
			System.out.println("Không tìm thấy camera nào.");
			return;
		}
		final int width = (clientInfo.getImageWidth() != null && clientInfo.getImageWidth() > 0)
				? clientInfo.getImageWidth()
				: 1280;
		final int height = (clientInfo.getImageHeight() != null && clientInfo.getImageHeight() > 0)
				? clientInfo.getImageHeight()
				: 720;
		final int streamFrameRate = 15;
		final int captureInterval = (clientInfo.getCaptureIntervalSeconds() > 0)
				? clientInfo.getCaptureIntervalSeconds()
				: 5;
		final int compressionQuality = (clientInfo.getCompressionQuality() > 0
				&& clientInfo.getCompressionQuality() <= 100) ? clientInfo.getCompressionQuality() : 85;

		System.out.println("Đã lập lịch kết nối cho " + cameras.size() + " camera (Tối đa " + CONCURRENT_CONNECTIONS
				+ " khởi tạo cùng lúc)...");
		int optimalPermits = DEFAULT_PERMITS; // Mặc định là 3

        if (cameras.size() > 5) {
            // Nếu số lượng camera lớn, ta cần tính toán để mở rộng luồng
            // nhằm giảm thời gian chờ đợi của người dùng.
            System.out.println("Số lượng camera lớn (>5). Đang tính toán tài nguyên hệ thống...");
            optimalPermits = calculateOptimalThreadCount();
        }
        System.out.println("-> Số luồng khởi tạo đồng thời được chốt là: " + optimalPermits);
        this.connectionPermits = new Semaphore(optimalPermits);	
		

		String machineIdTemp = null;
		try {
			machineIdTemp = ClientService.getStableMacAddress();
		} catch (Exception e) {
		}
		final String machineId = machineIdTemp;

		Thread dispatcherThread = new Thread(() -> {
			System.out.println("--> [Dispatcher] Bắt đầu phân phối camera...");

			for (CameraDTO camera : cameras) {
				try {
					// 2. Xin giấy phép. Nếu đã đủ 3 người đang khởi tạo, dòng này sẽ CHỜ (BLOCK)
					// cho đến khi có người trả giấy phép.
					connectionPermits.acquire();

					// 3. Tạo callback để trả giấy phép khi xong
					Runnable onInitDone = () -> {
						System.out.println(
								"<- [Dispatcher] Camera ID " + camera.getId() + " xong giai đoạn khởi tạo. Trả slot.");
						connectionPermits.release();
					};

					// 4. Bắt đầu xử lý camera này
					uiController.setCameraLoading(camera.getId(), true);
					startSingleCamera(camera, width, height, streamFrameRate, captureInterval, compressionQuality,
							machineId, onInitDone);

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println("Luồng điều phối bị ngắt!");
					break;
				}
			}
			System.out.println("--> [Dispatcher] Đã phân phối xong tất cả yêu cầu.");
		});

		dispatcherThread.setName("Camera-Dispatcher");
		dispatcherThread.setDaemon(true);
		dispatcherThread.start();
	}

	private void startSingleCamera(CameraDTO camera, int width, int height, int fps, int interval, int quality,
			String machineId, Runnable onInitDone) {
		final int currentCameraId = camera.getId();
		String cameraIp = camera.getIpAddress();
		String urlTemplate = camera.getOnvifUrl();

		System.out.println("-> [Start] Bắt đầu xử lý Camera ID: " + currentCameraId);

		if (cameraIp == null || cameraIp.trim().isEmpty()) {
			System.err.println("!!! Bỏ qua camera ID: " + currentCameraId + " (Thiếu IP)");
			updateUiOffline(currentCameraId);
			onInitDone.run(); 
			return;
		}
		boolean isOnline = NetworkUtils.isCameraReachable(cameraIp, urlTemplate, 1500);

		if (!isOnline) {
			System.err.println("!!! Camera Offline (TCP Check Fail): ID " + currentCameraId);
			updateUiOffline(currentCameraId);
			reportCameraStatus(currentCameraId, false, machineId);
			onInitDone.run();
			return;
		}

		System.out.println("-> Camera Online: ID " + currentCameraId + ". Tạo luồng Streamer...");

		Consumer<Image> consumer = (Image frame) -> {
			if (!activeStreamers.containsKey(currentCameraId)) {
				return;
			}

			if (uiController != null) {
				if (frame == null) {
					uiController.setCameraLoading(currentCameraId, false);
				}
				uiController.updateCameraView(currentCameraId, frame);
			}
		};

		CameraStreamer streamer = new CameraStreamer(camera, consumer, width, height, fps, interval, quality,
				this.imageUploadService, this, machineId, onInitDone
		);

		activeStreamers.put(camera.getId(), streamer);

		Thread cameraThread = new Thread(streamer, "Streamer-" + camera.getId());
		cameraThread.setDaemon(true);
		cameraThread.setPriority(Thread.NORM_PRIORITY - 1);

		activeThreads.put(camera.getId(), cameraThread);

		cameraThread.start();
	}

	private void updateUiOffline(int cameraId) {
		if (uiController != null) {
			uiController.setCameraLoading(cameraId, false);
			Platform.runLater(() -> uiController.updateCameraView(cameraId, null));
		}
	}

	public void disconnectFromCamera(int cameraId) {
		System.out.println("Nhận yêu cầu ngắt kết nối cho Camera ID: " + cameraId);

		// 1. Kiểm tra xem streamer có thực sự tồn tại không
		if (!activeStreamers.containsKey(cameraId)) {

			System.err.println("Không tìm thấy streamer đang hoạt động cho ID: " + cameraId + ". Có thể đã dừng.");
			// Đảm bảo UI được cập nhật về trạng thái offline
			if (uiController != null) {
				Platform.runLater(() -> {
					uiController.setCameraLoading(cameraId, false);
					uiController.updateCameraView(cameraId, null);
				});
			}
			return;
		}

		// 2. Lấy machineId để báo cáo
		String machineId = null;
		try {
			// Lấy machineId giống như cách làm trong 'connectToCameras'
			machineId = ClientService.getStableMacAddress();
		} catch (Exception e) {
			System.err.println("Không thể lấy MAC address để báo cáo trạng thái dừng: " + e.getMessage());
			// Vẫn tiếp tục, machineId có thể là null khi báo cáo
		}

		// 3. Dừng stream (hàm này xóa khỏi maps và dừng thread)
		stopStream(cameraId);

		// 4. Báo cáo trạng thái (isActive = false) cho API
		// Hàm reportCameraStatus đã chạy trong ExecutorService riêng
		reportCameraStatus(cameraId, false, machineId);

		// 5. Cập nhật UI để hiển thị placeholder (offline)
		if (uiController != null) {
			// Yêu cầu UI cập nhật, đảm bảo nó chạy trên luồng UI
			Platform.runLater(() -> {
				uiController.setCameraLoading(cameraId, false);
				uiController.updateCameraView(cameraId, null);
			});
		} else {
			System.err.println("uiController là null, không thể cập nhật UI sau khi ngắt kết nối.");
		}

		System.out.println("Đã hoàn tất ngắt kết nối cho Camera ID: " + cameraId);
	}

	/**
	 * Dừng một luồng stream camera cụ thể bằng ID của nó.
	 */
	public void stopStream(int cameraId) {
		CameraStreamer streamer = activeStreamers.remove(cameraId);
		Thread thread = activeThreads.remove(cameraId);
		if (streamer != null) {
			streamer.stopStreaming(); // Gửi tín hiệu yêu cầu streamer dừng lại
			System.out.println("Đã dừng stream cho Camera ID: " + cameraId);
		}
		if (thread != null) {
			try {
				thread.join(5000); // Chờ một chút để luồng kết thúc
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.err.println("Bị gián đoạn khi chờ luồng stream dừng: " + cameraId);
			}
		}
	}

	public void reportCameraStatus(int cameraId, boolean isActive, String machineId) {
		apiExecutor.submit(() -> {
			try {
				apiClient.updateCameraStatus(cameraId, machineId, isActive);
			} catch (IOException e) {
				System.err.println("Lỗi API khi báo cáo trạng thái camera " + cameraId + ": " + e.getMessage());
			}
		});
	}
	public void updateStreamerConfigurations(ClientDTO newConfig) {
		System.out.println("CameraService: Cập nhật cấu hình cho " + activeStreamers.size() + " streamer(s)...");

		if (activeStreamers.isEmpty()) {
			System.out.println("Không có streamer nào đang chạy để cập nhật.");
			return;
		}
		CameraStreamer anyStreamer = activeStreamers.values().iterator().next();
		boolean heavyChange = (newConfig.getImageWidth() != anyStreamer.getCurrentWidth())
				|| (newConfig.getImageHeight() != anyStreamer.getCurrentHeight());
		if (heavyChange) {
			System.out.println("Phát hiện thay đổi nặng (độ phân giải). Khởi động lại tất cả các luồng.");
			List<CameraDTO> allCameras = new ArrayList<>(uiController.getCameraInfoMap().values());
			connectToCameras(newConfig, allCameras);

		} else {
			System.out.println("Phát hiện thay đổi nhẹ (interval/quality). Cập nhật nóng.");
			for (CameraStreamer streamer : activeStreamers.values()) {
				streamer.updateLightConfiguration(newConfig);
			}
		}
	}

	/**
	 * Dừng tất cả các luồng stream camera đang hoạt động.
	 */
	public void stopAllStreams() {
		System.out.println("Đang dừng tất cả các luồng stream...");
		// Tạo bản sao của keys để tránh ConcurrentModificationException
		List<Integer> cameraIds = new ArrayList<>(activeStreamers.keySet());
		for (Integer cameraId : cameraIds) {
			stopStream(cameraId);
		}
		activeStreamers.clear();
		activeThreads.clear();
		System.out.println("Tất cả luồng stream đã dừng.");
	}

	public void requestDeleteCamera(int cameraId) throws IOException {
		// Hàm này chỉ đơn giản là gọi qua lớp ApiClient.
		// Nó che giấu việc "làm thế nào" (how) một camera bị xóa khỏi UI.
		ApiClient.getInstance().deleteCamera(cameraId);
	}
	public int calculateOptimalThreadCount() {
	    int cpuCores = Runtime.getRuntime().availableProcessors();
	    
	    // Đo băng thông (Hàm này tốn khoảng 1-2 giây để chạy)
	    double bandwidth = NetworkUtils.measureBandwidthMbps();
	    System.out.println("Băng thông đo được: " + bandwidth + " Mbps");

	    if (bandwidth > 50.0) { 
	        // Mạng khỏe (> 50Mbps): Max tốc độ
	        return Math.min(cpuCores * 2, 12);
	    } else if (bandwidth > 10.0) {
	        // Mạng trung bình (10-50Mbps): Vừa phải
	        return Math.min(cpuCores, 6);
	    } else {
	        // Mạng yếu (< 10Mbps): Rất chậm
	        return 2; 
	    }
	}

}
