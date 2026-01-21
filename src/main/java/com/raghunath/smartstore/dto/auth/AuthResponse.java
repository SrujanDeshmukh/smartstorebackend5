package com.raghunath.smartstore.dto.auth;

import com.raghunath.smartstore.entity.User;
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
    private Object userData;     // ✅ UserLoginProfile (7 fields)
    private String message;
    private boolean success = true;
    private String city;

    // ✅ MAIN CONSTRUCTOR (4 params - NO userId)
    public AuthResponse(String accessToken, String refreshToken, String userType, Object userData) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userType = userType;
        this.userData = userData;
        this.message = "Login successful";
        this.success = true;

        // ✅ PERFECT SMART CITY LOGIC
        this.city = extractCity(userData, userType);
    }

    // ✅ BACKWARD COMPATIBLE (if still needed)
    public AuthResponse(String accessToken, String refreshToken, String userType, Object userData, String userId) {
        this(accessToken, refreshToken, userType, userData);  // Calls main constructor
    }

    // ✅ Constructor for token refresh only
    public AuthResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.success = true;
        this.city = null;
    }

    // ✅ Constructor for error
    public AuthResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
        this.city = null;
    }

    // ✅ PERFECT SMART CITY EXTRACTION
    private String extractCity(Object userData, String userType) {
        if (userData == null) return null;

        try {
            // ✅ UserLoginProfile DTO
            if (userData instanceof UserLoginProfile) {
                return ((UserLoginProfile) userData).getCity();
            }

            // ✅ Raw User entity (fallback)
            if (userData instanceof User && "USER".equals(userType)) {
                return ((User) userData).getCity();
            }
        } catch (Exception e) {
            // Silent fallback
        }

        return null;  // ✅ Vendor/Employee = null
    }
}
