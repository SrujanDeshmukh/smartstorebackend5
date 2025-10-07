package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.entity.User;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.entity.Employee;
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
        // Search in User table
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("User not found with email: " + email);
        }

        User user = userOpt.get();

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        // Check if user is active
        if (!user.isActive()) {
            throw new AccountInactiveException("User account is inactive");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Update refresh token in database
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        log.info("✅ User login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "USER", user, user.getId());
    }

    private AuthResponse authenticateVendor(String email, String password) {
        // Search in Vendor table
        Optional<Vendor> vendorOpt = vendorRepository.findByEmail(email);

        if (vendorOpt.isEmpty()) {
            throw new InvalidCredentialsException("Vendor not found with email: " + email);
        }

        Vendor vendor = vendorOpt.get();

        // Verify password
        if (!passwordEncoder.matches(password, vendor.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        // Check if vendor is active
        if (!vendor.isActive()) {
            throw new AccountInactiveException("Vendor account is inactive");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(vendor.getEmail(), "VENDOR");
        String refreshToken = jwtUtil.generateRefreshToken(vendor.getEmail());

        // Update refresh token in database
        vendor.setRefreshToken(refreshToken);
        vendorRepository.save(vendor);

        log.info("✅ Vendor login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "VENDOR", vendor, vendor.getId());
    }

    private AuthResponse authenticateEmployee(String email, String password) {
        // Search in Employee table
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);

        if (employeeOpt.isEmpty()) {
            throw new InvalidCredentialsException("Employee not found with email: " + email);
        }

        Employee employee = employeeOpt.get();

        // Verify password
        if (!passwordEncoder.matches(password, employee.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        // Check if employee is active
        if (!employee.isActive()) {
            throw new AccountInactiveException("Employee account is inactive");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(employee.getEmail(), "EMPLOYEE");
        String refreshToken = jwtUtil.generateRefreshToken(employee.getEmail());

        // Update refresh token in database
        employee.setRefreshToken(refreshToken);
        employeeRepository.save(employee);

        log.info("✅ Employee login successful: {}", email);
        return new AuthResponse(accessToken, refreshToken, "EMPLOYEE", employee, employee.getId());
    }

    public void logout(String email, String userType) {
        try {
            switch (userType.toUpperCase()) {
                case "VENDOR":
                    vendorRepository.findByEmail(email).ifPresent(vendor -> {
                        vendor.setRefreshToken(null);
                        vendorRepository.save(vendor);
                    });
                    break;
                case "EMPLOYEE":
                    employeeRepository.findByEmail(email).ifPresent(employee -> {
                        employee.setRefreshToken(null);
                        employeeRepository.save(employee);
                    });
                    break;
                case "USER":
                default:
                    userRepository.findByEmail(email).ifPresent(user -> {
                        user.setRefreshToken(null);
                        userRepository.save(user);
                    });
                    break;
            }
            log.info("✅ {} logout successful: {}", userType, email);
        } catch (Exception e) {
            log.error("Logout failed for {}: {}", email, e.getMessage());
        }
    }
}
