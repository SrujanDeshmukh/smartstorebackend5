package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.dto.LoginRequest;
import com.raghunath.smartstore.dto.VendorRegisterRequest;
import com.raghunath.smartstore.entity.Vendor;
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
    // VENDOR LOGIN
    // ================================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginVendor(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Vendor login attempt for email: {}", request.getEmail());

            AuthResponse authResponse = vendorService.login(request.getEmail(), request.getPassword());

            response.put("status", "success");
            response.put("message", "Login successful");
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());
            response.put("email", request.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Vendor login failed for {}: {}", request.getEmail(), e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // ================================
    // TOKEN REFRESH
    // ================================

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestParam String refreshToken) {
        Map<String, Object> response = new HashMap<>();

        try {
            AuthResponse authResponse = vendorService.refreshAccessToken(refreshToken);

            response.put("status", "success");
            response.put("message", "Token refreshed successfully");
            response.put("accessToken", authResponse.getAccessToken());
            response.put("refreshToken", authResponse.getRefreshToken());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // ================================
    // VENDOR LOGOUT
    // ================================

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logoutVendor(@RequestHeader("Authorization") String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            // Extract email from JWT token (you'll need JwtUtil)
            // String email = jwtUtil.extractUsername(actualToken);

            // For now, you can get email from request body or implement JWT extraction
            // vendorService.logout(email);

            response.put("status", "success");
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "Logout failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ================================
    // VENDOR PROFILE MANAGEMENT
    // ================================

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getVendorProfile(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();

        try {
            Vendor vendor = vendorService.getVendorByEmail(email);

            response.put("status", "success");
            response.put("vendor", vendor);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching vendor profile for {}: {}", email, e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PutMapping("/upi")
    public ResponseEntity<Map<String, Object>> updateUpiId(
            @RequestParam String email,
            @RequestParam String upiId) {

        Map<String, Object> response = new HashMap<>();

        try {
            String result = vendorService.updateUpiId(email, upiId);

            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating UPI ID for {}: {}", email, e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
