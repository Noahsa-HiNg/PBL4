package com.pbl4.server.security; // Đảm bảo đúng package

// --- Các import cần thiết ---
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl4.server.security.JwtTokenProvider; // Dịch vụ token
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger; // Dùng SLF4J
import org.slf4j.LoggerFactory; // Dùng SLF4J
import org.springframework.http.MediaType; // Để set Content-Type
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // <<< Kế thừa lớp này
import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // Để đặt URL login

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap; // Để tạo Map response
import java.util.Map;

/**
 * Bộ lọc này CHỈ xử lý yêu cầu đăng nhập tại URL được cấu hình (vd: /api/auth/login).
 * Nó KHÔNG kiểm tra token trên các request khác.
 * Nó kế thừa UsernamePasswordAuthenticationFilter để tích hợp vào luồng xác thực của Spring.
 */
// Lớp này KHÔNG cần @Component vì nó được tạo thủ công trong SecurityConfig
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // Các dependency được inject từ SecurityConfig qua constructor
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    // Lớp nội bộ để dễ dàng đọc JSON request body khi đăng nhập
    private static class LoginRequest {
        public String username;
        public String password;
        // Jackson cần getter hoặc field public để đọc JSON
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    // Constructor nhận các dependency
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager,
                                 JwtTokenProvider jwtTokenProvider,
                                 ObjectMapper objectMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;

        // Đặt URL và phương thức HTTP mà bộ lọc này sẽ xử lý
        // Phải khớp với cấu hình trong SecurityConfig và URL mà client gọi
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/api/auth/login", "POST"));
        log.info("JwtAuthenticationFilter được cấu hình cho URL /api/auth/login (POST)");
    }

    /**
     * Được gọi khi có request khớp với URL login (/api/auth/login).
     * Đọc username/password từ JSON request body và cố gắng xác thực.
     * @return Đối tượng Authentication chứa thông tin user nếu xác thực thành công.
     * @throws AuthenticationException Nếu xác thực thất bại.
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
            // Đọc đối tượng LoginRequest từ JSON trong request body
            LoginRequest creds = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
            log.debug("Đang thử xác thực cho user: {}", creds.getUsername());

            // Tạo đối tượng token (chưa được xác thực) từ thông tin đọc được
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    creds.getUsername(),
                    creds.getPassword(),
                    Collections.emptyList() // Authorities sẽ được UserDetailsService cung cấp sau
            );

            // Giao cho AuthenticationManager (đã được cấu hình với UserDetailsService và PasswordEncoder)
            // để thực hiện việc xác thực (so sánh password hash).
            // Nếu sai user/pass, nó sẽ ném ra AuthenticationException.
            return authenticationManager.authenticate(authToken);

        } catch (IOException e) {
            log.error("Lỗi khi đọc request body đăng nhập: {}", e.getMessage());
            // Ném lỗi để Spring Security xử lý (sẽ gọi unsuccessfulAuthentication)
            // Có thể tạo AuthenticationException cụ thể hơn
            throw new RuntimeException("Lỗi khi đọc dữ liệu đăng nhập từ request.", e);
        }
    }

    /**
     * Được gọi KHI attemptAuthentication() xác thực thành công.
     * Tạo JWT token và gửi về cho client.
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain chain,
                                          Authentication authResult) throws IOException, ServletException {

        String username = authResult.getName(); // Lấy username từ đối tượng Authentication đã thành công
        log.info("Xác thực thành công cho user: {}", username);

        // Dùng JwtTokenProvider để tạo chuỗi JWT token
        String token = jwtTokenProvider.generateToken(authResult);
        log.debug("Đã tạo JWT Token cho user '{}'", username);

        // Chuẩn bị dữ liệu JSON để gửi về client
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", token);
        // (Tùy chọn: Có thể thêm username, role vào response nếu cần)
        // responseData.put("username", username);
        // authResult.getAuthorities().stream().findFirst().ifPresent(role ->
        //     responseData.put("role", role.getAuthority().replace("ROLE_", ""))
        // );

        // Thiết lập response header và ghi JSON body
        response.setStatus(HttpServletResponse.SC_OK); // Status 200 OK
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // Kiểu nội dung là JSON
        response.getWriter().write(objectMapper.writeValueAsString(responseData)); // Ghi JSON vào body
        response.getWriter().flush(); // Đảm bảo dữ liệu được gửi đi ngay lập tức
    }

    /**
     * Được gọi KHI attemptAuthentication() ném ra AuthenticationException (xác thực thất bại).
     * Gửi về lỗi 401 Unauthorized dạng JSON.
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            AuthenticationException failed) throws IOException, ServletException {

        log.warn("Xác thực thất bại: {}", failed.getMessage());

        // Thiết lập response lỗi 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Status 401 Unauthorized
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // Kiểu nội dung là JSON
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("message", "Tên đăng nhập hoặc mật khẩu không đúng."); // Thông báo lỗi
        errorData.put("error", "Unauthorized");
        response.getWriter().write(objectMapper.writeValueAsString(errorData)); // Ghi JSON lỗi vào body
        response.getWriter().flush();
    }
}