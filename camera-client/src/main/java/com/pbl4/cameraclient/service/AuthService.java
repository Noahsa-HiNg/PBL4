package com.pbl4.cameraclient.service;

import com.pbl4.cameraclient.network.ApiClient;

public class AuthService {

    /**
     * Giả lập quá trình đăng nhập.
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return Luôn trả về true để mô phỏng đăng nhập thành công.
     */
    public boolean login(String username, String password) {
        System.out.println("--- MOCK LOGIN ---");
        System.out.println("Đang giả lập đăng nhập cho user: " + username);

        // Giả vờ có một chút độ trễ mạng
        try {
            Thread.sleep(1500); // Chờ 1.5 giây
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        // Sau khi "đăng nhập thành công", giả lập việc nhận và lưu token
        String fakeToken = "this-is-a-fake-jwt-token-for-testing-" + username;
        ApiClient.getInstance().setAuthToken(fakeToken);

        System.out.println("Đăng nhập giả lập thành công! Token đã được lưu.");
        System.out.println("--------------------");

        return true;
    }

    public void logout() {
        System.out.println("Đã đăng xuất và xóa token.");
        ApiClient.getInstance().clearAuthToken();
        // Thêm các logic khác để quay về màn hình đăng nhập...
    }
}