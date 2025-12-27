package com.pbl4.server.repository;

import com.pbl4.server.entity.EmailChangeToken;
import com.pbl4.server.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Integer> {
    EmailChangeToken findByToken(String token);
    EmailChangeToken findByUser(UserEntity user); 
}