package com.pbl4.server.repository;

import pbl4.common.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pbl4.server.entity.ImageEntity;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Long> { 
    // ...
}