package com.pbl4.cameraclient.service;

import com.pbl4.cameraclient.model.LoginRequest;
import com.pbl4.cameraclient.model.LoginResponse;
import com.pbl4.cameraclient.network.ApiClient;

import java.io.IOException;

public class AuthService {

    private final ApiClient apiClient = ApiClient.getInstance();

    /**
     * Thực hiện đăng nhập bằng cách gọi API của server.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     * @return true nếu đăng nhập thành công, false nếu thất bại.
     */
    public boolean login(String username, String password) {
        try {
            // 1. Tạo đối tượng request
            LoginRequest requestData = new LoginRequest(username, password);

            // 2. Gọi ApiClient để thực hiện yêu cầu mạng
            System.out.println("Sending login request to server for user: " + username);
            LoginResponse response = apiClient.login(requestData);

            // 3. Xử lý kết quả
            if (response != null && response.getToken() != null && !response.getToken().isEmpty()) {
                // Lưu token để sử dụng cho các request sau
                apiClient.setAuthToken(response.getToken());
                System.out.println("Login successful! Token received.");
                return true;
            } else {
                System.err.println("Login failed: " + (response != null ? response.getMessage() : "No token in response"));
                return false;
            }

        } catch (IOException e) {
            // Bắt lỗi mạng hoặc lỗi server (vd: 401 Unauthorized)
            System.err.println("An error occurred during login API call: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void logout() {
        apiClient.clearAuthToken();
        System.out.println("User logged out, token cleared.");
    }
}