package com.pbl4.cameraclient.model;

// import vn.edu.dut.pbl4.common.model.LoginResponse;

public class LoginResponse {
    private String token;
    private String message;

//     Getters - Setters cần thiết cho Jackson để deserialize
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}