package com.example.loginauth.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "username_reviews")
public class UsernameReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 16)
    private String decision;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(length = 128)
    private String model;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UsernameReview() {
    }

    public UsernameReview(String username, String decision, String reasonCode, String model) {
        this.username = username;
        this.decision = decision;
        this.reasonCode = reasonCode;
        this.model = model;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
