package com.raghunath.smartstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String userType;
    private Object userData;
    private String userId;       // Changed from Long to String
    private String message;
    private boolean success = true;

    // Constructor for login
    public AuthResponse(String accessToken, String refreshToken, String userType, Object userData, String userId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userType = userType;
        this.userData = userData;
        this.userId = userId;
        this.message = "Login successful";
        this.success = true;
    }

    // Constructor for token refresh only
    public AuthResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.success = true;
    }

    // Constructor for error
    public AuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
}
