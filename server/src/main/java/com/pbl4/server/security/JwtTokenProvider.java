package com.pbl4.server.security; // Hoặc com.pbl4.server.service

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component; // Cần @Component

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function; // Cần import này nếu bạn dùng getClaimFromToken helper

@Component // Đánh dấu là Spring Bean
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey jwtSecretKey;
    private final long jwtExpirationMs;

    // Constructor đọc giá trị từ application.properties
    public JwtTokenProvider(@Value("${jwt.secret}") String jwtSecret,
                            @Value("${jwt.expiration.ms}") long jwtExpirationMs) {
        if (jwtSecret == null || jwtSecret.length() < 32) {
             logger.warn("Cảnh báo: jwt.secret quá ngắn! Nên sử dụng chuỗi bí mật dài ít nhất 256 bits (32 ký tự an toàn).");
             // Có thể ném lỗi ở đây nếu muốn bắt buộc secret mạnh
             // throw new IllegalArgumentException("jwt.secret must be at least 32 characters long");
        }
        // Tạo SecretKey an toàn
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
        logger.info("JwtTokenProvider khởi tạo với thời gian hết hạn {} ms", jwtExpirationMs);
    }

    // Tạo SecretKey (có thể đặt private)
    private SecretKey getSigningKey() {
        return jwtSecretKey;
    }

    // Tạo Token
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername()) // Dùng setSubject thay vì subject
                .issuedAt(now)                      // Dùng setIssuedAt thay vì issuedAt
                .expiration(expiryDate)             // Dùng setExpiration thay vì expiration
                // Chỉ cần truyền SecretKey, thư viện tự biết thuật toán (HS256)
                .signWith(getSigningKey())
                .compact();
    }

    // Lấy username từ Token
    public String getUsernameFromJWT(String token) { // Đổi tên hàm cho khớp SecurityConfig
         return getClaimFromToken(token, Claims::getSubject);
    }

    // Xác thực Token
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey()) // Dùng SecretKey để xác thực
                .build()
                .parseSignedClaims(authToken); // Parse và kiểm tra
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Token JWT không hợp lệ: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Token JWT đã hết hạn: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Token JWT không được hỗ trợ: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("Chuỗi claims JWT trống: {}", ex.getMessage());
        } catch (io.jsonwebtoken.security.SecurityException ex) { // Bắt cả lỗi chữ ký sai
             logger.error("Chữ ký JWT không hợp lệ: {}", ex.getMessage());
        }
        return false;
    }

    // Hàm helper để lấy claim (có thể đặt private)
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                                  .verifyWith(getSigningKey())
                                  .build()
                                  .parseSignedClaims(token)
                                  .getPayload(); // Lấy payload
        return claimsResolver.apply(claims);
    }
}