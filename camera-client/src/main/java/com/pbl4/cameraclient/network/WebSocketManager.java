package com.pbl4.cameraclient.network;


import com.fasterxml.jackson.databind.ObjectMapper;
import javax.websocket.*;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// Sử dụng JSR 356 (javax.websocket)
@ClientEndpoint
public class WebSocketManager {

    private final String serverUrl; // Ví dụ: "ws://localhost:8080/ws"
    private final String jwtToken;
    private final String machineId;
    private Session session;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Consumer<String> onMessageReceived; // Callback cho MainViewController
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();

    public WebSocketManager(String serverUrl, String jwtToken,String machineId ) {
        this.serverUrl = serverUrl;
        this.jwtToken = jwtToken;
        this.machineId = machineId;
        
    }
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

    /**
     * Đặt callback để xử lý tin nhắn từ server (ví dụ: "SETTINGS_UPDATED")
     */
    public void setOnMessageReceived(Consumer<String> callback) {
        this.onMessageReceived = callback;
    }

    /**
     * Bắt đầu kết nối
     */
    public void connect() {
        try {
            System.out.println("WebSocket Client: Đang kết nối tới " + serverUrl);
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(serverUrl));
        } catch (Exception e) {
            System.err.println("Lỗi kết nối WebSocket: " + e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * Ngắt kết nối chủ động
     */
    public void disconnect() {
        reconnectScheduler.shutdown(); // Ngừng lịch kết nối lại
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi ngắt kết nối WebSocket: " + e.getMessage());
        }
    }

    /**
     * Được gọi khi kết nối thành công (Annotation @OnOpen)
     */
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("WebSocket Client: Đã kết nối (Session: " + session.getId() + ")");
        this.session = session;
        // Gửi tin nhắn xác thực (AUTH) ngay lập tức
        sendAuthentication();
    }

    /**
     * Gửi thông điệp {"type": "AUTH", "token": "..."}
     */
    private void sendAuthentication() {
        try {
            Map<String, String> authMessage = Map.of(
                    "type", "AUTH",
                    "token", this.jwtToken,
                    "clientType", "APP",
                    "machineId", this.machineId // <-- BẮT BUỘC
                );
            String jsonPayload = objectMapper.writeValueAsString(authMessage);
            System.out.println("WebSocket Client: Gửi thông tin xác thực...");
            this.session.getBasicRemote().sendText(jsonPayload);
        } 
        catch (Exception e) {
            System.err.println("Lỗi gửi tin nhắn AUTH: " + e.getMessage());
        }
    }

    /**
     * Được gọi khi nhận được tin nhắn từ server (Annotation @OnMessage)
     */
    @OnMessage
    public void onMessage(String message) {
        System.out.println("WebSocket Client: Nhận được tin nhắn: " + message);
        try {
            // Phân tích tin nhắn để kiểm tra "AUTH_SUCCESS"
            Map<String, String> msg = objectMapper.readValue(message, Map.class);
            String type = msg.get("type");

            if ("AUTH_SUCCESS".equals(type)) {
                System.out.println("WebSocket Client: Xác thực thành công!");
                // Bạn có thể làm gì đó ở đây, ví dụ: reset bộ đếm reconnect
            } else {
                // Đây là các tin nhắn khác (ví dụ: SETTINGS_UPDATED)
                if (onMessageReceived != null) {
                    onMessageReceived.accept(message); // Gửi payload về cho MainViewController
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi đọc JSON từ server: " + e.getMessage());
        }
    }

    /**
     * Được gọi khi kết nối bị đóng (Annotation @OnClose)
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("WebSocket Client: Đã ngắt kết nối. Lý do: " + closeReason.getReasonPhrase());
        scheduleReconnect();
    }

    /**
     * Được gọi khi có lỗi (Annotation @OnError)
     */
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Lỗi WebSocket: " + throwable.getMessage());
        // Lỗi thường dẫn đến ngắt kết nối, OnClose sẽ được gọi
    }

    /**
     * Lên lịch kết nối lại sau 10 giây
     */
    private void scheduleReconnect() {
        if (!reconnectScheduler.isShutdown()) {
            System.out.println("WebSocket Client: Sẽ thử kết nối lại sau 10 giây...");
            reconnectScheduler.schedule(this::connect, 10, TimeUnit.SECONDS);
        }
    }
}
