package com.pbl4.server.service;

import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pbl4.common.model.User; // DTO
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import io.jsonwebtoken.lang.Arrays;
import jakarta.persistence.EntityNotFoundException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // CREATE
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
        String token = UUID.randomUUID().toString();
        newUser.setEmailVerificationToken(token);
        

        UserEntity savedUser = userRepository.save(newUser);

        emailService.sendVerificationEmail(savedUser.getEmail(), token);
        
        return savedUser;
    }
    public void verifyEmail(String token) {
        UserEntity user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token xác thực không hợp lệ hoặc đã hết hạn."));
        
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null); // Xóa token sau khi đã dùng
        userRepository.save(user);
    }
    public void createPasswordResetToken(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy User với email: " + email));

        String token = UUID.randomUUID().toString();
        // Đặt thời gian hết hạn (ví dụ: 1 giờ)
        Timestamp expiryDate = Timestamp.from(Instant.now().plus(1, ChronoUnit.HOURS)); 

        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(expiryDate);
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    // --- CHỨC NĂNG ĐẶT LẠI MẬT KHẨU (BƯỚC 2) ---
    public void resetPassword(String token, String newPassword) {
        UserEntity user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ."));

        // Kiểm tra token đã hết hạn
        if (user.getPasswordResetTokenExpiry().before(Timestamp.from(Instant.now()))) {
            throw new RuntimeException("Token đã hết hạn.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        
        userRepository.save(user);
    }

    public UserEntity findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    // READ All
    public List<User> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    // READ One
    public User getUserById(int id) {
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(userEntity);
    }

    public User updateUser(int id, User userDto, Long currentUserId, String currentUserRole) {
        
        // --- 1. KIỂM TRA PHÂN QUYỀN (AuthZ) ---
        // NẾU (ID không phải của tôi) VÀ (Role của tôi không phải ADMIN)
        if ((id != currentUserId.intValue()) && (!currentUserRole.equals("ADMIN"))) {
            // THÌ Từ chối
            throw new SecurityException("Access Denied: Cannot update another user's profile.");
        }
        
        // --- 2. TÌM KIẾM ENTITY ---
        UserEntity existingUser = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));

        // --- 3. CẬP NHẬT TRƯỜNG AN TOÀN ---
        
        // Cập nhật Email (nếu được cung cấp)
        if (userDto.getEmail() != null) {
            existingUser.setEmail(userDto.getEmail());
        }
        
        // Cập nhật Mật khẩu (nếu được cung cấp)
        // [SỬA LỖI NPE] Kiểm tra an toàn
        String newPassword = userDto.getPassword();
        if (newPassword != null && !newPassword.trim().isEmpty()) {
             existingUser.setPasswordHash(passwordEncoder.encode(newPassword));
             System.out.println("Đã cập nhật mật khẩu cho user ID: " + id);
        }
        
        // [BỔ SUNG] Cho phép ADMIN cập nhật Role (User thường không thể)
        if (currentUserRole.equals("ADMIN") && userDto.getRole() != null) {
            existingUser.setRole(userDto.getRole());
            System.out.println("Admin đã cập nhật role cho user ID: " + id);
        }
        
        // 4. LƯU VÀ TRẢ VỀ
        UserEntity updatedUser = userRepository.save(existingUser);
        return toDto(updatedUser);
    }
    // DELETE
    public void deleteUser(int id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    private User toDto(UserEntity entity) {
        User dto = new User();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setRole(entity.getRole());
        dto.setCreatedAt(entity.getCreatedAt());
        // KHÔNG BAO GIỜ trả về password hash
        return dto;
    }

	
	public Long getUserIdByUsername(String username) {
	    
	    Optional<UserEntity> userOptional = userRepository.findByUsername(username); 
	    if (userOptional.isPresent()) {
	        // Lấy UserEntity từ Optional
	        UserEntity user = userOptional.get(); 

	        return (long) user.getId();
	    }

	    return null; 
	}
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList(); 
        }

        String searchKeyword = keyword.trim();

        List<UserEntity> entities = userRepository.searchByKeyword(searchKeyword);

        return entities.stream()
                       .map(this::toDto)
                       .collect(Collectors.toList());
    }

}