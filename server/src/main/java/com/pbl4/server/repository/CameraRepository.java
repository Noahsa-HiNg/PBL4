package com.pbl4.server.repository;

import com.pbl4.server.entity.CameraEntity; // <-- SỬA LẠI IMPORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// SỬA LẠI Ở ĐÂY:
public interface CameraRepository extends JpaRepository<CameraEntity, Integer> { 
}