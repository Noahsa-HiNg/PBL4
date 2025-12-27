package com.pbl4.cameraclient.camera;

import com.pbl4.cameraclient.model.CameraDTO;
import com.pbl4.cameraclient.model.ClientDTO;
import com.pbl4.cameraclient.service.CameraService;
import com.pbl4.cameraclient.service.ImageService; // <-- THÊM IMPORT
import com.pbl4.cameraclient.service.SettingsService;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
// import org.bytedeco.opencv.opencv_core.Mat; // Bỏ import nếu không dùng Mat

import java.awt.image.BufferedImage; // <-- THÊM IMPORT
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

public class CameraStreamer implements Runnable {

	private final CameraDTO camera;
	private final Consumer<Image> frameConsumer;
	private final AtomicBoolean running = new AtomicBoolean(true);
	private static final long START_TIMEOUT_SECONDS = 90;
	private FFmpegFrameGrabber grabber = null;
	private volatile boolean connectionSuccessful = false;
	private final CameraService cameraService;// Thêm volatile
	private String machineId;

	private volatile int imageWidth;
	private volatile int imageHeight;
	private volatile int frameRate;
	private volatile int captureIntervalSeconds;
	private volatile int compressionQuality;
	private final ImageService imageUploadService;
	private long lastSnapshotTime = 0;
	private final Runnable onInitializationFinished;

	/**
	 * Constructor MỚI: Nhận thêm thông số cài đặt snapshot.
	 */
	public CameraStreamer(CameraDTO camera, Consumer<Image> frameConsumer, int width, int height, int fps,
			int captureInterval, int quality, // Thêm 2
			ImageService uploadService, CameraService cameraService, String machineId,
			Runnable onInitializationFinished) { // Thêm 1
		this.camera = camera;
		this.frameConsumer = frameConsumer;
		this.imageWidth = width;
		this.imageHeight = height;
		this.frameRate = fps;
		this.captureIntervalSeconds = (captureInterval > 0) ? captureInterval : 5; // Đảm bảo > 0
		this.compressionQuality = quality;
		this.imageUploadService = uploadService;
		this.cameraService = cameraService;
		this.machineId = machineId;
		this.compressionQuality = quality;
		this.onInitializationFinished = onInitializationFinished;
	}

