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
import java.util.List;
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

    // READ All
    public List<User> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    // READ One
    public User getUserById(int id) {
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(userEntity);
    }

    // UPDATE
    public User updateUser(int id, User userDto) {
        UserEntity existingUser = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        existingUser.setEmail(userDto.getEmail());
        existingUser.setRole(userDto.getRole());
        // Thêm logic cập nhật mật khẩu nếu cần
        
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

}