package com.pbl4.server.repository;

import com.pbl4.server.entity.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CameraRepository extends JpaRepository<CameraEntity, Integer> {
}