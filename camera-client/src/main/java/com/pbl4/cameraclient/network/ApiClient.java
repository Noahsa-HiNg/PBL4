package com.pbl4.cameraclient.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.cameraclient.model.LoginRequest; // Import model mới
import com.pbl4.cameraclient.model.LoginResponse; // Import model mới
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // SỬA LẠI ĐỊA CHỈ SERVER CỦA BẠN
    private static final String BASE_URL = "http://192.168.5.208:8080/api";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static ApiClient instance;

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private String authToken;

    private ApiClient() {
        this.objectMapper = new ObjectMapper();
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request.Builder newRequest = chain.request().newBuilder();
                    if (authToken != null && !authToken.isEmpty()) {
                        newRequest.addHeader("Authorization", "Bearer " + authToken);
                    }
                    return chain.proceed(newRequest.build());
                });
        this.client = builder.build();
    }

    public void setAuthToken(String token) { this.authToken = token; }
    public void clearAuthToken() { this.authToken = null; }

    /**
     * Phương thức chuyên dụng để gọi API đăng nhập.
     * @param loginRequest Đối tượng chứa username và password.
     * @return Đối tượng LoginResponse chứa token từ server.
     * @throws IOException Nếu có lỗi mạng hoặc lỗi từ server.
     */
    public LoginResponse login(LoginRequest loginRequest) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(loginRequest);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login") // Endpoint đăng nhập
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // Ném lỗi với thông điệp từ server để dễ debug
                throw new IOException("Login failed with code: " + response.code() + " - " + response.body().string());
            }
            // Chuyển đổi JSON response thành đối tượng LoginResponse
            return objectMapper.readValue(response.body().string(), LoginResponse.class);
        }
    }
    
    // Thêm các phương thức post, get khác cho các API sau này...
}