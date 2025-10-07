package com.raghunath.smartstore.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "vendors")
public class Vendor {

    @Id
    private String id; // This will be the business vendor id

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobile;

    private String mobileOptional;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String refreshToken;       // Added for refresh token management

    private String upiId;              // For payment reception

    private LocalDateTime registeredAt;

    private Boolean isVerified = false;

    private Boolean isActive = true;   // Object Boolean field

    public Vendor() {
        this.registeredAt = LocalDateTime.now();
    }

    // Explicit getter for primitive boolean isActive()
    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }
}
