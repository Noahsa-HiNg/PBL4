package com.pbl4.cameraclient.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.cameraclient.model.AddCameraRequest;
import com.pbl4.cameraclient.model.CameraDTO;
import com.pbl4.cameraclient.model.ClientRegisterRequest;
import com.pbl4.cameraclient.model.ClientRegisterResponse;
import com.pbl4.cameraclient.model.LoginRequest; // Import model mới
import com.pbl4.cameraclient.model.LoginResponse; // Import model mới
import com.pbl4.cameraclient.model.UpdateCameraActiveRequest;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

import okhttp3.*;


import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {
	static String ip = AppConfig.getServerIp();
	static int port = AppConfig.getServerPort();
    // SỬA LẠI ĐỊA CHỈ SERVER CỦA BẠN
    private static final String BASE_URL = "http://" + ip + ":" + port + "/api";
//	private static final String BASE_URL = "http://192.168.0.4:8080/api";
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
    public ClientRegisterResponse registerOrGetClient(ClientRegisterRequest clientRegisterRequest) throws IOException {
    	// 1. Chuyển đổi đối tượng request thành JSON
        String jsonBody = objectMapper.writeValueAsString(clientRegisterRequest);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        
        // 2. Tạo request tới endpoint mới
        Request request = new Request.Builder()
                .url(BASE_URL + "/clients/register") // Endpoint đăng ký client
                .post(requestBody)
                .build();

        // 3. Gửi request và xử lý response
        try (Response response = client.newCall(request).execute()) {
            // Đọc body 1 LẦN
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                // Ném lỗi (ví dụ: 401 nếu token sai, 500 nếu server lỗi)
                throw new IOException("Failed to register client: " + response.code() + " - " + responseBody);
            }
            
            // 4. Chuyển đổi JSON response thành đối tượng ClientRegisterResponse
            return objectMapper.readValue(responseBody, ClientRegisterResponse.class);
        }
    }
    public void uploadSnapshot(int cameraId, byte[] imageBytes, long capturedAtMillis) throws IOException {
    	
        
        // 1. Tạo RequestBody cho file ảnh
        // Server sẽ tự tạo tên file bằng UUID, nên tên file ở client không quan trọng.
        RequestBody imageRequestBody = RequestBody.create(imageBytes, MediaType.get("image/jpeg"));

        // 2. Tạo MultipartBody (để gửi file và dữ liệu)
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                // Thêm trường "cameraId" (dạng String)
                .addFormDataPart("cameraId", String.valueOf(cameraId)) 
                // Thêm trường "capturedAt" (dạng String milliseconds)
                // Spring Boot sẽ tự động chuyển đổi String (long) này thành Timestamp
                .addFormDataPart("capturedAt", String.valueOf(capturedAtMillis)) 
                // Thêm file ảnh, tên file ("snapshot.jpg") chỉ là tạm thời
                .addFormDataPart("file", "snapshot.jpg", imageRequestBody) 
                .build();

        // 3. Tạo Request (đã bao gồm JWT Token từ Interceptor)
        Request request = new Request.Builder()
                .url(BASE_URL + "/images/upload") // API endpoint mới
                .post(requestBody)
                .build();

        // 4. Thực thi
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("Upload snapshot thất bại: " + response.code() + " - " + responseBody);
                throw new IOException("Upload snapshot thất bại: " + response.code() + " - " + responseBody);
            } else {
                 System.out.println("Upload response: " + responseBody); // Log response thành công từ server
            }
        }
    }
    public void updateCameraStatus(int cameraId,String machineId, boolean active) throws IOException {
        UpdateCameraActiveRequest requestData = new UpdateCameraActiveRequest(active,machineId);
        String jsonBody = objectMapper.writeValueAsString(requestData);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "/cameras/" + cameraId + "/status") // PUT /api/cameras/{id}/status
                .put(requestBody) // Dùng PUT
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Cập nhật trạng thái camera " + cameraId + " thất bại: " + response.body().string());
            } else {
                System.out.println("Cập nhật trạng thái camera " + cameraId + " thành công (active=" + active + ").");
            }
        }
    }
    public CameraDTO addCamera(AddCameraRequest addRequest) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(addRequest);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "/cameras/add") // POST /api/cameras/add
                .post(requestBody)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                // Ném lỗi (ví dụ: 409 Conflict - Camera đã tồn tại)
                throw new IOException("Thêm camera thất bại: " + response.code() + " - " + responseBody);
            }
            // Trả về CameraDTO của camera vừa tạo (Server trả về 201 Created)
            return objectMapper.readValue(responseBody, CameraDTO.class);
        }
    }
    /**
     * Gửi yêu cầu PUT để cập nhật thông tin camera.
     * Tự động đính kèm token xác thực đã lưu.
     *
     * @param cameraId ID của camera cần cập nhật
     * @param cameraData Đối tượng CameraDTO chứa thông tin mới
     * @throws IOException Nếu cuộc gọi mạng thất bại hoặc server trả về lỗi
     */
    public void updateCamera(int cameraId, CameraDTO cameraData) throws IOException {
        if (this.authToken == null || this.authToken.isEmpty()) {
            throw new IOException("Không thể cập nhật: Người dùng chưa được xác thực (token is null).");
        }

        // 1. Tạo URL (Giả sử API của bạn là /api/cameras/{id})
        String url = BASE_URL + "/cameras/" + cameraId;

        // 2. Chuyển đổi đối tượng CameraDTO thành chuỗi JSON
        String jsonPayload = objectMapper.writeValueAsString(cameraData);

        // 3. Tạo RequestBody
        RequestBody body = RequestBody.create(jsonPayload, JSON);

        // 4. Xây dựng Request, đính kèm token
        Request request = new Request.Builder()
            .url(url)
            .put(body) // Sử dụng .put() cho việc cập nhật
            .addHeader("Authorization", "Bearer " + this.authToken) // Thêm token xác thực
            .addHeader("Content-Type", "application/json")
            .build();

        System.out.println("Đang gửi PUT request đến: " + url);

        // 5. Thực thi cuộc gọi
        try (Response response = client.newCall(request).execute()) {
            
            // 6. Xử lý kết quả
            if (!response.isSuccessful()) {
                // Ném lỗi nếu server trả về 4xx hoặc 5xx
                throw new IOException("Yêu cầu cập nhật thất bại: " + response.code() + " - " + response.body().string());
            }

            // In ra để debug (tùy chọn)
            System.out.println("Cập nhật camera thành công. Phản hồi: " + response.body().string());
        }
    }
    public void deleteCamera(int cameraId) throws IOException {
        if (this.authToken == null || this.authToken.isEmpty()) {
            throw new IOException("Không thể xóa: Người dùng chưa được xác thực (token is null).");
        }

        // 1. Tạo URL (Giả sử API của bạn là /api/cameras/{id})
        String url = BASE_URL + "/cameras/" + cameraId; // Đã sửa (bỏ /api lặp)

        // 2. Xây dựng Request
        Request request = new Request.Builder()
            .url(url)
            .delete() // <-- Sử dụng .delete()
            .addHeader("Authorization", "Bearer " + this.authToken) // Thêm token
            .build();

        System.out.println("Đang gửi DELETE request đến: " + url);

        // 3. Thực thi cuộc gọi
        try (Response response = client.newCall(request).execute()) {
            
            if (!response.isSuccessful()) {
                throw new IOException("Yêu cầu xóa thất bại: " + response.code() + " - " + response.body().string());
            }

            System.out.println("Xóa camera thành công trên server. Phản hồi: " + response.body().string());
        }
    }
    
    // Thêm các phương thức post, get khác cho các API sau này...
}