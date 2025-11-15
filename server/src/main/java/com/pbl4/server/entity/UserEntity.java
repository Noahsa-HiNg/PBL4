package com.pbl4.server.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table(name = "Users")
public class UserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String email;

    @Column(nullable = false)
    private String role;

    // Liên kết 1-1: User có 1 Client "đang hoạt động"
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_client_id")
    private ClientEntity activeClient;

    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    // Liên kết 1-Nhiều: 1 User sở hữu nhiều Client
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ClientEntity> clients;
    @Column(name = "email_verification_token")
    private String emailVerificationToken;
    
    @Column(name = "email_verified")
    private boolean emailVerified = false; // Mặc định là false

    // --- BỔ SUNG CHO QUÊN MẬT KHẨU ---
    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private Timestamp passwordResetTokenExpiry; // Thời gian token hết hạn

    // --- Thêm Getters và Setters cho các trường mới ---
    
    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String token) { this.emailVerificationToken = token; }
    
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean verified) { this.emailVerified = verified; }
    
    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public Timestamp getPasswordResetTokenExpiry() { return passwordResetTokenExpiry; }
    public void setPasswordResetTokenExpiry(Timestamp passwordResetTokenExpiry) { this.passwordResetTokenExpiry = passwordResetTokenExpiry; }
    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public ClientEntity getActiveClient() { return activeClient; }
    public void setActiveClient(ClientEntity activeClient) { this.activeClient = activeClient; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Set<ClientEntity> getClients() { return clients; }
    public void setClients(Set<ClientEntity> clients) { this.clients = clients; }
}