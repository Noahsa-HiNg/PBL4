package com.pbl4.server.repository;

import com.pbl4.server.entity.ImageEntity;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page; // Thêm import
import org.springframework.data.domain.Pageable;
@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
	Page<ImageEntity> findByCameraId(int cameraId, Pageable pageable);
	Page<ImageEntity> findByCameraClientUserId(int userId, Pageable pageable);
	
	Page<ImageEntity> findAllByOrderByCapturedAtDesc(Pageable pageable);
	Page<ImageEntity> findByCameraIdOrderByCapturedAtDesc(int cameraId, Pageable pageable);
	Page<ImageEntity> findByCameraClientUserIdOrderByCapturedAtDesc(int userId, Pageable pageable);
	Page<ImageEntity> findByCameraClientUserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            int userId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );
	Page<ImageEntity> findByCameraIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            int cameraId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );
	Optional<ImageEntity> findFirstByCameraIdOrderByCapturedAtDesc(int cameraId);
	List<ImageEntity> findByCameraId(int cameraId);
	@Modifying
    @Transactional
    void deleteAllByCameraId(int cameraId);
	long countByCameraClientUserId(int userId);
	long countByCameraId(int cameraId);
	
}