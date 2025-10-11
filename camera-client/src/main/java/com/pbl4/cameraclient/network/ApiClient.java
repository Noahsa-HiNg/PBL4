package com.pbl4.cameraclient.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // Hằng số cho cấu hình
    private static final String BASE_URL = "http://localhost:8080/api"; // <-- Sửa lại địa chỉ server của bạn ở đây
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // --- BẮT ĐẦU PHẦN SINGLETON ---
    private static ApiClient instance;

    // Phương thức để lấy thể hiện duy nhất của ApiClient
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }
    // --- KẾT THÚC PHẦN SINGLETON ---

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private String authToken;

    // Constructor để private để không ai tạo đối tượng mới từ bên ngoài được
    private ApiClient() {
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
        this.objectMapper = new ObjectMapper();
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public void clearAuthToken() {
        this.authToken = null;
    }

    public String post(String endpoint, Object body) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + " - " + response.body().string());
            }
            return response.body().string();
        }
    }
}