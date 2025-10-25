package com.pbl4.server.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey jwtSecretKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(@Value("${jwt.secret}") String jwtSecret, 
                            @Value("${jwt.expiration.ms}") long jwtExpirationMs) {
        // Tạo SecretKey an toàn từ chuỗi secret
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
    }

    // 1. Tạo Token từ thông tin Authentication
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. Lấy username từ Token
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser() // <-- Dùng parser()
                .verifyWith(jwtSecretKey) // <-- Dùng verifyWith() thay vì setSigningKey()
                .build()
                .parseSignedClaims(token) // <-- Dùng parseSignedClaims()
                .getPayload(); // <-- Dùng getPayload() thay vì getBody()
                
        return claims.getSubject();
    }

    // 3. Xác thực Token
    public boolean validateToken(String authToken) {
        try {
        	Jwts.parser() // <-- Dùng parser()
            .verifyWith(jwtSecretKey) // <-- Dùng verifyWith()
            .build()
            .parseSignedClaims(authToken); // <-- Dùng parseSignedClaims()
            
        return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}