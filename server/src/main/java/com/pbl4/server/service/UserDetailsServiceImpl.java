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

    @Override // Thêm @Override
    @Transactional // Đảm bảo hoạt động trong 1 transaction
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Attempting to load user: " + username); 
        
        // 1. Tìm User
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        System.out.println("User found: " + userEntity.getUsername());
        System.out.println("Password hash from DB: " + userEntity.getPasswordHash());

        // 2. KHẮC PHỤC LỖI QUAN TRỌNG NHẤT (403 Forbidden)
        // Spring Security yêu cầu vai trò phải có tiền tố "ROLE_"
        String roleName = "ROLE_" + userEntity.getRole().toUpperCase(); 
        
        // 3. Tạo danh sách quyền hạn
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        // 4. Trả về đối tượng UserDetails của Spring Security
        return new org.springframework.security.core.userdetails.User(
                userEntity.getUsername(),
                userEntity.getPasswordHash(), // Lấy hash từ Entity
                authorities);
    }
}