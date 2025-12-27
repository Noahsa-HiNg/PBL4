package com.pbl4.cameraclient.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.IllegalFormatException;

/**
 * Lớp chứa các phương thức tiện ích liên quan đến mạng.
 */
public class NetworkUtils {

    /**
     * Kiểm tra xem máy tính hiện tại có thể "ping" (kiểm tra khả năng kết nối)
     * đến một địa chỉ IP hoặc hostname cho trước hay không bằng cách sử dụng
     * phương thức InetAddress.isReachable().
     *
     * Lưu ý: Kết quả có thể bị ảnh hưởng bởi cài đặt Firewall.
     * Phương thức này không đảm bảo dịch vụ cụ thể (như RTSP) đang chạy trên host đó.
     *
     * @param address       Địa chỉ IP (ví dụ: "192.168.1.1") hoặc hostname (ví dụ: "google.com").
     * @param timeoutMillis Thời gian chờ tối đa cho phản hồi, tính bằng mili giây.
     * Giá trị hợp lý thường là 1000 (1 giây) đến 3000 (3 giây).
     * @return true nếu nhận được phản hồi trong thời gian chờ, false nếu không
     * (timeout, không thể phân giải địa chỉ, hoặc bị chặn bởi firewall).
     */
    public static boolean isReachable(String address, int timeoutMillis) {
        if (address == null || address.trim().isEmpty()) {
            System.err.println("Lỗi: Địa chỉ không được để trống.");
            return false;
        }
        if (timeoutMillis <= 0) {
            System.err.println("Lỗi: Timeout phải là số dương.");
            return false; // Hoặc ném IllegalArgumentException
        }

        try {
            // 1. Phân giải địa chỉ hostname/IP thành đối tượng InetAddress
            InetAddress inetAddress = InetAddress.getByName(address.trim());

            // 2. Thực hiện kiểm tra isReachable
            boolean reachable = inetAddress.isReachable(timeoutMillis);

            if (reachable) {
                System.out.println("   Ping thành công: " + address + " có thể kết nối.");
            }
         
            return reachable;

        } catch (UnknownHostException e) {
            // Xảy ra khi hostname không hợp lệ hoặc DNS không phân giải được
            System.err.println("   Lỗi Ping: Không thể phân giải địa chỉ hostname: " + address);
            return false;
        } catch (IOException e) {
            // Các lỗi mạng khác, ví dụ: không có quyền gửi ICMP
            // Thường không cần in chi tiết lỗi này trừ khi debug sâu
            System.err.println("   Lỗi Ping (IO): " + address + " - " + e.getMessage());
            return false;
        } catch (SecurityException e) {
             // Lỗi bảo mật nếu không có quyền
             System.err.println("   Lỗi Ping (Security): " + address + " - " + e.getMessage());
             return false;
        }
    }
    public static boolean canConnectTcp(String address, int port, int timeoutMillis) {
        if (address == null || address.trim().isEmpty()) {
            System.err.println("Lỗi TCP Connect: Địa chỉ không được để trống.");
            return false;
        }
        if (timeoutMillis <= 0) {
             System.err.println("Lỗi TCP Connect: Timeout phải là số dương.");
             return false;
        }

        // Sử dụng try-with-resources để đảm bảo socket luôn được đóng
        try (Socket socket = new Socket()) {
            System.out.println("-> Đang thử kết nối TCP tới: " + address + ":" + port + " với timeout " + timeoutMillis + "ms...");
            // Tạo địa chỉ socket
            InetSocketAddress socketAddress = new InetSocketAddress(address.trim(), port);
            // Cố gắng kết nối với timeout
            socket.connect(socketAddress, timeoutMillis);
            // Nếu không có lỗi -> Kết nối thành công
            System.out.println("   Kết nối TCP thành công tới " + address + ":" + port);
            return true;
        } catch (SocketTimeoutException e) {
            // Lỗi timeout khi kết nối
            System.out.println("   Kết nối TCP thất bại (Timeout): " + address + ":" + port);
            return false;
        } catch (UnknownHostException e) {
             // Lỗi không phân giải được hostname
             System.err.println("   Lỗi TCP Connect: Không thể phân giải địa chỉ: " + address);
             return false;
        } catch (IOException e) {
            // Các lỗi IO khác (ví dụ: Connection refused - cổng đóng, No route to host)
            System.out.println("   Kết nối TCP thất bại (IO): " + address + ":" + port + " - " + e.getMessage());
            return false;
        } catch (SecurityException e) {
             System.err.println("   Lỗi TCP Connect (Security): " + address + ":" + port + " - " + e.getMessage());
             return false;
        }
    }
    public static boolean isCameraReachable(String ip, String urlTemplate, int timeoutMs) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        String targetHost = ip;
        int targetPort = -1;
        String scheme = "http"; // Mặc định

