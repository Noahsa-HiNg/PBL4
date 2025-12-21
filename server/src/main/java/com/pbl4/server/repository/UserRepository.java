package com.pbl4.server.repository;

import com.pbl4.server.dto.UserRankingDto;
import com.pbl4.server.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
	Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByEmailVerificationToken(String token);
    Optional<UserEntity> findByPasswordResetToken(String token);
    @Query("SELECT u.id as userId, u.username as username, COUNT(i) as value " +
            "FROM UserEntity u " + 
            "JOIN ClientEntity c ON c.user.id = u.id " +     // Giả định trong ClientEntity có biến 'user'
            "JOIN CameraEntity cam ON cam.client.id = c.id " + // Giả định trong CameraEntity có biến 'client'
            "JOIN ImageEntity i ON i.camera.id = cam.id " +    // Giả định trong ImageEntity có biến 'camera'
            "GROUP BY u.id, u.username " +
            "ORDER BY value DESC")
     List<UserRankingDto> findTopUsersByImageCount(Pageable pageable);

     // 3. Xếp hạng user theo tổng số camera
     @Query("SELECT u.id as userId, u.username as username, COUNT(cam) as value " +
            "FROM UserEntity u " +
            "JOIN ClientEntity c ON c.user.id = u.id " +
            "JOIN CameraEntity cam ON cam.client.id = c.id " +
            "GROUP BY u.id, u.username " +
            "ORDER BY value DESC")
     List<UserRankingDto> findTopUsersByCameraCount(Pageable pageable);
}