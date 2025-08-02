package com.raghunath.smartstore.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "otp_records")
public class OtpRecord {

    @Id
    private String id;
    private String email;
    private String otp;
    private LocalDateTime createdAt;

    public OtpRecord(String email, String otp){
        this.email = email;
        this.otp = otp;
        this.createdAt = LocalDateTime.now();
    }

    public String getEmail() { return email; }
    public String getOtp() { return otp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