        // LOGIC ĐÃ SỬA
        if (urlTemplate != null && !urlTemplate.trim().isEmpty()) {
            try {
                // Thay thế tất cả %s bằng một chuỗi giả định (ví dụ "dummy") để tạo thành URL hợp lệ về mặt cú pháp
                // Điều này giúp URI.create không bị lỗi và ta lấy được Scheme + Port
                String tempUrl = urlTemplate.replace("%s", "dummy");
                
                URI uri = URI.create(tempUrl);
                
                if (uri.getScheme() != null) {
                    scheme = uri.getScheme();
                }
                // Lấy port từ template (ví dụ 554)
                targetPort = uri.getPort();

            } catch (Exception e) {
                // Nếu lỗi parse, giữ nguyên mặc định
                e.printStackTrace(); 
            }
        }

        // Fallback port nếu template không ghi rõ port (ví dụ -1)
        if (targetPort <= 0) {
            if ("rtsp".equalsIgnoreCase(scheme)) targetPort = 554;
            else if ("http".equalsIgnoreCase(scheme)) targetPort = 80;
            else if ("https".equalsIgnoreCase(scheme)) targetPort = 443;
            else targetPort = 80;
        }

        // Thực hiện kết nối tới IP thực tế (targetHost) với Port đã lấy được
        try (Socket socket = new Socket()) {
            // Lưu ý: targetHost lúc này vẫn là IP gốc truyền vào, targetPort là port lấy từ template (554)
            socket.connect(new InetSocketAddress(targetHost, targetPort), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    public static double measureBandwidthMbps() {
        // Link file test tốc độ (Nên dùng file nhỏ khoảng 1MB - 10MB từ server ổn định)
        // Ví dụ: File 1MB của Tele2 (Server test mạng phổ biến)
        String fileUrl = "http://speedtest.tele2.net/1MB.zip"; 
        
        long startTime = System.currentTimeMillis();
        long totalBytesRead = 0;
        
        try {
            URL url = new URL(fileUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000); // Đừng đo quá 5 giây

            try (InputStream in = conn.getInputStream()) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                
                // Đọc dữ liệu
                while ((bytesRead = in.read(buffer)) != -1) {
                    totalBytesRead += bytesRead;
                }
            }
            
            long endTime = System.currentTimeMillis();
            long durationMs = endTime - startTime;
            
            if (durationMs == 0) durationMs = 1; // Tránh chia cho 0

            // Tính toán:
            // 1 byte = 8 bits
            // Mbps = (TotalBytes * 8) / (Duration_in_seconds * 1_000_000)
            
            double speedMbps = (totalBytesRead * 8.0) / (durationMs / 1000.0) / 1_000_000.0;
            
            // Làm tròn 2 chữ số thập phân
            return Math.round(speedMbps * 100.0) / 100.0;

        } catch (Exception e) {
            System.err.println("Lỗi đo băng thông: " + e.getMessage());
            return -1;
        }
    }
 // Trong class NetworkUtils
    public static long checkNetworkLatency() {
        long startTime = System.currentTimeMillis();
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("8.8.8.8", 53), 1000); // Timeout 1s
            return System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            return 9999; // Mạng lỗi hoặc quá lag
        }
    }
}


