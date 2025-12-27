package com.pbl4.cameraclient.service;

import com.pbl4.cameraclient.network.ApiClient;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.imageio.stream.ImageOutputStream;

public class ImageService {

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ExecutorService uploadExecutor;

    public ImageService() {
        this.uploadExecutor = Executors.newFixedThreadPool(2); // 2 luồng để upload
    }

    /**
     * Chuyển đổi BufferedImage sang JPEG và gửi lên server.
     * Chạy trên luồng riêng (async).
     *
     * @param cameraId ID của camera.
     * @param bImage   Ảnh BufferedImage cần upload.
     * @param quality  Chất lượng nén (từ 0 đến 100).
     * @param capturedAtMillis Thời gian chụp ảnh (milliseconds).
     */
    public void upload(int cameraId, BufferedImage bImage, int quality, long capturedAtMillis) {
        if (bImage == null) return;

        // Gửi tác vụ nén ảnh và upload vào Thread Pool 
        uploadExecutor.submit(() -> {
            try {
                // 1. Chuyển đổi BufferedImage sang byte[] (JPEG)
                byte[] jpegData;
                float jpegQuality = quality / 100.0f; 

                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                    ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
                    jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    jpgWriteParam.setCompressionQuality(jpegQuality);

                    try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                        jpgWriter.setOutput(ios);
                        jpgWriter.write(null, new IIOImage(bImage, null, null), jpgWriteParam);
                    } finally {
                        jpgWriter.dispose();
                    }
                    jpegData = baos.toByteArray();
                }

                if (jpegData == null) {
                     System.err.println("Không thể nén ảnh JPEG cho camera " + cameraId);
                     return;
                }

                // 2. Không cần tạo tên file, server sẽ tự tạo UUID

                // 3. Gọi ApiClient để upload
                System.out.println("Đang upload snapshot cho camera " + cameraId + " (Time: " + capturedAtMillis + ", Size: " + (jpegData.length / 1024) + " KB, Q: " + quality + "%)");
                // Gọi hàm mới, truyền cả capturedAtMillis
                apiClient.uploadSnapshot(cameraId, jpegData, capturedAtMillis); 
                // System.out.println("Upload thành công cho camera " + cameraId); // Log này nên ở ApiClient sau khi có response
            
            } catch (IOException e) {
                System.err.println("Lỗi I/O khi upload snapshot cho camera " + cameraId + ": " + e.getMessage());
                // e.printStackTrace(); // Bỏ comment nếu cần debug
            } catch (Exception e) {
                 System.err.println("Lỗi không xác định khi upload snapshot cho camera " + cameraId + ": " + e.getMessage());
                 e.printStackTrace();
            }
        });
    }

    /**
     * Dọn dẹp Thread Pool khi ứng dụng tắt.
     */
    public void shutdown() {
        // ... (Giữ nguyên code shutdown)
        uploadExecutor.shutdown();
        try {
            if (!uploadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                uploadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            uploadExecutor.shutdownNow();
        }
    }
}