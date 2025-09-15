package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.dto.RegisterRequest;
import com.raghunath.smartstore.entity.RefreshToken;
import com.raghunath.smartstore.entity.User;
import com.raghunath.smartstore.repository.RefreshTokenRepository;
import com.raghunath.smartstore.repository.UserRepository;
import com.raghunath.smartstore.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AsyncEmailService asyncEmailService;

    @Value("${app.user.default.role:USER}")
    private String defaultUserRole;

    @Value("${app.max.login.attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.account.lockout.minutes:30}")
    private int accountLockoutMinutes;

    // ================================
    // USER REGISTRATION
    // ================================

    /**
     * Register new user with enhanced validation and welcome email
     * @param request Registration request data
     * @return Registration result message
     */
    public String register(@Valid RegisterRequest request) {
        try {
            log.info("User registration attempt for email: {}", request.getEmail());

            // Check if email already exists
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.warn("Registration failed - Email already exists: {}", request.getEmail());
                return "Email is already registered";
            }

            // Check if mobile number already exists
            if (userRepository.findByMobileNumber(request.getMobileNumber()).isPresent()) {
                log.warn("Registration failed - Mobile number already exists: {}", request.getMobileNumber());
                return "Mobile number is already registered";
            }

            // Validate password match
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return "Passwords do not match";
            }

            // Validate password strength
            String passwordValidation = validatePassword(request.getPassword());
            if (!passwordValidation.equals("VALID")) {
                return passwordValidation;
            }

            // Create new user
            User user = new User();
            user.setFullName(request.getFullName().trim());
            user.setMobileNumber(request.getMobileNumber().trim());
            user.setEmail(request.getEmail().toLowerCase().trim());
            user.setPassword(passwordEncoder.encode(request.getPassword()));


            // Save user to database
            User savedUser = userRepository.save(user);

            // Send welcome email asynchronously
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
            return "User registered successfully";

        } catch (Exception e) {
            log.error("❌ Error during user registration for email {}: {}", request.getEmail(), e.getMessage());
            return "Registration failed. Please try again.";
        }
    }

    // ================================
    // USER LOGIN
    // ================================

    /**
     * User login with enhanced security checks
     * @param email User email
     * @param password User password
     * @return Authentication response with tokens
     */
    public AuthResponse login(String email, String password) {
        try {
            log.info("Login attempt for email: {}", email);

            // Find user by email
            User user = userRepository.findByEmail(email.toLowerCase().trim())
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            // Check if account is locked
            if (isAccountLocked(user)) {
                long remainingLockTime = getRemainingLockTime(user);
                throw new RuntimeException(String.format(
                        "Account is temporarily locked due to multiple failed login attempts. Please try again in %d minutes.",
                        remainingLockTime));
            }

            // Check if account is active
            if (!user.isActive()) {
                throw new RuntimeException("Account is deactivated. Please contact support.");
            }

            // Verify password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                handleFailedLoginAttempt(user);
                throw new RuntimeException("Invalid email or password");
            }

            // Reset failed attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                user.setLastFailedLoginAt(null);
                userRepository.save(user);
            }

            // Update last login time
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            String userRole = determineUserRole(user);
            String accessToken = jwtUtil.generateAccessToken(email, userRole);
            String refreshToken = jwtUtil.generateRefreshToken(email);

            // Store refresh token
            updateRefreshToken(email, refreshToken);

            log.info("✅ User logged in successfully: {}", email);
            return new AuthResponse(accessToken, refreshToken);

        } catch (RuntimeException e) {
            log.warn("❌ Login failed for email {}: {}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error during login for email {}: {}", email, e.getMessage());
            throw new RuntimeException("Login failed. Please try again.");
        }
    }

    // ================================
    // TOKEN MANAGEMENT
    // ================================

    /**
     * Update refresh token in database with enhanced error handling
     * @param email User email
     * @param newRefreshToken New refresh token
     */
    public void updateRefreshToken(String email, String newRefreshToken) {
        try {
            log.debug("Updating refresh token for email: {}", email);

            // Delete old refresh tokens for this user
            refreshTokenRepository.deleteByEmail(email);

            // Create and save new refresh token
            RefreshToken refreshTokenEntity = new RefreshToken(email, newRefreshToken);
            // Don't set createdAt manually - @CreatedDate will handle it automatically

            refreshTokenRepository.save(refreshTokenEntity);

            log.debug("✅ Refresh token updated successfully for email: {}", email);

        } catch (Exception e) {
            log.error("❌ Failed to update refresh token for email {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to update refresh token: " + e.getMessage());
        }
    }
    /**
     * Refresh access token with enhanced validation
     * @param refreshToken Refresh token
     * @return New authentication response
     */
    public AuthResponse refreshAccessToken(String refreshToken) {
        try {
            log.debug("Refresh token request received");

            // Validate refresh token format and signature
            if (!jwtUtil.isTokenValid(refreshToken)) {
                throw new RuntimeException("Invalid or expired refresh token");
            }

            // Verify token type
            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                throw new RuntimeException("Invalid token type. Refresh token expected.");
            }

            // Extract email from token
            String email = jwtUtil.extractUsername(refreshToken);

            // Verify refresh token exists in database
            RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found in database"));

            // Verify email matches
            if (!storedToken.getEmail().equals(email)) {
                throw new RuntimeException("Token email mismatch");
            }

            // Check if user still exists and is active
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isActive()) {
                throw new RuntimeException("User account is deactivated");
            }

            // Generate new tokens
            String userRole = determineUserRole(user);
            String newAccessToken = jwtUtil.generateAccessToken(email, userRole);
            String newRefreshToken = jwtUtil.generateRefreshToken(email);

            // Update refresh token in database
            updateRefreshToken(email, newRefreshToken);

            log.info("✅ Tokens refreshed successfully for email: {}", email);
            return new AuthResponse(newAccessToken, newRefreshToken);

        } catch (RuntimeException e) {
            log.warn("❌ Token refresh failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error during token refresh: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed. Please login again.");
        }
    }

    // ================================
    // LOGOUT & SECURITY
    // ================================

    /**
     * Logout user by invalidating refresh token
     * @param email User email
     */
    public void logout(String email) {
        try {
            refreshTokenRepository.deleteByEmail(email);
            log.info("✅ User logged out successfully: {}", email);
        } catch (Exception e) {
            log.error("❌ Error during logout for email {}: {}", email, e.getMessage());
        }
    }

    /**
     * Logout user from all devices by invalidating all refresh tokens
     * @param email User email
     */
    public void logoutFromAllDevices(String email) {
        try {
            refreshTokenRepository.deleteByEmail(email);

            // Optionally, you can add a user field to track "logout from all devices" timestamp
            // This would invalidate all existing JWTs issued before this timestamp
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                user.setGlobalLogoutAt(LocalDateTime.now());
                userRepository.save(user);
            }

            log.info("✅ User logged out from all devices: {}", email);
        } catch (Exception e) {
            log.error("❌ Error during logout from all devices for email {}: {}", email, e.getMessage());
        }
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Handle failed login attempt with account lockout logic
     * @param user User entity
     */
    private void handleFailedLoginAttempt(User user) {
        try {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            user.setLastFailedLoginAt(LocalDateTime.now());

            if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
                log.warn("⚠️ Account locked due to {} failed login attempts: {}",
                        maxLoginAttempts, user.getEmail());
            }

            userRepository.save(user);

        } catch (Exception e) {
            log.error("Error handling failed login attempt for user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Check if user account is locked
     * @param user User entity
     * @return true if account is locked
     */
    private boolean isAccountLocked(User user) {
        if (user.getFailedLoginAttempts() < maxLoginAttempts) {
            return false;
        }

        if (user.getLastFailedLoginAt() == null) {
            return false;
        }

        LocalDateTime lockoutEnd = user.getLastFailedLoginAt().plusMinutes(accountLockoutMinutes);
        return LocalDateTime.now().isBefore(lockoutEnd);
    }

    /**
     * Get remaining lock time in minutes
     * @param user User entity
     * @return Remaining lock time in minutes
     */
    private long getRemainingLockTime(User user) {
        if (user.getLastFailedLoginAt() == null) {
            return 0;
        }

        LocalDateTime lockoutEnd = user.getLastFailedLoginAt().plusMinutes(accountLockoutMinutes);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(lockoutEnd)) {
            return 0;
        }

        return java.time.Duration.between(now, lockoutEnd).toMinutes() + 1;
    }

    /**
     * Determine user role (can be enhanced for role-based systems)
     * @param user User entity
     * @return User role
     */
    private String determineUserRole(User user) {
        // Can be enhanced to support multiple roles from database
        return defaultUserRole; // "USER" by default
    }

    /**
     * Validate password strength
     * @param password Password to validate
     * @return "VALID" or error message
     */
    private String validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters long";
        }

        if (password.length() > 100) {
            return "Password cannot exceed 100 characters";
        }

        // Add more password strength checks if needed
        // Example: must contain uppercase, lowercase, number, special character

        return "VALID";
    }

    // ================================
    // USER MANAGEMENT
    // ================================

    /**
     * Get user by email
     * @param email User email
     * @return User entity
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Check if user exists by email
     * @param email User email
     * @return true if user exists
     */
    public boolean userExists(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim()).isPresent();
    }

    /**
     * Update user password
     * @param email User email
     * @param newPassword New password
     * @return Success message
     */
    public String updatePassword(String email, String newPassword) {
        try {
            User user = getUserByEmail(email);

            String passwordValidation = validatePassword(newPassword);
            if (!passwordValidation.equals("VALID")) {
                return passwordValidation;
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Logout from all devices to force re-login with new password
            logoutFromAllDevices(email);

            log.info("✅ Password updated successfully for user: {}", email);
            return "Password updated successfully";

        } catch (Exception e) {
            log.error("❌ Error updating password for user {}: {}", email, e.getMessage());
            return "Failed to update password. Please try again.";
        }
    }

    /**
     * Get authentication statistics
     * @return Authentication service statistics
     */
    public AuthServiceStats getAuthStats() {
        try {
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countByIsActiveTrue();
            long lockedUsers = userRepository.countByFailedLoginAttemptsGreaterThanEqual(maxLoginAttempts);

            return new AuthServiceStats(totalUsers, activeUsers, lockedUsers, maxLoginAttempts);

        } catch (Exception e) {
            log.error("Error getting auth stats: {}", e.getMessage());
            return new AuthServiceStats(0, 0, 0, maxLoginAttempts);
        }
    }

    // ================================
    // INNER CLASS FOR STATISTICS
    // ================================

    /**
     * Authentication Service Statistics
     */
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
    }
}
