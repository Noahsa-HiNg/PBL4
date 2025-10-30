package com.pbl4.server.security; // Đảm bảo đúng package

// --- Các import cần thiết ---
// Lấy các lớp này từ các file bạn đã có hoặc tạo mới
import com.pbl4.server.security.JwtTokenProvider;
import com.pbl4.server.service.UserDetailsServiceImpl;

// Import từ Jakarta EE Servlet API (thường có sẵn trong Spring Boot Web)
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Import từ SLF4J (thư viện logging phổ biến, nên dùng thay System.out)
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Import từ Spring Core (để đánh dấu @NonNull - tùy chọn nhưng nên có)
import org.springframework.lang.NonNull;

// Import từ Spring Security Core
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Để bắt lỗi cụ thể

// Import từ Spring Web (lớp cơ sở cho filter)
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bộ lọc này chạy MỘT LẦN cho mỗi request đến server.
 * Nhiệm vụ:
 * 1. Kiểm tra header "Authorization" xem có chứa Bearer Token không.
 * 2. Nếu có token, dùng JwtTokenProvider để xác thực (validate).
 * 3. Nếu token hợp lệ, lấy username, tải UserDetails.
 * 4. Tạo đối tượng Authentication và đặt vào SecurityContextHolder.
 * -> Báo cho Spring Security biết request này đã được xác thực.
 */
// Lớp này KHÔNG cần @Component vì nó được tạo thủ công trong SecurityConfig
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    // Khởi tạo logger để ghi log thay vì System.out.println
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

    // Các dependency sẽ được inject từ SecurityConfig
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    // Constructor để nhận dependency
    public JwtAuthorizationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsServiceImpl userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Phương thức chính của bộ lọc, xử lý từng request.
     * @param request     Đối tượng request đến
     * @param response    Đối tượng response trả về
     * @param filterChain Chuỗi các bộ lọc tiếp theo
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, // @NonNull giúp IDE cảnh báo nếu null
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
    	logger.debug("JwtAuthorizationFilter: Đang xử lý request đến '{}'", request.getRequestURI());

        try {
            // Lấy giá trị header "Authorization"
            String header = request.getHeader("Authorization");

            // Kiểm tra xem header có tồn tại và bắt đầu bằng "Bearer " không
            if (header != null && header.startsWith("Bearer ")) {
                // Tách lấy phần token (bỏ "Bearer " ở đầu - 7 ký tự)
                String token = header.substring(7);

                // Dùng JwtTokenProvider để kiểm tra token (chữ ký, thời hạn)
                if (jwtTokenProvider.validateToken(token)) {
                    // Nếu token hợp lệ, lấy username từ token
                    String username = jwtTokenProvider.getUsernameFromJWT(token); // Đảm bảo tên hàm khớp với Provider

                    // Tải thông tin chi tiết người dùng (bao gồm roles/authorities)
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        // Tạo đối tượng Authentication đã được xác thực
                        // Tham số thứ 2 (credentials) là null vì dùng token
                        // Tham số thứ 3 là danh sách quyền (authorities) lấy từ userDetails
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                        // Đặt đối tượng Authentication vào SecurityContextHolder
                        
                        // Đây là cách báo cho Spring Security biết user đã đăng nhập cho request này
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.debug("Đã xác thực thành công user '{}' từ JWT token.", username);

                    } catch (UsernameNotFoundException e) {
                        // Trường hợp user trong token không còn tồn tại trong DB
                        logger.error("User '{}' từ token không tìm thấy trong CSDL.", username, e);
                        SecurityContextHolder.clearContext(); // Xóa context cũ (nếu có)
                    }
                } else {
                    // Token không hợp lệ (sai chữ ký, hết hạn,...)
                    logger.warn("Token JWT không hợp lệ nhận được từ request: {}", request.getRequestURI());
                    SecurityContextHolder.clearContext();
                }
            }
            // Nếu không có header "Authorization" hoặc không bắt đầu bằng "Bearer ",
            // bộ lọc này không cần làm gì, cứ để request đi tiếp.
            // Spring Security sẽ dựa vào authorizeHttpRequests để quyết định có cho phép hay không.

        } catch (Exception e) {
            // Bắt các lỗi không mong muốn khác trong quá trình xử lý token
            logger.error("Không thể thiết lập xác thực người dùng trong security context", e);
            SecurityContextHolder.clearContext(); // Đảm bảo context được xóa khi có lỗi nghiêm trọng
        }

        // **QUAN TRỌNG:** Chuyển request và response đến bộ lọc tiếp theo trong chuỗi
        filterChain.doFilter(request, response);
    }
}