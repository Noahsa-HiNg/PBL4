package com.pbl4.cameraclient.model;

// Sử dụng model chung nếu đã có, nếu không thì tạo mới
// import vn.edu.dut.pbl4.common.model.LoginRequest; 

public class LoginRequest {
    private String username;
    private String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters - Setters cần thiết cho thư viện Jackson để serialize
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}