package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.dto.LoginRequest;
import com.raghunath.smartstore.dto.RegisterRequest;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.AuthService;
import com.raghunath.smartstore.service.UnifiedAuthService;
import com.raghunath.smartstore.exception.InvalidCredentialsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService; // Keep for user registration
    private final UnifiedAuthService unifiedAuthService; // New unified login service
    private final JwtUtil jwtUtil;

    // ================================
    // REGISTRATION (Keep separate for now)
    // ================================

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // ================================
    // UNIFIED LOGIN (New Implementation)
    // ================================

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            String email = request.getEmail();
            String password = request.getPassword();
            String userType = request.getType(); // USER, VENDOR, or EMPLOYEE

            log.info("🔐 Login attempt - Email: {}, Type: {}", email, userType);

            AuthResponse authResponse = unifiedAuthService.login(email, password, userType);

            return ResponseEntity.ok(authResponse);

        } catch (InvalidCredentialsException e) {
            log.warn("❌ Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage(),
                            "error", "INVALID_CREDENTIALS"
                    ));
        } catch (Exception e) {
            log.error("❌ Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Login failed. Please try again.",
                            "error", "INTERNAL_ERROR"
                    ));
        }
    }

    // ================================
    // UNIFIED LOGOUT (New Implementation)
    // ================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        try {
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String email = jwtUtil.extractUsername(actualToken);

            // Extract user type from JWT token
            String userType = jwtUtil.extractClaim(actualToken,
                    claims -> claims.get("role", String.class));

            unifiedAuthService.logout(email, userType);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logged out successfully"
            ));

        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Logout failed",
                            "error", e.getMessage()
                    ));
        }
    }

    // ================================
    // TOKEN REFRESH (Keep existing)
    // ================================

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            if (!jwtUtil.isTokenValid(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired refresh token"));
            }

            String email = jwtUtil.extractUsername(refreshToken);
            String tokenType = jwtUtil.extractClaim(refreshToken,
                    claims -> claims.get("type", String.class));

            if (!"REFRESH".equals(tokenType)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid token type"));
            }

            String role = jwtUtil.extractClaim(refreshToken,
                    claims -> claims.get("role", String.class));

            String newAccessToken = jwtUtil.generateAccessToken(email, role != null ? role : "USER");
            String newRefreshToken = jwtUtil.generateRefreshToken(email);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token refresh failed: " + e.getMessage()));
        }
    }
}
