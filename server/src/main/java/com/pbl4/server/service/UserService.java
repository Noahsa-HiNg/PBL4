package com.pbl4.server.service;

import com.pbl4.server.entity.UserEntity;
import com.pbl4.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Phương thức xác thực người dùng.
     * @param username Tên đăng nhập người dùng.
     * @param password Mật khẩu thô (chưa mã hóa) người dùng nhập vào.
     * @return UserEntity nếu xác thực thành công, ngược lại là null.
     */
    public UserEntity authenticate(String username, String password) {
        // Đây là ví dụ đơn giản, bạn nên thay thế bằng logic truy vấn database thực tế
        // với mật khẩu đã được mã hóa (password hashing).
        
        // Ví dụ truy cập database:
        // UserEntity user = userRepository.findByUsername(username);
        // if (user != null && passwordEncoder.matches(password, user.getPasswordHash())) {
        //     return user;
        // }

        // Ví dụ đơn giản của bạn:
        if ("admin".equals(username) && "password123".equals(password)) {
            UserEntity user = new UserEntity();
            user.setUsername("admin");
            // ... các thuộc tính khác
            return user;
        }

        return null;
    }

    /**
     * Phương thức tạo JWT Token.
     * @param user Đối tượng UserEntity của người dùng đã xác thực.
     * @return Chuỗi JWT Token.
     */
    public String generateJwtToken(UserEntity user) {
        // Thực tế: Sử dụng thư viện như JJWT để tạo JWT token.
        return "chuỗi_jwt_token_do_server_tạo_ra";
    }
}