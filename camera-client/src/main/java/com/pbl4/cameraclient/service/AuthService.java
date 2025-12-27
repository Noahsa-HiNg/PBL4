package com.pbl4.cameraclient.service;

import com.pbl4.cameraclient.model.LoginRequest;
import com.pbl4.cameraclient.model.LoginResponse;
import com.pbl4.cameraclient.network.ApiClient;
import java.io.IOException;

public class AuthService {

    private static final AuthService instance = new AuthService();

    public static AuthService getInstance() {
        return instance;
    }

    private final ApiClient apiClient = ApiClient.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    // Constructor nên là private chuẩn Singleton
    private AuthService() {
    }

    // --- SỬA ĐỔI QUAN TRỌNG TẠI ĐÂY ---
    // 1. Thêm 'throws IOException'
    // 2. Xóa bỏ hoàn toàn try-catch
    public boolean login(String username, String password) throws IOException {
        
        // 1. Tạo đối tượng request
        LoginRequest requestData = new LoginRequest(username, password);

        // 2. Gọi ApiClient. 
        // Nếu mạng lỗi (Timeout, Connect Refused), dòng này sẽ TỰ ĐỘNG ném IOException ra ngoài.
        // Controller sẽ bắt được lỗi này ở hàm setOnFailed.
        System.out.println("Sending login request to server for user: " + username);
        LoginResponse response = apiClient.login(requestData);

        // 3. Xử lý kết quả logic (Server trả về 200 OK nhưng nội dung có thể thành công hoặc thất bại)
        if (response != null && response.getToken() != null && !response.getToken().isEmpty()) {
            // Đăng nhập thành công
            session.createSession(response.getToken());
            System.out.println("Login successful! Session created.");
            return true;
        } else {
            // Kết nối thành công tới server, nhưng server từ chối (ví dụ: sai pass)
            System.err.println("Login failed: " + (response != null ? response.getMessage() : "No token in response"));
            return false; 
        }
    }

    public void logout() {
        session.clearSession();
        System.out.println("User logged out, token cleared.");
    }

    public String getJwtToken() {
        return session.getAuthToken();
    }
    public void setJwtToken(String token) {
        if (token == null) {
            // Nếu truyền null -> Gọi logout để xóa session
            this.logout();
        } else {
            // Nếu truyền token mới -> Tạo session mới
            session.createSession(token);
        }
    }
}