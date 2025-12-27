package com.pbl4.server.service; // Hoặc package của UserDetailsServiceImpl

import com.pbl4.server.entity.UserEntity; // Import Entity
import com.pbl4.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority; // Import đúng
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Nên có

import java.util.Arrays; // Import đúng
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override 
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Attempting to load user: " + username); 
        
        // 1. Tìm User
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        System.out.println("User found: " + userEntity.getUsername());
        System.out.println("Password hash from DB: " + userEntity.getPasswordHash());

        String roleName = "ROLE_" + userEntity.getRole().toUpperCase(); 
        
        // 3. Tạo danh sách quyền hạn
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        
        return new org.springframework.security.core.userdetails.User(
                userEntity.getUsername(),
                userEntity.getPasswordHash(), 
                authorities);
    }
}