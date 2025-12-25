package com.pbl4.server.security; 


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


public class JwtAuthorizationFilter extends OncePerRequestFilter {

  
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthorizationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsServiceImpl userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
    	logger.debug("JwtAuthorizationFilter: Đang xử lý request đến '{}'", request.getRequestURI());

        try {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {
              
                String token = header.substring(7);

                if (jwtTokenProvider.validateToken(token)) {
            
                    String username = jwtTokenProvider.getUsernameFromJWT(token); 

               
                    try {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

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


        } catch (Exception e) {
            // Bắt các lỗi không mong muốn khác trong quá trình xử lý token
            logger.error("Không thể thiết lập xác thực người dùng trong security context", e);
            SecurityContextHolder.clearContext(); // Đảm bảo context được xóa khi có lỗi nghiêm trọng
        }

        // **QUAN TRỌNG:** Chuyển request và response đến bộ lọc tiếp theo trong chuỗi
        filterChain.doFilter(request, response);
    }
}