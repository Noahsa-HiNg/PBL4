// File: com.pbl4.server.service.EmailService.java
package com.pbl4.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    // URL trỏ về Frontend (hoặc Backend nếu bạn phục vụ HTML từ Spring)
    
    @Value("${server.public.url}")
    private String frontendBaseUrl; 

    /**
     * [MỚI] Gửi email xác thực tài khoản
     */
    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Xác thực tài khoản của bạn";
        // API này sẽ được tạo trong AuthController
        String verificationUrl = frontendBaseUrl + "/api/auth/verify-email?token=" + token; 
        String message = "Chào mừng bạn! Vui lòng nhấp vào đường dẫn sau để kích hoạt tài khoản: \n" + verificationUrl;

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(toEmail);
        email.setSubject(subject);
        email.setText(message);
        mailSender.send(email);
    }
    
    /**
     * Gửi email quên mật khẩu
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        String subject = "Yêu cầu Đặt lại Mật khẩu";
        // Link này trỏ đến trang Frontend (nơi có form đổi mật khẩu)
        String resetUrl = frontendBaseUrl + "/reset-password.html?token=" + token; 
        String message = "Để đặt lại mật khẩu, vui lòng nhấp vào đường dẫn sau: \n" + resetUrl;
        
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(toEmail);
        email.setSubject(subject);
        email.setText(message);
        mailSender.send(email);
    }
}