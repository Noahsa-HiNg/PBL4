package com.pbl4.server.entity;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;

@Entity
@Table(name = "email_change_tokens")
public class EmailChangeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String token;

    @Column(name = "new_email", nullable = false)
    private String newEmail; // Đây là nơi lưu email mới chờ xác thực

    @OneToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private UserEntity user;

    @Column(name = "expiry_date", nullable = false)
    private Timestamp expiryDate;

    // --- Constructors ---
    public EmailChangeToken() {}

    public EmailChangeToken(UserEntity user, String newEmail) {
        this.user = user;
        this.newEmail = newEmail;
        this.token = UUID.randomUUID().toString(); // Tự động sinh token ngẫu nhiên
        
        // Set hết hạn sau 15 phút
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, 15);
        this.expiryDate = new Timestamp(now.getTimeInMillis());
    }

    // --- Getters & Setters ---
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getNewEmail() { return newEmail; }
    public UserEntity getUser() { return user; }
    public Timestamp getExpiryDate() { return expiryDate; }
}