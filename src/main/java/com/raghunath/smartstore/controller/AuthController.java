package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.auth.AuthResponse;
import com.raghunath.smartstore.dto.auth.LoginRequest;
import com.raghunath.smartstore.dto.auth.RegisterRequest;
import com.raghunath.smartstore.entity.RefreshToken;
import com.raghunath.smartstore.repository.RefreshTokenRepository;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.AuthService;
import com.raghunath.smartstore.service.UnifiedAuthService;
import com.raghunath.smartstore.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    // REGISTRATION (Keep existing)
    // ================================

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // ================================
    // UNIFIED LOGIN (Keep existing - already good)
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
    // ENHANCED UNIFIED LOGOUT SYSTEM
    // ================================

    /**
     * Enhanced logout endpoint for all user types (USER, VENDOR, EMPLOYEE)
     * Logs out from current device only
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("🔐 Logout request from IP: {}", getClientIP(request));

            // Extract and validate JWT token
            String token = extractTokenFromHeader(authHeader);

            // Validate token before processing
            JwtUtil.TokenValidationResult validation = jwtUtil.validateTokenWithDetails(token);
            if (!validation.isValid() && validation.getClaims() == null) {
                log.warn("❌ Invalid token for logout: {}", validation.getMessage());
                response.put("success", false);
                response.put("error", "INVALID_TOKEN");
                response.put("message", "Invalid or malformed token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Extract user information (works even with expired tokens)
            String email = jwtUtil.extractUsernameFromExpiredToken(token);
            String userType = jwtUtil.extractRoleFromExpiredToken(token);

            if (email == null) {
                log.warn("❌ Cannot extract email from token");
                response.put("success", false);
                response.put("error", "INVALID_TOKEN_CLAIMS");
                response.put("message", "Cannot extract user information from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Default userType if not found
            if (userType == null) {
                userType = "USER";
                log.debug("⚠️ User type not found in token, defaulting to USER for email: {}", email);
            }

            log.info("🔓 Processing logout for user: {} ({})", email, userType);

            // Perform logout (delete refresh tokens for this user and type)
            unifiedAuthService.logout(email, userType);

            // Success response
            response.put("success", true);
            response.put("message", "Logout successful");
            response.put("userType", userType);
            response.put("email", email);
            response.put("timestamp", LocalDateTime.now());

            log.info("✅ Logout successful for user: {} ({})", email, userType);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid token format during logout: {}", e.getMessage());
            response.put("success", false);
            response.put("error", "INVALID_TOKEN_FORMAT");
            response.put("message", "Invalid Authorization header format");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            log.error("❌ Unexpected logout error: {}", e.getMessage());
            response.put("success", false);
            response.put("error", "LOGOUT_FAILED");
            response.put("message", "Logout failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Logout from all devices - invalidates all refresh tokens for user
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, Object>> logoutFromAllDevices(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("🔐 Logout-all request from IP: {}", getClientIP(request));

            String token = extractTokenFromHeader(authHeader);

            // Validate token
            JwtUtil.TokenValidationResult validation = jwtUtil.validateTokenWithDetails(token);
            if (!validation.isValid() && validation.getClaims() == null) {
                response.put("success", false);
                response.put("error", "INVALID_TOKEN");
                response.put("message", "Invalid or malformed token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Extract email (works with expired tokens too)
            String email = jwtUtil.extractUsernameFromExpiredToken(token);

            if (email == null) {
                response.put("success", false);
                response.put("error", "INVALID_TOKEN_CLAIMS");
                response.put("message", "Cannot extract user information from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            log.info("🔓 Processing logout-all for user: {}", email);

            // Logout from all devices (all user types)
            unifiedAuthService.logoutFromAllDevices(email);

            response.put("success", true);
            response.put("message", "Logged out from all devices successfully");
            response.put("email", email);
            response.put("timestamp", LocalDateTime.now());

            log.info("✅ Logout-all successful for user: {}", email);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Logout-all failed: {}", e.getMessage());
            response.put("success", false);
            response.put("error", "LOGOUT_ALL_FAILED");
            response.put("message", "Logout from all devices failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Check authentication status - useful for frontend to verify token validity
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            String token = extractTokenFromHeader(authHeader);

            JwtUtil.TokenValidationResult validation = jwtUtil.validateTokenWithDetails(token);

            if (validation.isValid()) {
                String email = jwtUtil.extractUsername(token);
                String userType = jwtUtil.extractRole(token);
                long remainingTime = jwtUtil.getTokenRemainingTime(token);

                response.put("success", true);
                response.put("authenticated", true);
                response.put("email", email);
                response.put("userType", userType);
                response.put("remainingTime", remainingTime);
                response.put("needsRefresh", jwtUtil.isTokenExpiringSoon(token));

                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("authenticated", false);
                response.put("message", validation.getMessage());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("authenticated", false);
            response.put("message", "Token validation failed");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // ================================
    // TOKEN REFRESH (Keep existing - already excellent)
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

            // Update refresh token in database (manual approach for compatibility)
            var existingTokens = refreshTokenRepository.findByEmailAndUserType(email, userType);
            if (!existingTokens.isEmpty()) {
                refreshTokenRepository.deleteAll(existingTokens);
            }
            RefreshToken newRefreshTokenEntity = new RefreshToken(email, newRefreshToken, userType);
            refreshTokenRepository.save(newRefreshTokenEntity);

            log.info("✅ Tokens refreshed successfully for user: {} type: {}", email, userType);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tokens refreshed successfully",
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken,
                    "tokenType", "Bearer",
                    "expiresIn", jwtUtil.getTokenRemainingTime(newAccessToken),
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

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header format. Expected: Bearer <token>");
        }

        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            throw new IllegalArgumentException("Empty token provided");
        }

        return token;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.trim().isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
