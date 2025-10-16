package com.pbl4.server.repository;
import com.pbl4.server.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {

    /**
     * Phương thức này sẽ được Spring Data JPA tự động triển khai.
     * Nó tìm kiếm một UserEntity dựa trên username.
     *
     * @param username tên người dùng cần tìm
     * @return UserEntity nếu tìm thấy, ngược lại là null
     */
	Optional<UserEntity> findByUsername(String username);

}