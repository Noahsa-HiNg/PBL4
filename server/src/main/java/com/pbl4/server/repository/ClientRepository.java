package com.pbl4.server.repository;
import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.entity.ClientEntity;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<ClientEntity, Integer> { 
    // Tìm một client cụ thể bằng machineId VÀ user
    Optional<ClientEntity> findByMachineIdAndUser(String machineId, UserEntity user);
    List<ClientEntity> findByUserId(int userId);
    ClientEntity findByIdAndUserId(int id, int userId);
    long countByUserId(int userId);
    int findStatusById(int clientId);

 // Tìm Clients KHÔNG phải là status X (dùng cho Scheduler OFFLINE)
    List<ClientEntity> findByStatusNotAndLastImageReceivedBefore(String status, Timestamp timeThreshold);
 // 1. Tìm client theo trạng thái (cho MonitorService)
    List<ClientEntity> findByStatus(String status);

    // 2. Tìm client theo danh sách trạng thái (tối ưu hơn cho SUSPENDED và PING_SENT)
    List<ClientEntity> findByStatusIn(List<String> statuses);

    // 3. Tìm client đang ACTIVE nhưng đã quá hạn ảnh (ACTIVE -> SUSPENDED)
   
    List<ClientEntity> findByStatusAndLastImageReceivedBefore(String status, Timestamp timeThreshold);
    @Query("SELECT c.user.username FROM ClientEntity c WHERE c.id = :clientId")
    Optional<String> findUsernameByClientId(@Param("clientId") int clientId);
}