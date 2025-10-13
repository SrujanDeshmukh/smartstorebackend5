package com.raghunath.smartstore.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String type = "USER"; // Default to USER

    // Custom getter to ensure valid type
    public String getType() {
        if (type == null || type.trim().isEmpty()) {
            return "USER";
        }
        String upperType = type.toUpperCase().trim();

        // Validate type
        if ("USER".equals(upperType) || "VENDOR".equals(upperType) || "EMPLOYEE".equals(upperType)) {
            return upperType;
        }

        return "USER"; // Default fallback
    }
}
