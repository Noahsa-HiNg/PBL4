package com.pbl4.server.repository;

import com.pbl4.server.entity.CameraEntity;

import pbl4.common.model.Camera;

import java.util.Optional;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
@Repository
public interface CameraRepository extends JpaRepository<CameraEntity, Integer> {
	List<CameraEntity> findByClientUserId(int userId);
	List<CameraEntity> findByClientId(int clientId);
	long countByIsActive(boolean isActive);
	long countByClientUserId(int userId);
	@Modifying
    @Transactional
    @Query("UPDATE CameraEntity c SET c.isActive = :isActive WHERE c.client.id = :clientId")
    int updateAllByClientId(@Param("clientId") int clientId, @Param("isActive") boolean isActive);
	Optional<CameraEntity> findByIpAddressAndUsername(String ipAddress, String username);
	Optional<CameraEntity> findByIpAddressAndClient_Id(String ipAddress, int clientId);
	Optional<CameraEntity> findByIdAndClientUserId(int cameraId, int userId);
	@Modifying // Bắt buộc vì đây là câu lệnh thay đổi dữ liệu (DELETE/UPDATE)
    @Query("DELETE FROM CameraEntity c WHERE c.id = :cameraId")
    void deleteCameraByIdCustom(@Param("cameraId") int cameraId);
	Optional<Camera> findById(Long id);
	
	

}