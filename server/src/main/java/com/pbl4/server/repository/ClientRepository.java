package com.pbl4.server.repository;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.entity.ClientEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Integer> { 
    // Tìm một client cụ thể bằng machineId VÀ user
    Optional<ClientEntity> findByMachineIdAndUser(String machineId, UserEntity user);
}