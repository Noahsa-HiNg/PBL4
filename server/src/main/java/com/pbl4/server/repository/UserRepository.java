package com.pbl4.server.repository;

import com.pbl4.server.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * Phương thức này sẽ được Spring Data JPA tự động triển khai.
     * Nó tìm kiếm một UserEntity dựa trên username.
     *
     * @param username tên người dùng cần tìm
     * @return UserEntity nếu tìm thấy, ngược lại là null
     */
    UserEntity findByUsername(String username);

}