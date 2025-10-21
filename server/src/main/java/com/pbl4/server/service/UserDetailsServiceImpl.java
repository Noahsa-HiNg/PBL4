package com.pbl4.server.service;

import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList; // Hoặc dùng Collections.emptyList()

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm UserEntity trong DB
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // 2. Tạo đối tượng UserDetails mà Spring Security hiểu
        // Tham số: username, password hash (từ DB), danh sách quyền hạn (roles/authorities)
        // Hiện tại dùng danh sách quyền hạn rỗng. Bạn có thể thêm role vào đây nếu cần phân quyền chi tiết.
        return new User(userEntity.getUsername(), userEntity.getPasswordHash(), new ArrayList<>());
    }
}