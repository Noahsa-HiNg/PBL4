package com.pbl4.server.service;

import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pbl4.common.model.User; // DTO
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import io.jsonwebtoken.lang.Arrays;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // CREATE
    public User createUser(User userDto) {
        if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists!");
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(userDto.getUsername());
        userEntity.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
        userEntity.setEmail(userDto.getEmail());
        userEntity.setRole(userDto.getRole() != null ? userDto.getRole() : "viewer");
        userEntity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        
        UserEntity savedUser = userRepository.save(userEntity);
        return toDto(savedUser);
    }

    public UserEntity findByUsername(String username) {
        // Giả định userRepository là dependency đã được tiêm
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

public User updateUser(int id, User userDto, Long currentUserId) {
        
        // --- 1. KIỂM TRA BẢO MẬT (NGHIÊM NGẶT) ---
        // User chỉ được cập nhật tài khoản của chính mình (ID từ URL phải khớp với ID từ Token)
        if (id != currentUserId.intValue()) {
            throw new SecurityException("Access Denied: Cannot update another user's profile.");
            // Hoặc kiểm tra: if (role != ADMIN) throw...
        }
        
        // 2. TÌM KIẾM ENTITY
        UserEntity existingUser = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found")); // Trả về 404/500

        // 3. CẬP NHẬT TRƯỜNG AN TOÀN
        
        // Cập nhật Email (Frontend của bạn đang gửi trường này)
        if (userDto.getEmail() != null) {
            existingUser.setEmail(userDto.getEmail());
        }
        
        // Bảo mật: CHỈ ĐƯỢC PHÉP CẬP NHẬT ROLE BỞI ADMIN HOẶC NẾU CẦN
        // Nếu Frontend KHÔNG gửi role, không ghi đè giá trị này.
        // XÓA DÒNG NÀY: existingUser.setRole(userDto.getRole());
        
        // Thêm logic cập nhật mật khẩu nếu cần:
        if (userDto.getPassword() != null && !userDto.getPassword().trim().isEmpty()) {
             existingUser.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
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
	    
	    // 1. Repository trả về Optional<UserEntity>
	    Optional<UserEntity> userOptional = userRepository.findByUsername(username); 
	    
	    // 2. Kiểm tra và trích xuất dữ liệu một cách an toàn
	    if (userOptional.isPresent()) {
	        // Lấy UserEntity từ Optional
	        UserEntity user = userOptional.get(); 
	        
	        // Trả về ID (chuyển đổi int -> Long)
	        return (long) user.getId();
	    }
	    
	    // 3. Nếu không tìm thấy, trả về null
	    return null; 
	}
	/**
     * [BỔ SUNG MỚI] Lấy danh sách Users khớp với từ khóa tìm kiếm.
     * @param keyword Từ khóa tìm kiếm (có thể là tên, email, hoặc ID).
     * @return Danh sách User DTO.
     */
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList(); // Trả về danh sách trống nếu không có từ khóa
        }
        
        // Loại bỏ khoảng trắng và chuyển thành dạng truy vấn
        String searchKeyword = keyword.trim();
        
        // Gọi phương thức Repository đã tạo truy vấn phức tạp
        List<UserEntity> entities = userRepository.searchByKeyword(searchKeyword);
        
        // Chuyển đổi sang DTO và trả về
        return entities.stream()
                       .map(this::toDto)
                       .collect(Collectors.toList());
    }

}