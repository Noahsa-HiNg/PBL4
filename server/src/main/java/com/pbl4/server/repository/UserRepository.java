package com.pbl4.server.repository;

import com.pbl4.server.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> { // Kiểu ID là Integer

    // Spring Data JPA tự hiểu để tạo query tìm user theo username
    Optional<UserEntity> findByUsername(String username);
}