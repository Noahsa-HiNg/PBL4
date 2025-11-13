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

public User updateUser(int id, User userDto, Long currentUserId,String currentUserRole) {
        
	 // Hoặc kiểm tra: if (role != ADMIN) throw...
        if ((id != currentUserId.intValue())&&(!currentUserRole.equals("ADMIN"))) {
            throw new SecurityException("Access Denied: Cannot update another user's profile.");
           
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

        if (userDto.getPassword() != null && !userDto.getPassword().trim().isEmpty()) {
             existingUser.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
        }

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