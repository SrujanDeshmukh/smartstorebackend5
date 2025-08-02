package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.dto.LoginRequest;
import com.raghunath.smartstore.dto.RegisterRequest;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponse> refresh(@RequestParam String refreshToken){
        return ResponseEntity.ok(authService.refreshAccessToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String token){
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);
        authService.logout(email);
        return ResponseEntity.ok("Logged out successfully");
    }
}
