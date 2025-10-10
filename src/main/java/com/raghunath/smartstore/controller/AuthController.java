package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.dto.LoginRequest;
import com.raghunath.smartstore.dto.RegisterRequest;
import com.raghunath.smartstore.entity.RefreshToken;
import com.raghunath.smartstore.repository.RefreshTokenRepository;
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

    private final AuthService authService;
    private final UnifiedAuthService unifiedAuthService;
    private final RefreshTokenRepository refreshTokenRepository;
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
    // TOKEN REFRESH (UPDATED WITH USER TYPE SUPPORT)
    // ================================

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                log.warn("❌ Refresh token request with empty token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "error", "MISSING_TOKEN",
                                "message", "Refresh token is required"
                        ));
            }

            if (!jwtUtil.isTokenValid(refreshToken)) {
                log.warn("❌ Invalid or expired refresh token provided");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "success", false,
                                "error", "INVALID_TOKEN",
                                "message", "Invalid or expired refresh token"
                        ));
            }

            String email = jwtUtil.extractUsername(refreshToken);
            if (email == null || email.trim().isEmpty()) {
                log.warn("❌ Cannot extract username from refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "success", false,
                                "error", "INVALID_TOKEN_DATA",
                                "message", "Cannot extract user information from token"
                        ));
            }

            String tokenType = jwtUtil.extractClaim(refreshToken,
                    claims -> claims.get("type", String.class));

            if (!"REFRESH".equals(tokenType)) {
                log.warn("❌ Invalid token type provided: {}", tokenType);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "error", "INVALID_TOKEN_TYPE",
                                "message", "Invalid token type. Refresh token expected."
                        ));
            }

            // Find stored refresh token
            RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found in database"));

            // Verify email matches
            if (!storedToken.getEmail().equals(email)) {
                throw new RuntimeException("Token email mismatch");
            }

            // Get user type from stored token
            String userType = storedToken.getUserType();
            String role = userType != null ? userType : "USER";

            // Generate new tokens
            String newAccessToken = jwtUtil.generateAccessToken(email, role);
            String newRefreshToken = jwtUtil.generateRefreshToken(email);

            // Update refresh token in database
            refreshTokenRepository.deleteByEmailAndUserType(email, userType);
            RefreshToken newRefreshTokenEntity = new RefreshToken(email, newRefreshToken, userType);
            refreshTokenRepository.save(newRefreshTokenEntity);

            log.info("✅ Tokens refreshed successfully for user: {} type: {}", email, userType);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tokens refreshed successfully",
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken,
                    "email", email,
                    "role", role
            ));

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("❌ Refresh token expired: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "TOKEN_EXPIRED",
                            "message", "Refresh token has expired. Please login again."
                    ));
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("❌ Malformed refresh token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "MALFORMED_TOKEN",
                            "message", "Invalid token format"
                    ));
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("❌ Invalid token signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "INVALID_SIGNATURE",
                            "message", "Token signature validation failed"
                    ));
        } catch (Exception e) {
            log.error("❌ Unexpected error during token refresh: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "INTERNAL_ERROR",
                            "message", "Token refresh failed. Please try again"
                    ));
        }
    }
}
