package com.pbl4.server.repository;

import com.pbl4.server.entity.CameraEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CameraRepository extends JpaRepository<CameraEntity, Integer> {
	List<CameraEntity> findByClientUserId(int userId);
	List<CameraEntity> findByClientId(int clientId);
	long countByIsActive(boolean isActive);
	long countByClientUserId(int userId);
}