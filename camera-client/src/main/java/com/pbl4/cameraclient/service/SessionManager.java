package com.pbl4.cameraclient.service;

import com.pbl4.cameraclient.model.ClientDTO;
import com.pbl4.cameraclient.network.ApiClient;

public class SessionManager {

    // Áp dụng Singleton Pattern
    private static SessionManager instance;

    private String authToken;
    private ClientDTO clientInfo;

    // private constructor để ngăn người khác tạo đối tượng mới
    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Lưu trữ token sau khi đăng nhập thành công.
     * Cũng đồng thời thiết lập token cho ApiClient.
     */
    public void createSession(String token) {
        this.authToken = token;
        ApiClient.getInstance().setAuthToken(token);
        this.clientInfo = null;
    }

    /**
     * Xóa session khi đăng xuất.
     */
    public void clearSession() {
        this.authToken = null;
        ApiClient.getInstance().clearAuthToken();
    }

    /**
     * Đây là phương thức kiểm tra quan trọng nhất.
     * @return true nếu người dùng đã đăng nhập (có token), ngược lại false.
     */
    public boolean isAuthenticated() {
        return this.authToken != null && !this.authToken.isEmpty();
    }

    public String getAuthToken() {
        return authToken;
    }
    public void setClientInfo(ClientDTO clientInfo) {
        this.clientInfo = clientInfo;
    }
    
    // Thêm hàm này để các controller khác lấy
    public ClientDTO getClientInfo() {
        return this.clientInfo;
    }
}