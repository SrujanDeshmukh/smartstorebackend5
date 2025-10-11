package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.dto.LoginRequest;
import com.raghunath.smartstore.dto.VendorRegisterRequest;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/vendor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VendorController {

    private final VendorService vendorService;
    private final JwtUtil jwtUtil;

    // ================================
    // VENDOR REGISTRATION
    // ================================

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerVendor(@Valid @RequestBody VendorRegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Vendor registration attempt for email: {}", request.getEmail());

            String result = vendorService.register(request);

            if (result.equals("Vendor registered successfully")) {
                response.put("status", "success");
                response.put("message", result);
                response.put("email", request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", result);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            log.error("Error during vendor registration for {}: {}", request.getEmail(), e.getMessage());
            response.put("status", "error");
            response.put("message", "Registration failed. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ================================
    // VENDOR LOGIN (ENHANCED)
    // ================================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginVendor(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Vendor login attempt for email: {}", request.getEmail());

            AuthResponse authResponse = vendorService.login(request.getEmail(), request.getPassword());

            // Get vendor data for response
            Vendor vendor = vendorService.getVendorByEmail(request.getEmail());

            response.put("success", true);
            response.put("message", "Login successful");
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("userType", "VENDOR");
            response.put("userData", vendor);
            response.put("userId", vendor.getId());

            log.info("✅ Vendor login successful for: {}", request.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Vendor login failed for {}: {}", request.getEmail(), e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "INVALID_CREDENTIALS");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // ================================
    // TOKEN REFRESH (ENHANCED)
    // ================================

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "MISSING_TOKEN");
                response.put("message", "Refresh token is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            AuthResponse authResponse = vendorService.refreshAccessToken(refreshToken);

            response.put("success", true);
            response.put("message", "Token refreshed successfully");
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Token refresh failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "TOKEN_REFRESH_FAILED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // ================================
    // VENDOR LOGOUT (FIXED)
    // ================================

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutVendor(@RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;

            // Extract email from JWT token
            String email = jwtUtil.extractUsername(actualToken);

            // Validate token role
            String role = jwtUtil.extractRole(actualToken);
            if (!"VENDOR".equals(role)) {
                response.put("success", false);
                response.put("message", "Invalid token type");
                response.put("error", "INVALID_TOKEN_TYPE");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Call logout service
            vendorService.logout(email);

            response.put("success", true);
            response.put("message", "Logged out successfully");

            log.info("✅ Vendor logged out successfully: {}", email);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Logout failed: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Logout failed");
            response.put("error", "LOGOUT_FAILED");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ================================
    // VENDOR PROFILE MANAGEMENT (ENHANCED)
    // ================================

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getCurrentVendorProfile(@RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String email = jwtUtil.extractUsername(actualToken);

            // Validate token role
            String role = jwtUtil.extractRole(actualToken);
            if (!"VENDOR".equals(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Vendor role required.");
                response.put("error", "ACCESS_DENIED");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            Vendor vendor = vendorService.getVendorByEmail(email);

            response.put("success", true);
            response.put("vendor", vendor);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching vendor profile: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "PROFILE_FETCH_FAILED");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // Backward compatibility endpoint
    @GetMapping("/profile/by-email")
    public ResponseEntity<Map<String, Object>> getVendorProfileByEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            Vendor vendor = vendorService.getVendorByEmail(email);

            response.put("success", true);
            response.put("vendor", vendor);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching vendor profile for {}: {}", email, e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "PROFILE_FETCH_FAILED");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // ================================
    // UPI ID UPDATE (ENHANCED)
    // ================================

    @PutMapping("/upi")
    public ResponseEntity<Map<String, Object>> updateUpiId(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String email = request.get("email");
            String upiId = request.get("upiId");

            if (email == null || upiId == null) {
                response.put("success", false);
                response.put("message", "Email and UPI ID are required");
                response.put("error", "MISSING_FIELDS");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String result = vendorService.updateUpiId(email, upiId);

            response.put("success", true);
            response.put("message", result);

            log.info("✅ UPI ID updated for vendor: {}", email);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error updating UPI ID: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "UPI_UPDATE_FAILED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Enhanced UPI update with JWT token
    @PutMapping("/upi/secure")
    public ResponseEntity<Map<String, Object>> updateUpiIdSecure(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            String email = jwtUtil.extractUsername(actualToken);

            // Validate token role
            String role = jwtUtil.extractRole(actualToken);
            if (!"VENDOR".equals(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Vendor role required.");
                response.put("error", "ACCESS_DENIED");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            String upiId = request.get("upiId");
            if (upiId == null || upiId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "UPI ID is required");
                response.put("error", "MISSING_UPI_ID");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String result = vendorService.updateUpiId(email, upiId);

            response.put("success", true);
            response.put("message", result);
            response.put("upiId", upiId);

            log.info("✅ UPI ID updated securely for vendor: {}", email);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error updating UPI ID securely: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", "UPI_UPDATE_FAILED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
