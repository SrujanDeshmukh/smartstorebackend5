package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.auth.RegisterRequest;
import com.raghunath.smartstore.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    // ================================
    // USER REGISTRATION (Keep as-is)
    // ================================
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        // AuthService.register(...) returns the created userId or throws typed exceptions
        String userId = authService.register(request);

        // Build Location header
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(userId)
                .toUri();

        Map<String, Object> body = Map.of(
                "success", true,
                "message", "User registered successfully",
                "userId", userId
        );

        return ResponseEntity.created(location).body(body); // HTTP 201
    }

    // ================================
    // USER LOOKUP (Keep as-is)
    // ================================
    @GetMapping("/user/{email}")
    public ResponseEntity<?> getUserByEmailPath(@PathVariable("email") String email) {
        // email will be URL-decoded by Spring automatically
        // Delegate to AuthService
        var user = authService.getUserByEmail(email); // throws NotFoundException if not found
        return ResponseEntity.ok(Map.of("success", true, "user", user));
    }

    // ❌ REMOVED: /login - NOW IN UnifiedAuthController (/unified-auth/login)
    // ❌ REMOVED: /logout - NOW IN UnifiedAuthController (/unified-auth/logout)
    // ❌ REMOVED: /logout-all - NOW IN UnifiedAuthController (/unified-auth/logout-all)
    // ❌ REMOVED: /refresh - NOW IN UnifiedAuthController (/unified-auth/refresh)
    // ❌ REMOVED: /status - NOW IN UnifiedAuthController (/unified-auth/status)
}
