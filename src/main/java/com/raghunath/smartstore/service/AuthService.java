package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.auth.RegisterRequest;
import com.raghunath.smartstore.entity.User;
import com.raghunath.smartstore.exception.BadRequestException;
import com.raghunath.smartstore.exception.NotFoundException;
import com.raghunath.smartstore.exception.ResourceConflictException;
import com.raghunath.smartstore.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AsyncEmailService asyncEmailService;
    private final UnifiedAuthService unifiedAuthService;  // ✅ For calling logout after password change

    @Value("${app.user.default.role:USER}")
    private String defaultUserRole;

    @Value("${app.max.login.attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.account.lockout.minutes:30}")
    private int accountLockoutMinutes;

    // ================================
    // USER REGISTRATION (KEEP AS-IS)
    // ================================
    public String register(@Valid RegisterRequest request) {
        log.info("User registration attempt for email: {}", request.getEmail());

        String email = request.getEmail() == null ? null : request.getEmail().toLowerCase().trim();
        String mobile = request.getMobileNumber() == null ? null : request.getMobileNumber().trim();
        String location = request.getLocation() == null ? null : request.getLocation().trim();

        // Basic validations
        if (email == null || email.isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (mobile == null || mobile.isEmpty()) {
            throw new BadRequestException("Mobile number is required");
        }
        if(location == null || location.isEmpty()){
            throw new BadRequestException("Location is required");
        }
        if (request.getPassword() == null || request.getConfirmPassword() == null ||
                !request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        String passwordValidation = validatePassword(request.getPassword());
        if (!"VALID".equals(passwordValidation)) {
            throw new BadRequestException(passwordValidation);
        }

        // Check uniqueness (DB unique index + duplicate key handling is recommended)
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("Registration failed - Email already exists: {}", email);
            throw new ResourceConflictException("Email is already registered");
        }
        if (userRepository.findByMobileNumber(mobile).isPresent()) {
            log.warn("Registration failed - Mobile number already exists: {}", mobile);
            throw new ResourceConflictException("Mobile number is already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName() == null ? null : request.getFullName().trim());
        user.setMobileNumber(mobile);
        user.setEmail(email);
        user.setLocation(location);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        try {
            User savedUser = userRepository.save(user);

            // Fire-and-forget welcome email
            CompletableFuture<Boolean> emailResult = asyncEmailService.sendUserWelcomeEmailAsync(
                    savedUser.getEmail(), savedUser.getFullName());
            emailResult.whenComplete((success, throwable) -> {
                if (success) {
                    log.info("✅ Welcome email sent to new user: {}", savedUser.getEmail());
                } else {
                    log.warn("⚠️ Failed to send welcome email to: {}", savedUser.getEmail());
                }
            });

            log.info("✅ User registered successfully: {}", savedUser.getEmail());
            return savedUser.getId();
        } catch (DuplicateKeyException dk) {
            // In case unique index race occurs
            log.warn("Duplicate key on register for email/mobile: {}", dk.getMessage());
            throw new ResourceConflictException("Email or mobile already registered");
        } catch (Exception e) {
            log.error("❌ Error during user registration for email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Registration failed. Please try again.");
        }
    }

    // ❌ REMOVED: login() - NOW IN UnifiedAuthService
    // ❌ REMOVED: updateRefreshToken() - NOW IN UnifiedAuthService
    // ❌ REMOVED: refreshAccessToken() - NOW IN UnifiedAuthService
    // ❌ REMOVED: logout() - NOW IN UnifiedAuthService
    // ❌ REMOVED: logoutFromAllDevices() - NOW IN UnifiedAuthService
    // ❌ REMOVED: handleFailedLoginAttempt() - NOW IN UnifiedAuthService
    // ❌ REMOVED: isAccountLocked() - NOW IN UnifiedAuthService

    // ================================
    // UTILITY METHODS (KEEP AS-IS)
    // ================================
    private long getRemainingLockTime(User user) {
        if (user.getLastFailedLoginAt() == null) return 0;
        LocalDateTime lockoutEnd = user.getLastFailedLoginAt().plusMinutes(accountLockoutMinutes);
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(lockoutEnd)) return 0;
        return java.time.Duration.between(now, lockoutEnd).toMinutes() + 1;
    }

    private String determineUserRole(User user) {
        return defaultUserRole;
    }

    private String validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters long";
        }
        if (password.length() > 100) {
            return "Password cannot exceed 100 characters";
        }
        return "VALID";
    }

    // ================================
    // USER MANAGEMENT (KEEP AS-IS)
    // ================================
    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public boolean userExists(String email) {
        if (email == null) return false;
        return userRepository.findByEmail(email.toLowerCase().trim()).isPresent();
    }

    public void updatePassword(String email, String newPassword) {
        if (email == null || newPassword == null) {
            throw new BadRequestException("Email and new password required");
        }

        User user = getUserByEmail(email);
        String passwordValidation = validatePassword(newPassword);
        if (!"VALID".equals(passwordValidation)) {
            throw new BadRequestException(passwordValidation);
        }

        try {
            user.updatePassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // ✅ Call UnifiedAuthService for logout (no duplication)
            unifiedAuthService.logoutFromAllDevices(email);

            log.info("✅ Password updated successfully for user: {}", email);
        } catch (Exception e) {
            log.error("❌ Error updating password for user {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to update password. Please try again.");
        }
    }

    public AuthServiceStats getAuthStats() {
        try {
            long totalUsers = userRepository.count();

            long activeUsers;
            try {
                activeUsers = userRepository.countByIsActiveTrue();
            } catch (Exception e) {
                activeUsers = userRepository.findAll().stream()
                        .mapToLong(u -> u.isActive() ? 1L : 0L)
                        .sum();
            }

            long lockedUsers;
            try {
                lockedUsers = userRepository.countByFailedLoginAttemptsGreaterThanEqual(maxLoginAttempts);
            } catch (Exception e) {
                lockedUsers = userRepository.findAll().stream()
                        .mapToLong(u -> u.getFailedLoginAttempts() >= maxLoginAttempts ? 1L : 0L)
                        .sum();
            }

            return new AuthServiceStats(totalUsers, activeUsers, lockedUsers, maxLoginAttempts);
        } catch (Exception e) {
            log.error("Error getting auth stats: {}", e.getMessage(), e);
            return new AuthServiceStats(0, 0, 0, maxLoginAttempts);
        }
    }

    // ================================
    // INNER CLASSES (KEEP AS-IS)
    // ================================
    public static class AuthServiceStats {
        private final long totalUsers;
        private final long activeUsers;
        private final long lockedUsers;
        private final int maxLoginAttempts;

        public AuthServiceStats(long totalUsers, long activeUsers, long lockedUsers, int maxLoginAttempts) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.lockedUsers = lockedUsers;
            this.maxLoginAttempts = maxLoginAttempts;
        }

        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getLockedUsers() { return lockedUsers; }
        public int getMaxLoginAttempts() { return maxLoginAttempts; }

        @Override
        public String toString() {
            return String.format("AuthStats{total=%d, active=%d, locked=%d, maxAttempts=%d}",
                    totalUsers, activeUsers, lockedUsers, maxLoginAttempts);
        }
    }
}
