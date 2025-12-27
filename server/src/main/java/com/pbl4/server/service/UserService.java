package com.pbl4.server.service;

import com.pbl4.server.dto.ChangePasswordRequest;
import com.pbl4.server.entity.EmailChangeToken;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.EmailChangeTokenRepository;
import com.pbl4.server.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pbl4.common.model.User; // DTO

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailChangeTokenRepository emailChangeTokenRepository; // Chỉ giữ lại 1 biến này
    
    @Autowired
    private EmailService emailService;

    // Sử dụng Constructor Injection cho tất cả Repository (Best practice)
    public UserService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder,
                       EmailChangeTokenRepository emailChangeTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailChangeTokenRepository = emailChangeTokenRepository;
    }

    // --- 1. ĐĂNG KÝ USER ---
    @Transactional
    public UserEntity registerUser(User userDto) { 
        if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            throw new RuntimeException("Username đã tồn tại.");
        }
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại.");
        }

        UserEntity newUser = new UserEntity();
        newUser.setUsername(userDto.getUsername());
        newUser.setEmail(userDto.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
        
        newUser.setRole("VIEWER"); 
        newUser.setEmailVerified(false); 
        
        // Token kích hoạt tài khoản (Lưu trong bảng Users)
        String token = UUID.randomUUID().toString();
        newUser.setEmailVerificationToken(token);

        UserEntity savedUser = userRepository.save(newUser);

        // Gửi mail kích hoạt đăng ký
        emailService.sendVerificationEmail(savedUser.getEmail(), token);
        
        return savedUser;
    }

    @Transactional
    public void verifyEmail(String token) {
        UserEntity user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token xác thực không hợp lệ hoặc đã hết hạn."));
        
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null); 
        userRepository.save(user);
    }

    // --- 2. QUÊN MẬT KHẨU ---
    @Transactional
    public void createPasswordResetToken(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy User với email: " + email));

        String token = UUID.randomUUID().toString();
        Timestamp expiryDate = Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)); 

        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(expiryDate);
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        UserEntity user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ."));

        if (user.getPasswordResetTokenExpiry().before(Timestamp.from(Instant.now()))) {
            throw new RuntimeException("Token đã hết hạn.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        
        userRepository.save(user);
    }

    // --- 3. ĐỌC DỮ LIỆU ---
    public UserEntity findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public User getUserById(int id) {
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(userEntity);
    }

    public Long getUserIdByUsername(String username) {
        Optional<UserEntity> userOptional = userRepository.findByUsername(username); 
        return userOptional.map(userEntity -> (long) userEntity.getId()).orElse(null);
    }

    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList(); 
        }
        return userRepository.searchByKeyword(keyword.trim()).stream()
                       .map(this::toDto)
                       .collect(Collectors.toList());
    }

    // --- 4. CẬP NHẬT THÔNG TIN ---
    @Transactional
    public User updateUser(int id, User userDto, Long currentUserId, String currentUserRole) {
        
        // Check AuthZ
        if ((id != currentUserId.intValue()) && (!currentUserRole.equals("ADMIN"))) {
            throw new SecurityException("Access Denied: Cannot update another user's profile.");
        }
        
        UserEntity existingUser = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        String newPassword = userDto.getPassword();
        if (newPassword != null && !newPassword.trim().isEmpty()) {
             existingUser.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        if (currentUserRole.equals("ADMIN") && userDto.getRole() != null) {
            existingUser.setRole(userDto.getRole());
        }
        
        UserEntity updatedUser = userRepository.save(existingUser);
        return toDto(updatedUser);
    }

    @Transactional
    public void deleteUser(int id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu hiện tại không đúng!");
        }

        String encodedNewPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(encodedNewPassword);
        userRepository.save(user);
    }

    @Transactional
    public void requestEmailChange(int userId, String newEmail) throws Exception {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User không tồn tại"));
        if (userRepository.existsByEmail(newEmail)) {
            throw new Exception("EMAIL_EXIST"); 
        }
        EmailChangeToken oldToken = emailChangeTokenRepository.findByUser(user);
        if (oldToken != null) {
            emailChangeTokenRepository.delete(oldToken);
        }

        // Tạo token mới
        EmailChangeToken tokenEntity = new EmailChangeToken(user, newEmail);
        emailChangeTokenRepository.save(tokenEntity);
        emailService.sendEmailChangeVerification(newEmail, tokenEntity.getToken());
    }

    @Transactional
    public void verifyEmailChange(String token) {
        EmailChangeToken changeToken = emailChangeTokenRepository.findByToken(token);
        
        if (changeToken == null) {
            throw new RuntimeException("Mã xác thực không hợp lệ.");
        }

        if (changeToken.getExpiryDate().before(new java.sql.Timestamp(System.currentTimeMillis()))) {
            throw new RuntimeException("Mã xác thực đã hết hạn.");
        }

        UserEntity user = changeToken.getUser();
        String newEmail = changeToken.getNewEmail();

        if (userRepository.existsByEmail(newEmail)) {
             throw new RuntimeException("Email này đã được đăng ký bởi người khác.");
        }

        // Cập nhật chính thức
        user.setEmail(newEmail);
        userRepository.save(user);

        emailChangeTokenRepository.delete(changeToken);
    }

    // --- UTIL ---
    private User toDto(UserEntity entity) {
        User dto = new User();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setRole(entity.getRole());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}