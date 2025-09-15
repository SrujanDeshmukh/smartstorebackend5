package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.dto.LoginRequest;
import com.raghunath.smartstore.dto.VendorRegisterRequest;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vendor/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VendorAuthController {

    private final VendorService vendorService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody VendorRegisterRequest request) {
        return ResponseEntity.ok(vendorService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(vendorService.login(request.getEmail(), request.getPassword()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestParam String refreshToken) {
        try {
            // Validate refresh token
            if (!jwtUtil.isTokenValid(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid or expired refresh token");
            }

            // Extract username from refresh token
            String email = jwtUtil.extractUsername(refreshToken);

            // Check if it's a refresh token (not access token)
            String tokenType = jwtUtil.extractClaim(refreshToken,
                    claims -> claims.get("type", String.class));

            if (!"REFRESH".equals(tokenType)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid token type. Refresh token required");
            }

            // Generate new access token
            String role = "USER"; // You might want to store role in refresh token too
            String newAccessToken = jwtUtil.generateAccessToken(email, role);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to refresh token: " + e.getMessage());
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token) {
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);
        vendorService.logout(email);
        return ResponseEntity.ok("Logged out successfully");
    }

    @PutMapping("/upi")
    public ResponseEntity<String> updateUpiId(
            @RequestHeader("Authorization") String token,
            @RequestParam String upiId) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);
        return ResponseEntity.ok(vendorService.updateUpiId(email, upiId));
    }
}
