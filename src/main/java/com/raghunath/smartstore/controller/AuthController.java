package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.dto.LoginRequest;
import com.raghunath.smartstore.dto.RegisterRequest;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    // Register
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.ok(authService.register(request));
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.getEmail(), request.getPassword()));
    }

    // Refresh Access Token
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        try {
            // Validate refresh token
            if (!jwtUtil.isTokenValid(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired refresh token"));
            }

            // Extract user email from refresh token
            String email = jwtUtil.extractUsername(refreshToken);

            // Verify it's actually a refresh token (not access token)
            String tokenType = jwtUtil.extractClaim(refreshToken,
                    claims -> claims.get("type", String.class));

            if (!"REFRESH".equals(tokenType)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid token type"));
            }

            // Generate new tokens
            String newAccessToken = jwtUtil.generateAccessToken(email, "USER");
            String newRefreshToken = jwtUtil.generateRefreshToken(email);

            // Update refresh token in database
            authService.updateRefreshToken(email, newRefreshToken);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token refresh failed: " + e.getMessage()));
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token){
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);
        authService.logout(email);
        return ResponseEntity.ok("Logged out successfully");
    }
}
