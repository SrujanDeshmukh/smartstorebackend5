package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.auth.AuthResponse;
import com.raghunath.smartstore.dto.auth.UserLoginProfile;
import com.raghunath.smartstore.entity.RefreshToken;
import com.raghunath.smartstore.entity.User;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.entity.Employee;
import com.raghunath.smartstore.repository.RefreshTokenRepository;
import com.raghunath.smartstore.repository.UserRepository;
import com.raghunath.smartstore.repository.VendorRepository;
import com.raghunath.smartstore.repository.EmployeeRepository;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.exception.BadRequestException;
import com.raghunath.smartstore.exception.InvalidCredentialsException;
import com.raghunath.smartstore.exception.AccountInactiveException;
import com.raghunath.smartstore.exception.UnauthorizedException;
import com.raghunath.smartstore.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
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

    // Lockout configuration
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    // ================================
    // MAIN LOGIN METHOD (Unified for ALL types)
    // ================================
    @Transactional
    public AuthResponse login(String email, String password, String userType) {
        log.info("🔐 Unified login attempt: {} ({})", email, userType);

        validateLoginInput(email, password);
        email = email.toLowerCase().trim();
        userType = normalizeUserType(userType);

        try {
            return switch (userType) {
                case "VENDOR" -> authenticateVendor(email, password);
                case "EMPLOYEE" -> authenticateEmployee(email, password);
                case "USER" -> authenticateUser(email, password);
                default -> {
                    log.warn("❌ Invalid user type provided: {}", userType);
                    throw new BadRequestException("Invalid user type: " + userType);
                }
            };
        } catch (InvalidCredentialsException | AccountInactiveException | UnauthorizedException | BadRequestException e) {
            log.warn("Authentication failed for {}: {}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected login error for {}: {}", email, e.getMessage(), e);
            throw new InvalidCredentialsException("Login failed. Please try again.");
        }
    }

    // ================================
    // USER AUTHENTICATION
    // ================================
    private AuthResponse authenticateUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.isAccountLocked()) {
            long remaining = computeRemainingLockMinutes(user.getLastFailedLoginAt(), LOCKOUT_MINUTES);
            throw new UnauthorizedException(String.format(
                    "Account temporarily locked due to multiple failed attempts. Try again in %d minutes", remaining));
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordFailedLoginForUser(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new AccountInactiveException("User account is inactive. Please contact support.");
        }

        // Successful login
        user.updateLastLogin();
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        rotateRefreshToken(user.getEmail(), refreshToken, "USER");

        log.info("✅ User login successful: {}", email);

        UserLoginProfile profile = new UserLoginProfile(user);

        return new AuthResponse(accessToken, refreshToken, "USER", profile);
    }

    // ================================
    // VENDOR AUTHENTICATION
    // ================================
    private AuthResponse authenticateVendor(String email, String password) {
        Vendor vendor = vendorRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (vendor.isAccountLocked()) {
            long remaining = computeRemainingLockMinutes(vendor.getLastFailedLoginAt(), LOCKOUT_MINUTES);
            throw new UnauthorizedException(String.format(
                    "Account temporarily locked due to multiple failed attempts. Try again in %d minutes", remaining));
        }

        if (!passwordEncoder.matches(password, vendor.getPassword())) {
            recordFailedLoginForVendor(vendor);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!vendor.isActive()) {
            throw new AccountInactiveException("Vendor account is inactive. Please contact support.");
        }

        // Check vendor approval (optional - remove if not needed)
        if (!vendor.isApproved()) {
            throw new UnauthorizedException("Vendor account is pending admin approval.");
        }

        // Successful login
        vendor.updateLastLogin();
        vendorRepository.save(vendor);

        String accessToken = jwtUtil.generateAccessToken(vendor.getEmail(), "VENDOR");
        String refreshToken = jwtUtil.generateRefreshToken(vendor.getEmail());

        rotateRefreshToken(vendor.getEmail(), refreshToken, "VENDOR");

        log.info("✅ Vendor login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "VENDOR", vendor);
    }

    // ================================
    // EMPLOYEE AUTHENTICATION
    // ================================
    private AuthResponse authenticateEmployee(String email, String password) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (employee.isAccountLocked()) {
            long remaining = computeRemainingLockMinutes(employee.getLastFailedLoginAt(), LOCKOUT_MINUTES);
            throw new UnauthorizedException(String.format(
                    "Account temporarily locked due to multiple failed attempts. Try again in %d minutes", remaining));
        }

        if (!passwordEncoder.matches(password, employee.getPassword())) {
            recordFailedLoginForEmployee(employee);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!employee.isActive()) {
            throw new AccountInactiveException("Employee account is inactive. Please contact support.");
        }

        // Successful login
        employee.updateLastLogin();
        employeeRepository.save(employee);

        String accessToken = jwtUtil.generateAccessToken(employee.getEmail(), "EMPLOYEE");
        String refreshToken = jwtUtil.generateRefreshToken(employee.getEmail());

        rotateRefreshToken(employee.getEmail(), refreshToken, "EMPLOYEE");

        log.info("✅ Employee login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "EMPLOYEE", employee);
    }

    // ================================
    // TOKEN REFRESH
    // ================================
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        validateRefreshToken(refreshToken);

        log.debug("🔄 Unified token refresh");

        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.validateRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String email = jwtUtil.extractUsername(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (!stored.getEmail().equals(email)) {
            throw new UnauthorizedException("Token email mismatch");
        }

        String userType = stored.getUserType();
        Object userData = getUserByEmailAndType(email, userType)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!isUserActiveByEmail(email, userType)) {
            throw new UnauthorizedException("User account is deactivated");
        }

        String newAccessToken = jwtUtil.generateAccessToken(email, userType);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        rotateRefreshToken(email, newRefreshToken, userType);

        log.info("✅ Tokens refreshed: {} ({})", email, userType);
        return new AuthResponse(newAccessToken, newRefreshToken, userType, userData, getUserId(userData));
    }

    // ================================
    // LOGOUT
    // ================================
    @Transactional
    public void logout(String email, String userType) {
        email = email.toLowerCase().trim();
        userType = normalizeUserType(userType);

        log.info("🔓 Unified logout: {} ({})", email, userType);

        try {
            refreshTokenRepository.deleteByEmailAndUserType(email, userType);
        } catch (Exception e) {
            List<RefreshToken> tokens = refreshTokenRepository.findByEmailAndUserType(email, userType);
            if (!tokens.isEmpty()) refreshTokenRepository.deleteAll(tokens);
        }

        log.info("✅ Logout complete: {} ({})", email, userType);
    }

    @Transactional
    public void logoutFromAllDevices(String email) {
        email = email.toLowerCase().trim();
        log.info("🔓 Unified logout-all: {}", email);

        try {
            refreshTokenRepository.deleteByEmail(email);
        } catch (Exception e) {
            List<RefreshToken> tokens = refreshTokenRepository.findByEmail(email);
            if (!tokens.isEmpty()) refreshTokenRepository.deleteAll(tokens);
        }

        log.info("✅ Logout-all complete: {}", email);
    }

    // ================================
    // FAILED LOGIN HANDLERS
    // ================================
    private void recordFailedLoginForUser(User user) {
        try {
            user.recordFailedLogin();
            userRepository.save(user);
            log.debug("Recorded failed login for user: {} (attempts={})", user.getEmail(), user.getFailedLoginAttempts());
        } catch (Exception e) {
            log.warn("Failed to record failed login for user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void recordFailedLoginForVendor(Vendor vendor) {
        try {
            vendor.recordFailedLogin();
            vendorRepository.save(vendor);
            log.debug("Recorded failed login for vendor: {} (attempts={})", vendor.getEmail(), vendor.getFailedLoginAttempts());
        } catch (Exception e) {
            log.warn("Failed to record failed login for vendor {}: {}", vendor.getEmail(), e.getMessage());
        }
    }

    private void recordFailedLoginForEmployee(Employee employee) {
        try {
            employee.recordFailedLogin();
            employeeRepository.save(employee);
            log.debug("Recorded failed login for employee: {} (attempts={})", employee.getEmail(), employee.getFailedLoginAttempts());
        } catch (Exception e) {
            log.warn("Failed to record failed login for employee {}: {}", employee.getEmail(), e.getMessage());
        }
    }

    // ================================
    // TOKEN ROTATION
    // ================================
    private void rotateRefreshToken(String email, String refreshToken, String userType) {
        try {
            try {
                refreshTokenRepository.deleteByEmailAndUserType(email, userType);
            } catch (Exception e) {
                List<RefreshToken> existing = refreshTokenRepository.findByEmailAndUserType(email, userType);
                if (!existing.isEmpty()) refreshTokenRepository.deleteAll(existing);
            }

            RefreshToken entity = new RefreshToken(email, refreshToken, userType);
            refreshTokenRepository.save(entity);

            log.debug("Stored new refresh token for {} (type={})", email, userType);
        } catch (Exception e) {
            log.error("Failed to persist refresh token for {}: {}", email, e.getMessage());
        }
    }

    // ================================
    // VALIDATION METHODS
    // ================================
    private void validateLoginInput(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null) {
            throw new BadRequestException("Email and password are required");
        }
    }

    private void validateRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new BadRequestException("Refresh token is required");
        }
    }

    private String normalizeUserType(String userType) {
        return (userType == null || userType.trim().isEmpty()) ? "USER" : userType.toUpperCase().trim();
    }

    private long computeRemainingLockMinutes(LocalDateTime lastFailedAt, int lockMinutes) {
        if (lastFailedAt == null) return 0;
        LocalDateTime lockEnd = lastFailedAt.plusMinutes(lockMinutes);
        if (LocalDateTime.now().isAfter(lockEnd)) return 0;
        return Math.max(0, Duration.between(LocalDateTime.now(), lockEnd).toMinutes());
    }

    // ================================
    // USER MANAGEMENT UTILITIES
    // ================================
    public boolean isUserActiveByEmail(String email, String userType) {
        try {
            email = email.toLowerCase().trim();
            userType = normalizeUserType(userType);

            return switch (userType) {
                case "VENDOR" -> vendorRepository.findByEmail(email).map(Vendor::isActive).orElse(false);
                case "EMPLOYEE" -> employeeRepository.findByEmail(email).map(Employee::isActive).orElse(false);
                case "USER" -> userRepository.findByEmail(email).map(User::isActive).orElse(false);
                default -> false;
            };
        } catch (Exception e) {
            log.error("Error checking user active status: {}", e.getMessage());
            return false;
        }
    }

    public Optional<Object> getUserByEmailAndType(String email, String userType) {
        try {
            email = email.toLowerCase().trim();
            userType = normalizeUserType(userType);

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

    private String getUserId(Object userData) {
        if (userData instanceof Vendor v) return v.getId();
        if (userData instanceof User u) return u.getId();
        if (userData instanceof Employee e) return e.getId();
        return null;
    }

    @Transactional
    public boolean deactivateUser(String email, String userType) {
        try {
            email = email.toLowerCase().trim();
            userType = normalizeUserType(userType);

            boolean deactivated = switch (userType) {
                case "VENDOR" -> {
                    Optional<Vendor> vendorOpt = vendorRepository.findByEmail(email);
                    if (vendorOpt.isPresent()) {
                        Vendor vendor = vendorOpt.get();
                        vendor.setIsActive(false);
                        vendorRepository.save(vendor);
                        yield true;
                    }
                    yield false;
                }
                case "EMPLOYEE" -> {
                    Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
                    if (employeeOpt.isPresent()) {
                        Employee employee = employeeOpt.get();
                        employee.setIsActive(false);
                        employeeRepository.save(employee);
                        yield true;
                    }
                    yield false;
                }
                case "USER" -> {
                    Optional<User> userOpt = userRepository.findByEmail(email);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        user.setIsActive(false);
                        userRepository.save(user);
                        yield true;
                    }
                    yield false;
                }
                default -> false;
            };

            if (deactivated) {
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
    // AUTHENTICATION STATISTICS
    // ================================
    public AuthStats getAuthStats() {
        try {
            long totalUsers = userRepository.count();
            long totalVendors = vendorRepository.count();
            long totalEmployees = employeeRepository.count();
            long activeTokens = refreshTokenRepository.count();

            long activeUsers = userRepository.findAll().stream().mapToLong(u -> u.isActive() ? 1L : 0L).sum();
            long activeVendors = vendorRepository.findAll().stream().mapToLong(v -> v.isActive() ? 1L : 0L).sum();
            long activeEmployees = employeeRepository.findAll().stream().mapToLong(e -> e.isActive() ? 1L : 0L).sum();

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

    @Transactional
    public int cleanupTokens() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);

            List<RefreshToken> tokensToDelete;
            try {
                tokensToDelete = refreshTokenRepository.findExpiredTokens(cutoff);
            } catch (Exception e) {
                List<RefreshToken> allTokens = refreshTokenRepository.findAll();
                tokensToDelete = allTokens.stream()
                        .filter(t -> {
                            try {
                                return !jwtUtil.isTokenValid(t.getToken());
                            } catch (Exception ex) {
                                return true;
                            }
                        }).toList();
            }

            if (!tokensToDelete.isEmpty()) {
                refreshTokenRepository.deleteAll(tokensToDelete);
                log.info("✅ Cleaned up {} expired/invalid refresh tokens", tokensToDelete.size());
            }
            return tokensToDelete.size();
        } catch (Exception e) {
            log.error("❌ Error during token cleanup: {}", e.getMessage(), e);
            return 0;
        }
    }

    // ================================
    // INNER CLASSES
    // ================================
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
