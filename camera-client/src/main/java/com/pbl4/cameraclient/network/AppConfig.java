package com.pbl4.cameraclient.network;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties properties = new Properties();

    // Khối static này sẽ chạy ngay khi class được gọi lần đầu tiên
    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Không tìm thấy file config.properties!");
            } else {
                properties.load(input); // Nạp dữ liệu từ file vào biến properties
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Hàm lấy địa chỉ IP
    public static String getServerIp() {
        return properties.getProperty("server.ip", "localhost"); // Mặc định là localhost nếu không tìm thấy
    }

    // Hàm lấy Port (chuyển đổi sang số nguyên)
    public static int getServerPort() {
        String portStr = properties.getProperty("server.port", "8080");
        return Integer.parseInt(portStr);
    }
}