package com.pbl4.server.repository;

import com.pbl4.server.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page; // Thêm import
import org.springframework.data.domain.Pageable;
@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
	Page<ImageEntity> findByCameraId(int cameraId, Pageable pageable);
	Page<ImageEntity> findByCameraClientUserId(int userId, Pageable pageable);
}