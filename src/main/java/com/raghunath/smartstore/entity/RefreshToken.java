package com.raghunath.smartstore.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Document(collection = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    private String id;

    @Indexed
    private String email;

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String userType; // "USER", "VENDOR", "EMPLOYEE"

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Indexed(expireAfter = "7d") // Auto-delete after 7 days
    private LocalDateTime expiresAt;

    // Constructor for creating new refresh token
    public RefreshToken(String email, String token) {
        this.email = email;
        this.token = token;
        this.userType = "USER"; // Default type
        this.expiresAt = LocalDateTime.now().plusDays(7);
    }

    // Constructor with user type
    public RefreshToken(String email, String token, String userType) {
        this.email = email;
        this.token = token;
        this.userType = userType;
        this.expiresAt = LocalDateTime.now().plusDays(7);
    }
}