	@Override
	public void run() {
		String rtspUrl = buildRtspUrl();
		if (rtspUrl == null) {
			System.out.println("Không thể kết nối với Url sai");
			cameraService.disconnectFromCamera(camera.getId());
			return;
		}
		System.out.println("-> [Thread " + Thread.currentThread().getId() + "] Chuẩn bị kết nối tới: "
				+ camera.getCameraName() + " tại " + rtspUrl);
		ExecutorService executor = Executors.newSingleThreadExecutor();

		try {
			grabber = createGrabber(rtspUrl);
			FFmpegLogCallback.set(); // Bật log FFmpeg

			// == Thực thi grabber.start() với timeout ==
			Future<Void> startFuture = executor.submit(() -> {
				System.out.println("--> [" + Thread.currentThread().getName() + "] Bắt đầu gọi grabber.start()...");
				grabber.start(); // Lời gọi blocking
				System.out.println("--> [" + Thread.currentThread().getName() + "] grabber.start() hoàn thành.");

				return null;
			});

			try {
				startFuture.get(START_TIMEOUT_SECONDS, TimeUnit.SECONDS);
				System.out.println("-> [Thread " + Thread.currentThread().getId() + "] Kết nối thành công tới: "
						+ camera.getCameraName());
				connectionSuccessful = true;
				if (cameraService != null) {
					cameraService.reportCameraStatus(camera.getId(), true, machineId);
				}
			} catch (TimeoutException e) {
				System.err.println("!!! [Thread " + Thread.currentThread().getId() + "] Timeout ("
						+ START_TIMEOUT_SECONDS + "s) khi chờ grabber.start() cho " + camera.getCameraName());
				startFuture.cancel(true);

				throw new FrameGrabber.Exception("Timeout during grabber.start()");
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();

				System.err.println("!!! [Thread " + Thread.currentThread().getId()
						+ "] Lỗi ExecutionException khi gọi grabber.start() cho " + camera.getCameraName() + ": "
						+ (cause != null ? cause.getMessage() : e.getMessage()));
				if (cause instanceof FrameGrabber.Exception) {
					throw (FrameGrabber.Exception) cause;
				} else {
					throw new FrameGrabber.Exception("Error during grabber.start() execution", cause);
				}
			} catch (InterruptedException e) { // Bắt thêm InterruptedException

				Thread.currentThread().interrupt();
				System.err.println("!!! [Thread " + Thread.currentThread().getId()
						+ "] Luồng chờ grabber.start() bị gián đoạn cho " + camera.getCameraName());
				throw new FrameGrabber.Exception("Interrupted during grabber.start()");
			} finally {
				triggerInitFinished();
				executor.shutdownNow();
			}
			// ==========================================================

			// Nếu kết nối thành công, bắt đầu lấy frame
			Java2DFrameConverter bufferedImageConverter = new Java2DFrameConverter();
			lastSnapshotTime = System.currentTimeMillis(); // Đặt timer khi bắt đầu
			int count = 0;
			
			SettingsService settingsService = new SettingsService();
			while (running.get() && connectionSuccessful) {
				Frame grabbedFrame = null;
				try {
					grabbedFrame = grabber.grabImage(); // Thêm try-catch
				} catch (FrameGrabber.Exception frameEx) {
					System.err.println("!!! [Thread " + Thread.currentThread().getId() + "] Lỗi grabImage cho "
							+ camera.getCameraName() + ": " + frameEx.getMessage());
					running.set(false); // Dừng vòng lặp
					break; // Thoát vòng lặp
				}

				if (grabbedFrame == null) {
					System.err.println("-> [Thread " + Thread.currentThread().getId()
							+ "] Cảnh báo: Lấy được frame null từ " + camera.getCameraName());
					count++;
					if (count == 10) {
						running.set(false);
						break;
					}
					continue;

				}
				count = 0;

				// ====================================================
				// == LOGIC CHỤP ẢNH ĐỊNH KỲ ==
				// ====================================================
				long now = System.currentTimeMillis();
				if (imageUploadService != null && (now - lastSnapshotTime) > (this.captureIntervalSeconds * 1000L)) {
					lastSnapshotTime = now; // Reset timer

					// Lấy BufferedImage (việc này nhanh)
					// Dùng getBufferedImage thay vì convert để tránh clone không cần thiết
					BufferedImage bImage = bufferedImageConverter.getBufferedImage(grabbedFrame, 1.0, false, null);
					if (bImage != null) {
						// Gửi (async) đến service upload (sẽ tự nén và gửi đi)
						imageUploadService.upload(camera.getId(), bImage, this.compressionQuality, now);
					}
					String saveDir = settingsService.getSnapshotSavePath();

			        // Chỉ lưu nếu người dùng đã chọn đường dẫn
			        if (saveDir != null && !saveDir.isEmpty()) {
			            
			            // Chạy trong luồng riêng để không làm lag video stream
			            new Thread(() -> {
			                try {
			                    File folder = new File(saveDir);
			                    // Kiểm tra thư mục có tồn tại không
			                    if (folder.exists() && folder.isDirectory()) {
			                        
			                        // Tạo tên file: cam_{id}_{timestamp}.jpg
			                        String fileName = String.format("cam_%d_%d.jpg", camera.getId(), now);
			                        File outputFile = new File(folder, fileName);
			                        
			                        // Ghi file ra đĩa
			                        ImageIO.write(bImage, "jpg", outputFile);
			                        
			                        System.out.println("Đã lưu snapshot cục bộ: " + outputFile.getAbsolutePath());
			                    }
			                } catch (IOException e) {
			                    System.err.println("Lỗi khi lưu ảnh cục bộ: " + e.getMessage());
			                }
			            }).start();
			        }
				}
				// ====================================================

				// --- LOGIC STREAM LÊN UI ---
				Image fxImage = convertToJavaFXImage(grabbedFrame, bufferedImageConverter);
				if (fxImage != null && frameConsumer != null) {
					Platform.runLater(() -> frameConsumer.accept(fxImage));
				}
			}

		} catch (FrameGrabber.Exception e) {
			System.err.println("!!! [Thread " + Thread.currentThread().getId() + "] LỖI TRONG CATCH (FrameGrabber): "
					+ camera.getCameraName() + " - " + e.getMessage());
			triggerInitFinished();
		} catch (Exception e) {
			System.err.println("!!! [Thread " + Thread.currentThread().getId() + "] LỖI TRONG CATCH (Exception): "
					+ camera.getCameraName() + " - " + e.getMessage());
			e.printStackTrace();
			triggerInitFinished();
		} finally {
			System.out.println(">>> [Thread " + Thread.currentThread().getId()
					+ "] ĐANG TRONG KHỐI FINALLY cho Camera: " + (camera != null ? camera.getCameraName() : "UNKNOWN"));

			// Dọn dẹp grabber một cách an toàn
			if (grabber != null) {
				try {
					// Chỉ gọi stop() nếu đã start() thành công
					if (connectionSuccessful) {
						System.out.println(
								">>> [Thread " + Thread.currentThread().getId() + "] Đang gọi grabber.stop()...");
						grabber.stop();
					}
					System.out.println(
							">>> [Thread " + Thread.currentThread().getId() + "] Đang gọi grabber.release()...");
					grabber.release();
					System.out.println("-> [Thread " + Thread.currentThread().getId() + "] Đã release grabber cho "
							+ camera.getCameraName());
				} catch (FrameGrabber.Exception e) {
					System.err.println("!!! [Thread " + Thread.currentThread().getId()
							+ "] Lỗi khi stop/release grabber: " + e.getMessage());
				} catch (Exception e) {
					System.err.println("!!! [Thread " + Thread.currentThread().getId()
							+ "] Lỗi không mong muốn khi stop/release grabber: " + e.getMessage());
				}
			}
			if (frameConsumer != null) {

				System.out.println(
						"!!! [Thread " + Thread.currentThread().getId() + "] Sending NULL frame signal for Camera ID: "
								+ (camera != null ? camera.getId() : "UNKNOWN"));
				try {
					Platform.runLater(() -> {
						try {
							frameConsumer.accept(null);
						} catch (Exception uiEx) {
							System.err.println(
									"!!! Lỗi khi gọi accept(null) trong Platform.runLater: " + uiEx.getMessage());
						}
					});
				} catch (Exception platformEx) { // Bắt lỗi nếu Platform.runLater bị gọi sau khi FX tắt
					System.err.println(
							"!!! Lỗi khi gọi Platform.runLater (có thể FX đã tắt): " + platformEx.getMessage());
				}
			} else {
				System.err.println("!!! [Thread " + Thread.currentThread().getId()
						+ "] frameConsumer bị null, không thể gửi tín hiệu dừng.");
			}
			if (cameraService != null) {
				cameraService.reportCameraStatus(camera.getId(), false, machineId);
			}
			System.out.println("<<< [Thread " + Thread.currentThread().getId() + "] KẾT THÚC KHỐI FINALLY cho Camera: "
					+ (camera != null ? camera.getCameraName() : "UNKNOWN"));
		}
	} // Kết thúc run()

