package pbl4.common.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Lớp này ánh xạ tới bảng 'Users' trong cơ sở dữ liệu.
 * Lưu trữ thông tin tài khoản người dùng quản trị.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String passwordHash;
    private String email;
    private String role;
    private Timestamp createdAt;
    private Integer activeClientId;
    

    public User() {
    }

    public User(int id, String username, String passwordHash, String email, String role, Timestamp createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
    public Integer getActiveClientId() {
        return activeClientId;
    }

    public void setActiveClientId(Integer activeClientId) {
        this.activeClientId = activeClientId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", activeClientId=" + activeClientId +
                ", createdAt=" + createdAt +
                '}';
    }
}