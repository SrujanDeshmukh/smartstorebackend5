package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.auth.AuthResponse;
import com.raghunath.smartstore.entity.RefreshToken;
import com.raghunath.smartstore.entity.User;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.entity.Employee;
import com.raghunath.smartstore.repository.RefreshTokenRepository;
import com.raghunath.smartstore.repository.UserRepository;
import com.raghunath.smartstore.repository.VendorRepository;
import com.raghunath.smartstore.repository.EmployeeRepository;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.exception.InvalidCredentialsException;
import com.raghunath.smartstore.exception.AccountInactiveException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedAuthService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ================================
    // MAIN LOGIN METHOD (Enhanced)
    // ================================

    @Transactional
    public AuthResponse login(String email, String password, String userType) {
        log.info("Attempting login for email: {} as type: {}", email, userType);

        // Normalize inputs
        email = email.toLowerCase().trim();
        userType = userType.toUpperCase().trim();

        try {
            return switch (userType) {
                case "VENDOR" -> authenticateVendor(email, password);
                case "EMPLOYEE" -> authenticateEmployee(email, password);
                case "USER" -> authenticateUser(email, password);
                default -> {
                    log.warn("❌ Invalid user type provided: {}", userType);
                    throw new InvalidCredentialsException("Invalid user type: " + userType);
                }
            };
        } catch (InvalidCredentialsException | AccountInactiveException e) {
            log.error("Authentication failed for {}: {}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected login error for {}: {}", email, e.getMessage());
            throw new InvalidCredentialsException("Login failed. Please try again.");
        }
    }

    // ================================
    // USER AUTHENTICATION (Enhanced)
    // ================================

    private AuthResponse authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.debug("User not found with email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.debug("Invalid password for user: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            log.debug("Inactive user attempted login: {}", email);
            throw new AccountInactiveException("User account is inactive. Please contact support.");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Store refresh token
        storeRefreshToken(user.getEmail(), refreshToken, "USER");

        log.info("✅ User login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "USER", user, user.getId());
    }

    // ================================
    // VENDOR AUTHENTICATION (Enhanced)
    // ================================

    private AuthResponse authenticateVendor(String email, String password) {
        Optional<Vendor> vendorOpt = vendorRepository.findByEmail(email);

        if (vendorOpt.isEmpty()) {
            log.debug("Vendor not found with email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        Vendor vendor = vendorOpt.get();

        if (!passwordEncoder.matches(password, vendor.getPassword())) {
            log.debug("Invalid password for vendor: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!vendor.isActive()) {
            log.debug("Inactive vendor attempted login: {}", email);
            throw new AccountInactiveException("Vendor account is inactive. Please contact support.");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(vendor.getEmail(), "VENDOR");
        String refreshToken = jwtUtil.generateRefreshToken(vendor.getEmail());

        // Store refresh token
        storeRefreshToken(vendor.getEmail(), refreshToken, "VENDOR");

        log.info("✅ Vendor login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "VENDOR", vendor, vendor.getId());
    }

    // ================================
    // EMPLOYEE AUTHENTICATION (Enhanced)
    // ================================

    private AuthResponse authenticateEmployee(String email, String password) {
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);

        if (employeeOpt.isEmpty()) {
            log.debug("Employee not found with email: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        Employee employee = employeeOpt.get();

        if (!passwordEncoder.matches(password, employee.getPassword())) {
            log.debug("Invalid password for employee: {}", email);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!employee.isActive()) {
            log.debug("Inactive employee attempted login: {}", email);
            throw new AccountInactiveException("Employee account is inactive. Please contact support.");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(employee.getEmail(), "EMPLOYEE");
        String refreshToken = jwtUtil.generateRefreshToken(employee.getEmail());

        // Store refresh token
        storeRefreshToken(employee.getEmail(), refreshToken, "EMPLOYEE");

        log.info("✅ Employee login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "EMPLOYEE", employee, employee.getId());
    }

    // ================================
    // TOKEN MANAGEMENT (Enhanced)
    // ================================

    /**
     * Store refresh token in separate collection with enhanced error handling
     */
    private void storeRefreshToken(String email, String refreshToken, String userType) {
        try {
            email = email.toLowerCase().trim();
            log.debug("Storing refresh token for user: {} with type: {}", email, userType);

            // Clean up old tokens using compatible approach
            List<RefreshToken> existingTokens = refreshTokenRepository.findByEmailAndUserType(email, userType);

            if (!existingTokens.isEmpty()) {
                refreshTokenRepository.deleteAll(existingTokens);
                log.debug("Deleted {} existing refresh tokens for user: {}", existingTokens.size(), email);
            }

            // Create and save new refresh token
            RefreshToken refreshTokenEntity = new RefreshToken(email, refreshToken, userType);
            refreshTokenRepository.save(refreshTokenEntity);

            log.debug("✅ Refresh token stored successfully for {} with type: {}", email, userType);

        } catch (Exception e) {
            log.error("❌ Failed to store refresh token for {}: {}", email, e.getMessage());
            // Don't throw exception here - login should succeed even if token storage fails
        }
    }

    // ================================
    // LOGOUT FUNCTIONALITY (Enhanced)
    // ================================

    /**
     * Logout user from current device by invalidating refresh tokens
     */
    @Transactional
    public void logout(String email, String userType) {
        try {
            email = email.toLowerCase().trim();
            log.info("Processing logout for user: {} with type: {}", email, userType);

            // Find and delete refresh tokens using compatible approach
            List<RefreshToken> tokensToDelete = refreshTokenRepository.findByEmailAndUserType(email, userType);

            if (!tokensToDelete.isEmpty()) {
                refreshTokenRepository.deleteAll(tokensToDelete);
                log.info("✅ {} logout successful: {} (deleted {} tokens)", userType, email, tokensToDelete.size());
            } else {
                log.warn("⚠️ No refresh tokens found to delete for user: {} ({})", email, userType);
            }

        } catch (Exception e) {
            log.error("❌ Logout failed for {}: {}", email, e.getMessage());
            // Don't throw exception - logout should be graceful
        }
    }

    /**
     * Logout user from all devices (all user types)
     */
    @Transactional
    public void logoutFromAllDevices(String email) {
        try {
            email = email.toLowerCase().trim();
            log.info("Processing logout-all for user: {}", email);

            // Find and delete all refresh tokens for this email
            List<RefreshToken> tokensToDelete = refreshTokenRepository.findByEmail(email);

            if (!tokensToDelete.isEmpty()) {
                refreshTokenRepository.deleteAll(tokensToDelete);
                log.info("✅ User logged out from all devices: {} (deleted {} tokens)", email, tokensToDelete.size());
            } else {
                log.warn("⚠️ No refresh tokens found for user: {}", email);
            }

        } catch (Exception e) {
            log.error("❌ Logout-all failed for {}: {}", email, e.getMessage());
            // Don't throw exception - logout should be graceful
        }
    }

    // ================================
    // USER MANAGEMENT UTILITIES (New)
    // ================================

    /**
     * Check if user exists and is active by email and type
     */
    public boolean isUserActiveByEmail(String email, String userType) {
        try {
            email = email.toLowerCase().trim();
            userType = userType.toUpperCase().trim();

            return switch (userType) {
                case "VENDOR" -> vendorRepository.findByEmail(email)
                        .map(Vendor::isActive)
                        .orElse(false);
                case "EMPLOYEE" -> employeeRepository.findByEmail(email)
                        .map(Employee::isActive)
                        .orElse(false);
                case "USER" -> userRepository.findByEmail(email)
                        .map(User::isActive)
                        .orElse(false);
                default -> false;
            };
        } catch (Exception e) {
            log.error("Error checking user active status: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get user information by email and type (for admin purposes)
     */
    public Optional<Object> getUserByEmailAndType(String email, String userType) {
        try {
            email = email.toLowerCase().trim();
            userType = userType.toUpperCase().trim();

            return switch (userType) {
                case "VENDOR" -> vendorRepository.findByEmail(email).map(v -> (Object) v);
                case "EMPLOYEE" -> employeeRepository.findByEmail(email).map(e -> (Object) e);
                case "USER" -> userRepository.findByEmail(email).map(u -> (Object) u);
                default -> Optional.empty();
            };
        } catch (Exception e) {
            log.error("Error fetching user information: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Deactivate user account (soft delete)
     */
    @Transactional
    public boolean deactivateUser(String email, String userType) {
        try {
            email = email.toLowerCase().trim();
            userType = userType.toUpperCase().trim();

            boolean deactivated = switch (userType) {
                case "VENDOR" -> {
                    Optional<Vendor> vendorOpt = vendorRepository.findByEmail(email);
                    if (vendorOpt.isPresent()) {
                        Vendor vendor = vendorOpt.get();
                        vendor.setActive(false);
                        vendorRepository.save(vendor);
                        yield true;
                    }
                    yield false;
                }
                case "EMPLOYEE" -> {
                    Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
                    if (employeeOpt.isPresent()) {
                        Employee employee = employeeOpt.get();
                        employee.setActive(false);
                        employeeRepository.save(employee);
                        yield true;
                    }
                    yield false;
                }
                case "USER" -> {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        user.setActive(false);
                        userRepository.save(user);
                        yield true;
                    }
                    yield false;
                }
                default -> false;
            };

            if (deactivated) {
                // Also logout from all devices
                logoutFromAllDevices(email);
                log.info("✅ User deactivated and logged out: {} ({})", email, userType);
            }

            return deactivated;
        } catch (Exception e) {
            log.error("❌ Failed to deactivate user {}: {}", email, e.getMessage());
            return false;
        }
    }

    // ================================
    // AUTHENTICATION STATISTICS (New)
    // ================================

    /**
     * Get authentication statistics
     */
    public AuthStats getAuthStats() {
        try {
            long totalUsers = userRepository.count();
            long totalVendors = vendorRepository.count();
            long totalEmployees = employeeRepository.count();
            long activeTokens = refreshTokenRepository.count();

            // Count active users
            long activeUsers = userRepository.findAll().stream()
                    .mapToLong(user -> user.isActive() ? 1L : 0L)
                    .sum();

            long activeVendors = vendorRepository.findAll().stream()
                    .mapToLong(vendor -> vendor.isActive() ? 1L : 0L)
                    .sum();

            long activeEmployees = employeeRepository.findAll().stream()
                    .mapToLong(employee -> employee.isActive() ? 1L : 0L)
                    .sum();

            return new AuthStats(
                    totalUsers, totalVendors, totalEmployees,
                    activeUsers, activeVendors, activeEmployees,
                    activeTokens
            );
        } catch (Exception e) {
            log.error("Error getting auth stats: {}", e.getMessage());
            return new AuthStats(0, 0, 0, 0, 0, 0, 0);
        }
    }

    /**
     * Clean up expired or orphaned refresh tokens
     */
    @Transactional
    public int cleanupTokens() {
        try {
            // Find all refresh tokens
            List<RefreshToken> allTokens = refreshTokenRepository.findAll();
            List<RefreshToken> tokensToDelete = allTokens.stream()
                    .filter(token -> {
                        try {
                            // Check if token is expired or invalid
                            return !jwtUtil.isTokenValid(token.getToken());
                        } catch (Exception e) {
                            // If we can't validate, consider it invalid
                            return true;
                        }
                    })
                    .toList();

            if (!tokensToDelete.isEmpty()) {
                refreshTokenRepository.deleteAll(tokensToDelete);
                log.info("✅ Cleaned up {} expired/invalid refresh tokens", tokensToDelete.size());
            }

            return tokensToDelete.size();
        } catch (Exception e) {
            log.error("❌ Error during token cleanup: {}", e.getMessage());
            return 0;
        }
    }

    // ================================
    // INNER CLASSES
    // ================================

    /**
     * Enhanced Authentication Statistics
     */
    public static class AuthStats {
        private final long totalUsers;
        private final long totalVendors;
        private final long totalEmployees;
        private final long activeUsers;
        private final long activeVendors;
        private final long activeEmployees;
        private final long activeTokens;

        public AuthStats(long totalUsers, long totalVendors, long totalEmployees,
                         long activeUsers, long activeVendors, long activeEmployees,
                         long activeTokens) {
            this.totalUsers = totalUsers;
            this.totalVendors = totalVendors;
            this.totalEmployees = totalEmployees;
            this.activeUsers = activeUsers;
            this.activeVendors = activeVendors;
            this.activeEmployees = activeEmployees;
            this.activeTokens = activeTokens;
        }

        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getTotalVendors() { return totalVendors; }
        public long getTotalEmployees() { return totalEmployees; }
        public long getActiveUsers() { return activeUsers; }
        public long getActiveVendors() { return activeVendors; }
        public long getActiveEmployees() { return activeEmployees; }
        public long getActiveTokens() { return activeTokens; }

        public long getTotalAccounts() { return totalUsers + totalVendors + totalEmployees; }
        public long getActiveAccounts() { return activeUsers + activeVendors + activeEmployees; }

        @Override
        public String toString() {
            return String.format(
                    "AuthStats{total=%d, active=%d, tokens=%d, users=%d/%d, vendors=%d/%d, employees=%d/%d}",
                    getTotalAccounts(), getActiveAccounts(), activeTokens,
                    activeUsers, totalUsers, activeVendors, totalVendors, activeEmployees, totalEmployees
            );
        }
    }
}
