package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.vendor.UpdateVendorProfileRequest;
import com.raghunath.smartstore.dto.vendor.VendorRegisterRequest;
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
    // VENDOR REGISTRATION (Keep as-is)
    // ================================
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerVendor(@Valid @RequestBody VendorRegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Vendor registration attempt for email: {}", request.getEmail());

            String result = vendorService.register(request);

            if (result.startsWith("Vendor registered successfully")) {
                response.put("success", true);
                response.put("message", result);
                response.put("email", request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", result);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            log.error("Error during vendor registration for {}: {}", request.getEmail(), e.getMessage());
            response.put("success", false);
            response.put("message", "Registration failed. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ❌ REMOVED: /login - NOW IN UnifiedAuthController (/unified-auth/login with type=VENDOR)
    // ❌ REMOVED: /refresh-token - NOW IN UnifiedAuthController (/unified-auth/refresh)
    // ❌ REMOVED: /logout - NOW IN UnifiedAuthController (/unified-auth/logout)

    // ================================
    // VENDOR PROFILE (Keep as-is)
    // ================================
    @GetMapping("/profile")
    public ResponseEntity<?> getVendorProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            String email = jwtUtil.extractUsername(token);
            String userType = jwtUtil.extractRole(token);

            if (!"VENDOR".equals(userType)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Only vendors can access vendor profile"));
            }

            Vendor vendor = vendorService.getVendorByEmail(email);

            // DEBUG: log the vendor object and fullName
            log.debug("DEBUG getVendorProfile - vendor object: id={}, class={}, fullName='{}'",
                    vendor == null ? "null" : vendor.getId(),
                    vendor == null ? "null" : vendor.getClass().getName(),
                    vendor == null ? null : vendor.getFullName()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "vendor", vendor
            ));

        } catch (Exception e) {
            log.error("getVendorProfile error for token: {}, err: {}", authHeader, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Vendor not found"));
        }
    }

    @PutMapping("/profile/update")
    public ResponseEntity<?> updateVendorProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateVendorProfileRequest request) {

        try {
            // Extract email from JWT token
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            String email = jwtUtil.extractUsername(token);
            String userType = jwtUtil.extractRole(token);

            // Validate vendor token
            if (!"VENDOR".equals(userType)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Only vendors can update vendor profile"));
            }

            // Update vendor profile
            Vendor updatedVendor = vendorService.updateVendorProfile(email, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vendor profile updated successfully",
                    "vendor", updatedVendor
            ));

        } catch (RuntimeException e) {
            log.error("Vendor profile update failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during vendor profile update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Profile update failed. Please try again."));
        }
    }

    // ================================
    // UPI ID UPDATE (Keep as-is)
    // ================================
    // ================================
// UPI ID UPDATE (AUTHENTICATION MANDATORY) 🔒
// ================================
    @PutMapping("/upi")
    public ResponseEntity<Map<String, Object>> updateUpiId(
            @RequestHeader(value = "Authorization", required = true) String authHeader,
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // ✅ Step 1: Validate Authorization header format
            if (authHeader == null || authHeader.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Authorization header is required");
                response.put("error", "MISSING_AUTH_HEADER");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            if (!authHeader.startsWith("Bearer ")) {
                response.put("success", false);
                response.put("message", "Invalid Authorization header format. Expected: Bearer <token>");
                response.put("error", "INVALID_AUTH_FORMAT");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // ✅ Step 2: Extract and validate JWT token
            String token = authHeader.substring(7).trim();

            if (token.isEmpty()) {
                response.put("success", false);
                response.put("message", "Access token is required");
                response.put("error", "EMPTY_TOKEN");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // ✅ Step 3: Validate token expiration and signature
            if (!jwtUtil.isTokenValid(token)) {
                response.put("success", false);
                response.put("message", "Access token is invalid or expired");
                response.put("error", "INVALID_TOKEN");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // ✅ Step 4: Extract user information from token
            String email = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid token: unable to extract user information");
                response.put("error", "INVALID_TOKEN_CLAIMS");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // ✅ Step 5: Validate user role (VENDOR only)
            if (!"VENDOR".equals(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Only vendors can update UPI ID.");
                response.put("error", "ACCESS_DENIED");
                response.put("requiredRole", "VENDOR");
                response.put("actualRole", role);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // ✅ Step 6: Validate request body
            String upiId = request.get("upiId");
            if (upiId == null || upiId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "UPI ID is required");
                response.put("error", "MISSING_UPI_ID");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // ✅ Step 7: Validate UPI ID format (optional but recommended)
            if (!isValidUpiId(upiId)) {
                response.put("success", false);
                response.put("message", "Invalid UPI ID format. Expected format: username@bankname");
                response.put("error", "INVALID_UPI_FORMAT");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // ✅ Step 8: Update UPI ID
            String result = vendorService.updateUpiId(email, upiId.trim());

            response.put("success", true);
            response.put("message", result);
            response.put("upiId", upiId.trim());
            response.put("email", email);

            log.info("✅ UPI ID updated securely for vendor: {}", email);
            return ResponseEntity.ok(response);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("❌ Expired token for UPI update: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Access token has expired. Please login again.");
            response.put("error", "TOKEN_EXPIRED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("❌ Malformed token for UPI update: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Malformed access token");
            response.put("error", "MALFORMED_TOKEN");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("❌ Invalid token signature for UPI update: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Invalid token signature");
            response.put("error", "INVALID_SIGNATURE");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            log.error("❌ Error updating UPI ID: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to update UPI ID. Please try again.");
            response.put("error", "UPI_UPDATE_FAILED");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ================================
// UPI VALIDATION HELPER
// ================================
    private boolean isValidUpiId(String upiId) {
        if (upiId == null || upiId.trim().isEmpty()) {
            return false;
        }

        // UPI format: username@bankname (e.g., vendor@paytm, 9876543210@ybl)
        String upiRegex = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$";
        return upiId.matches(upiRegex) && upiId.length() >= 5 && upiId.length() <= 100;
    }

}
