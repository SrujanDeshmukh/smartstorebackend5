package com.raghunath.smartstore.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp_records")
public class OtpRecord {
    @Id
    private String id;

    private String email;

    private String otp;

    private LocalDateTime expirationTime;

    private int attempts;

    private LocalDateTime createdAt;
}