	/**
	 * Xây dựng URL RTSP dựa trên thông tin CameraDTO.
	 */
	private String buildRtspUrl() {
		String user = (camera.getUsername() != null) ? camera.getUsername().trim() : "";
		String pass = (camera.getPassword() != null) ? camera.getPassword() : "";
		String ip = (camera.getIpAddress() != null) ? camera.getIpAddress().trim() : "";

		if (ip.isEmpty()) {
			throw new IllegalArgumentException("Thiếu địa chỉ IP camera cho " + camera.getCameraName());
		}
		System.out.println(camera.getOnvifUrl());
		String formattedUrl = null;
		if (camera.getOnvifUrl().startsWith("rtsp://")) {
			String path = "/onvif1";

			if (!user.isEmpty() && !pass.isEmpty()) {
				formattedUrl = String.format(camera.getOnvifUrl(), user, pass, ip);
			} else {
				formattedUrl = String.format("rtsp://%s:554%s", ip, path);
			}
			System.out.println("Sử dụng URL: " + formattedUrl);
			return formattedUrl;
		}
		if (camera.getOnvifUrl().startsWith("http://") || camera.getOnvifUrl().startsWith("https://")) {
			formattedUrl = String.format(camera.getOnvifUrl(), ip);
			System.out.println("Sử dụng URL: " + formattedUrl);
			return formattedUrl;

		}
		return formattedUrl;
	}

