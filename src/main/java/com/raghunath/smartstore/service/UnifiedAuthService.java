package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.AuthResponse;
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

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedAuthService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository; // ✅ Added
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(String email, String password, String userType) {
        log.info("Attempting login for email: {} as type: {}", email, userType);

        try {
            switch (userType.toUpperCase()) {
                case "VENDOR":
                    return authenticateVendor(email, password);
                case "EMPLOYEE":
                    return authenticateEmployee(email, password);
                case "USER":
                default:
                    return authenticateUser(email, password);
            }
        } catch (Exception e) {
            log.error("Login failed for {}: {}", email, e.getMessage());
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    private AuthResponse authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("User not found with email: " + email);
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        if (!user.isActive()) {
            throw new AccountInactiveException("User account is inactive");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // ✅ Store refresh token in separate collection
        storeRefreshToken(user.getEmail(), refreshToken, "USER");

        log.info("✅ User login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "USER", user, user.getId());
    }

    private AuthResponse authenticateVendor(String email, String password) {
        Optional<Vendor> vendorOpt = vendorRepository.findByEmail(email);

        if (vendorOpt.isEmpty()) {
            throw new InvalidCredentialsException("Vendor not found with email: " + email);
        }

        Vendor vendor = vendorOpt.get();

        if (!passwordEncoder.matches(password, vendor.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        if (!vendor.isActive()) {
            throw new AccountInactiveException("Vendor account is inactive");
        }

        String accessToken = jwtUtil.generateAccessToken(vendor.getEmail(), "VENDOR");
        String refreshToken = jwtUtil.generateRefreshToken(vendor.getEmail());

        // ✅ Store refresh token in separate collection with VENDOR type
        storeRefreshToken(vendor.getEmail(), refreshToken, "VENDOR");

        log.info("✅ Vendor login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "VENDOR", vendor, vendor.getId());
    }

    private AuthResponse authenticateEmployee(String email, String password) {
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);

        if (employeeOpt.isEmpty()) {
            throw new InvalidCredentialsException("Employee not found with email: " + email);
        }

        Employee employee = employeeOpt.get();

        if (!passwordEncoder.matches(password, employee.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        if (!employee.isActive()) {
            throw new AccountInactiveException("Employee account is inactive");
        }

        String accessToken = jwtUtil.generateAccessToken(employee.getEmail(), "EMPLOYEE");
        String refreshToken = jwtUtil.generateRefreshToken(employee.getEmail());

        // ✅ Store refresh token in separate collection
        storeRefreshToken(employee.getEmail(), refreshToken, "EMPLOYEE");

        log.info("✅ Employee login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "EMPLOYEE", employee, employee.getId());
    }

    // ✅ NEW METHOD: Store refresh token in separate collection
    private void storeRefreshToken(String email, String refreshToken, String userType) {
        try {
            // Delete old refresh tokens for this user and type
            refreshTokenRepository.deleteByEmailAndUserType(email, userType);

            // Create new refresh token entity
            RefreshToken refreshTokenEntity = new RefreshToken(email, refreshToken, userType);
            refreshTokenRepository.save(refreshTokenEntity);

            log.debug("✅ Refresh token stored for {} with type: {}", email, userType);

        } catch (Exception e) {
            log.error("❌ Failed to store refresh token for {}: {}", email, e.getMessage());
        }
    }

    public void logout(String email, String userType) {
        try {
            // ✅ Delete from refresh_tokens collection instead of entity field
            refreshTokenRepository.deleteByEmailAndUserType(email, userType);

            log.info("✅ {} logout successful: {}", userType, email);
        } catch (Exception e) {
            log.error("Logout failed for {}: {}", email, e.getMessage());
        }
    }
}
