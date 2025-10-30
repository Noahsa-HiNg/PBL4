package com.pbl4.server.repository;

import com.pbl4.server.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> { // Kiểu ID là Integer

    // Spring Data JPA tự hiểu để tạo query tìm user theo username
	Optional<UserEntity> findByUsername(String username);
	@Query("SELECT u FROM UserEntity u " +
	           "WHERE u.username LIKE %:keyword% " +
	           "OR u.email LIKE %:keyword% " +
	           "OR CAST(u.id AS string) = :keyword") // Chuyển ID sang String để so sánh
	    List<UserEntity> searchByKeyword(@Param("keyword") String keyword);
}