	private Image convertToJavaFXImage(Frame frame, Java2DFrameConverter converter) {
		if (frame == null || frame.image == null) {
			return null;
		}
		BufferedImage bufferedImage = converter.convert(frame);
		if (bufferedImage == null) {
			return null;
		}
		WritableImage writableImage = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
		PixelWriter pixelWriter = writableImage.getPixelWriter();
		for (int x = 0; x < bufferedImage.getWidth(); x++) {
			for (int y = 0; y < bufferedImage.getHeight(); y++) {
				pixelWriter.setArgb(x, y, bufferedImage.getRGB(x, y));
			}
		}
		return writableImage;
	}

	public void stopStreaming() {
		running.set(false);
	}

	public int getCurrentWidth() {
		return this.imageWidth;
	}

	public int getCurrentHeight() {
		return this.imageHeight;
	}

	public void updateLightConfiguration(ClientDTO newConfig) {
		if (newConfig == null)
			return;
		int newInterval = (newConfig.getCaptureIntervalSeconds() > 0) ? newConfig.getCaptureIntervalSeconds() : 5;
		int newQuality = newConfig.getCompressionQuality();
		this.captureIntervalSeconds = newInterval;
		this.compressionQuality = newQuality;
		System.out.println(">>> Streamer ID " + camera.getId() + ": Cập nhật nóng thành công. Interval=" + newInterval
				+ "s, Quality=" + newQuality + "%");
	}

	private FFmpegFrameGrabber buildRtspGrabber(String rtspUrl) {
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspUrl);

		grabber.setOption("stimeout", "5000000"); // 5s timeout mạng RTSP
		grabber.setTimeout(10000); // 10s timeout đọc frame

		grabber.setImageWidth(this.imageWidth);
		grabber.setImageHeight(this.imageHeight);
		grabber.setFrameRate(this.frameRate);

		// RTSP KHÔNG ĐƯỢC setFormat("mjpeg")
		grabber.setFormat("rtsp");

		// Một số camera RTSP cần TCP để ổn định
		grabber.setOption("rtsp_transport", "udp");

		return grabber;
	}

	private FFmpegFrameGrabber buildMjpegGrabber(String mjpegUrl) {
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(mjpegUrl);

		// 1. Định dạng luồng là mjpeg
		grabber.setFormat("mjpeg");

		// 2. KHẮC PHỤC LỖI SWSCALER: Ép kiểu pixel đầu ra về BGR24
		grabber.setPixelFormat(avutil.AV_PIX_FMT_BGR24);

		// Tùy chỉnh kích thước
		if (this.imageWidth > 0 && this.imageHeight > 0) {
			grabber.setImageWidth(this.imageWidth);
			grabber.setImageHeight(this.imageHeight);
		}

		if (this.frameRate > 0) {
			grabber.setFrameRate(this.frameRate);
		}

		// --- Các Option tối ưu cho HTTP Stream ---
		grabber.setVideoOption("probesize", "32768");
		grabber.setVideoOption("analyzeduration", "100000");

		// Timeout (10 giây)
		grabber.setOption("timeout", "2000000");
		grabber.setOption("stimeout", "2000000");
		grabber.setOption("rtsp_transport", "udp");

		grabber.setOption("rw_timeout", "3000000"); // Cho HTTP/TCP

		// 3. GIẢM LOG NOISE: Chỉ hiện lỗi nghiêm trọng (Panic/Error), ẩn Warning
		avutil.av_log_set_level(avutil.AV_LOG_ERROR);

		return grabber;
	}

	private FFmpegFrameGrabber createGrabber(String url) {

		if (url.startsWith("rtsp://")) {
			System.out.println("-> Build RTSP Grabber");
			return buildRtspGrabber(url);
		}
		if (url.startsWith("http://") || url.startsWith("https://")) {
			System.out.println("-> Build RTSP Grabber");
			return buildMjpegGrabber(url);
		}

		throw new IllegalArgumentException("Không xác định được loại camera từ URL: " + url);
	}

	private boolean initSignalSent = false;

	private void triggerInitFinished() {
		if (!initSignalSent && onInitializationFinished != null) {
			try {
				onInitializationFinished.run();
				initSignalSent = true;
			} catch (Exception e) {
				System.err.println("Lỗi khi gọi callback khởi tạo: " + e.getMessage());
			}
		}
	}

}