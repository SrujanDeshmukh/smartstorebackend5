package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.auth.AuthResponse;
import com.raghunath.smartstore.dto.auth.LoginRequest;
import com.raghunath.smartstore.exception.BadRequestException;
import com.raghunath.smartstore.exception.InvalidCredentialsException;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.UnifiedAuthService;
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
@RequestMapping("/unified-auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;
    private final JwtUtil jwtUtil;

    // ================================
    // UNIFIED LOGIN (ALL USER TYPES)
    // ================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            String email = request.getEmail();
            String password = request.getPassword();
            String userType = request.getType(); // USER, VENDOR, or EMPLOYEE

            log.info("🔐 Unified login attempt - Email: {}, Type: {}", email, userType);

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
    // UNIFIED TOKEN REFRESH
    // ================================
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                throw new BadRequestException("Refresh token is required");
            }

            AuthResponse authResponse = unifiedAuthService.refreshAccessToken(refreshToken);

            Map<String, Object> body = Map.of(
                    "success", true,
                    "message", "Tokens refreshed successfully",
                    "accessToken", authResponse.getAccessToken(),
                    "refreshToken", authResponse.getRefreshToken(),
                    "userType", authResponse.getUserType(),
                    "tokenType", "Bearer",
                    "expiresIn", jwtUtil.getTokenRemainingTime(authResponse.getAccessToken())
            );

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("❌ Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage(),
                            "error", "TOKEN_REFRESH_FAILED"
                    ));
        }
    }

    // ================================
    // UNIFIED LOGOUT (CURRENT DEVICE)
    // ================================
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("🔐 Logout request from IP: {}", getClientIP(request));

            String token = extractTokenFromHeader(authHeader);

            JwtUtil.TokenValidationResult validation = jwtUtil.validateTokenWithDetails(token);
            if (!validation.isValid() && validation.getClaims() == null) {
                log.warn("❌ Invalid token for logout: {}", validation.getMessage());
                response.put("success", false);
                response.put("error", "INVALID_TOKEN");
                response.put("message", "Invalid or malformed token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String email = jwtUtil.extractUsernameFromExpiredToken(token);
            String userType = jwtUtil.extractRoleFromExpiredToken(token);

            if (email == null) {
                log.warn("❌ Cannot extract email from token");
                response.put("success", false);
                response.put("error", "INVALID_TOKEN_CLAIMS");
                response.put("message", "Cannot extract user information from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            if (userType == null) {
                userType = "USER";
                log.debug("⚠️ User type not found in token, defaulting to USER for email: {}", email);
            }

            log.info("🔓 Processing logout for user: {} ({})", email, userType);

            unifiedAuthService.logout(email, userType);

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

    // ================================
    // UNIFIED LOGOUT (ALL DEVICES)
    // ================================
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, Object>> logoutFromAllDevices(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("🔐 Logout-all request from IP: {}", getClientIP(request));

            String token = extractTokenFromHeader(authHeader);

            JwtUtil.TokenValidationResult validation = jwtUtil.validateTokenWithDetails(token);
            if (!validation.isValid() && validation.getClaims() == null) {
                response.put("success", false);
                response.put("error", "INVALID_TOKEN");
                response.put("message", "Invalid or malformed token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String email = jwtUtil.extractUsernameFromExpiredToken(token);

            if (email == null) {
                response.put("success", false);
                response.put("error", "INVALID_TOKEN_CLAIMS");
                response.put("message", "Cannot extract user information from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            log.info("🔓 Processing logout-all for user: {}", email);

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

    // ================================
    // AUTH STATUS CHECK
    // ================================
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
    // UTILITY METHODS
    // ================================
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

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.trim().isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
