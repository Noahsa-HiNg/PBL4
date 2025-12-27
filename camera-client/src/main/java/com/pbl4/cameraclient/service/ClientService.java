package com.pbl4.cameraclient.service;

import pbl4.common.model.Camera;
import pbl4.common.model.Client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.io.IOException;

import com.pbl4.cameraclient.model.ClientRegisterRequest;
import com.pbl4.cameraclient.model.ClientRegisterResponse;
import com.pbl4.cameraclient.network.ApiClient;

public class ClientService {
	private final ApiClient apiClient = ApiClient.getInstance();
	private final CameraService cameraService = new CameraService();
	
	
	public static String getStableMacAddress() throws SocketException {
        String ethernetMac = null;
        String wifiMac = null;

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();

            // 1. Bỏ qua loopback, ảo (virtual), và các interface không có MAC
            if (ni.isLoopback() || ni.isVirtual()) {
                continue;
            }
            byte[] mac = ni.getHardwareAddress();
            if (mac == null || mac.length == 0) {
                continue;
            }

            // 2. Chuyển MAC sang String
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            String macAddress = sb.toString();
            String displayName = ni.getDisplayName().toLowerCase();
            String name = ni.getName().toLowerCase(); // Tên giao diện (eth0, wlan0)

            // 3. Phân loại và ưu tiên
            // Ưu tiên 1: Ethernet (mạng dây)
            if (displayName.contains("ethernet") || name.startsWith("eth") || name.startsWith("en0") || displayName.contains("gigabit")) {
                if (ethernetMac == null) {
                    System.out.println("Tìm thấy Ethernet MAC: " + macAddress + " (" + ni.getDisplayName() + ")");
                    ethernetMac = macAddress;
                }
            }
            // Ưu tiên 2: Wi-Fi (mạng không dây)
            else if (displayName.contains("wi-fi") || displayName.contains("wireless") || name.startsWith("wlan") || name.startsWith("en1")) {
                if (wifiMac == null) {
                    System.out.println("Tìm thấy Wi-Fi MAC: " + macAddress + " (" + ni.getDisplayName() + ")");
                    wifiMac = macAddress;
                }
            }
        }

        // 4. Trả về theo thứ tự ưu tiên
        if (ethernetMac != null) {
            System.out.println("Sử dụng Ethernet MAC: " + ethernetMac);
            return ethernetMac; // Luôn ưu tiên Ethernet
        }
        
        if (wifiMac != null) {
            System.out.println("Sử dụng Wi-Fi MAC: " + wifiMac);
            return wifiMac; // Nếu không có Ethernet, dùng Wi-Fi
        }

        // 5. Dự phòng (Fallback): Nếu không tìm thấy Ethernet/WiFi,
        // quay lại hàm cũ của bạn (lấy cái đầu tiên tìm thấy, nhưng bỏ 'isUp')
        return getFirstAvailableMac();
    }

    /**
     * Hàm dự phòng: Lấy địa chỉ MAC đầu tiên tìm thấy (không phải loopback/virtual).
     */
    private static String getFirstAvailableMac() throws SocketException {
         Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
         while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isLoopback() || ni.isVirtual()) continue;
            byte[] mac = ni.getHardwareAddress();
            if (mac == null || mac.length == 0) continue;
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            System.out.println("Sử dụng MAC dự phòng (đầu tiên tìm thấy): " + sb.toString());
            return sb.toString();
        }
        return null; // Không tìm thấy gì
    }
    public static String getCurrentWindowsUser() {
        // 1) Thử biến môi trường (Windows thường dùng USERNAME)
        String user = System.getenv("USERNAME");
        if (user != null && !user.isBlank()) return user;

        // 2) Thử System property (cross-platform)
        user = System.getProperty("user.name");
        if (user != null && !user.isBlank()) return user;

        try {
            ProcessBuilder pb = new ProcessBuilder("whoami");
            Process p = pb.start();

            // Dùng charset mặc định; nếu bạn gặp lỗi encoding trên Windows có thể thử "Cp1252" hoặc "Cp437"
            InputStream is = p.getInputStream();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()))) {
                String line = br.readLine();
                if (line != null && !line.isBlank()) {
                    int idx = line.lastIndexOf('\\');
                    if (idx >= 0 && idx < line.length() - 1) {
                        return line.substring(idx + 1).trim();
                    } else {
                        return line.trim();
                    }
                }
            }
            p.waitFor();
        } catch (Exception e) {
            // không cần throw — chỉ ghi log nếu muốn
            // e.printStackTrace();
        }

        // Nếu không lấy được
        return null;
    }
    public ClientRegisterResponse registerClientAndGetCameras() throws IOException, Exception {
        // 1. Lấy thông tin máy
        String machineId = getStableMacAddress(); // Nên dùng hàm getMachineId() tôi đã cung cấp, nó ổn định hơn
        String clientName = getCurrentWindowsUser(); // Lấy tên user Windows làm tên client

        // 2. Kiểm tra thông tin máy
        if (machineId == null || machineId.startsWith("UNKNOWN") || clientName == null || clientName.isEmpty()) {
            // Ném Exception thay vì trả về false
            throw new Exception("Không thể xác định thông tin định danh của máy tính (MAC/Username).");
        }

        // 3. Tạo đối tượng request
        ClientRegisterRequest requestData = new ClientRegisterRequest(machineId,clientName);

        System.out.println("Đang đăng ký client với Server... Machine ID: " + machineId);

        // 4. Gọi API - Hàm này đã xử lý lỗi mạng/server và sẽ ném IOException nếu thất bại
        ClientRegisterResponse response = apiClient.registerOrGetClient(requestData);

        // 5. Kiểm tra và trả về response (Không cần gọi cameraService.connectToCameras ở đây)
        if (response != null) {
            System.out.println("Server phản hồi: " + response.getMessage());
            // Việc kết nối camera sẽ do LoginController xử lý sau khi nhận được response này
            return response;
        } else {
            // Trường hợp này ít xảy ra nếu API client xử lý lỗi tốt
            throw new Exception("Đăng ký client thất bại: Server không phản hồi hợp lệ (response null).");
        }
        // Không cần khối catch ở đây nữa vì ta muốn ném lỗi ra cho LoginController xử lý
    }
}