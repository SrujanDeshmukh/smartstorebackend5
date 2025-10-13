package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.auth.AuthResponse;
import com.raghunath.smartstore.dto.vendor.VendorRegisterRequest;
import com.raghunath.smartstore.dto.vendor.UpdateVendorProfileRequest;
import com.raghunath.smartstore.entity.RefreshToken;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.RefreshTokenRepository;
import com.raghunath.smartstore.repository.VendorRepository;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.exception.InvalidCredentialsException;
import com.raghunath.smartstore.exception.AccountInactiveException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.vendor.auto-approve:false}")
    private boolean autoApproveVendors;

    @Value("${app.vendor.max-login-attempts:5}")
    private int maxLoginAttempts;

    // ================================
    // VENDOR REGISTRATION (ENHANCED)
    // ================================

    @Transactional
    public String register(@Valid VendorRegisterRequest request) {
        try {
            log.info("Vendor registration attempt for email: {}", request.getEmail());

            // Check if email already exists
            if (vendorRepository.findByEmail(request.getEmail().toLowerCase().trim()).isPresent()) {
                log.warn("Registration failed - Email already exists: {}", request.getEmail());
                return "Email is already registered";
            }

            // Check if mobile already exists
            if (vendorRepository.findByMobile(request.getMobile()).isPresent()) {
                log.warn("Registration failed - Mobile already exists: {}", request.getMobile());
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

            // Create vendor entity
            Vendor vendor = Vendor.builder()
                    .fullName(request.getFullName().trim())
                    .email(request.getEmail().toLowerCase().trim())
                    .mobile(request.getMobile().trim())
                    .mobileOptional(request.getMobileOptional() != null ? request.getMobileOptional().trim() : null)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .isActive(true)
                    .isVerified(false)
                    .isApproved(autoApproveVendors)
                    .emailVerified(false)
                    .mobileVerified(false)
                    .totalOrders(0L)
                    .totalRevenue(0.0)
                    .rating(0.0)
                    .failedLoginAttempts(0)
                    .build();

            Vendor savedVendor = vendorRepository.save(vendor);

            log.info("✅ Vendor registered successfully: {} (ID: {})", savedVendor.getEmail(), savedVendor.getId());
            return "Vendor registered successfully. " +
                    (autoApproveVendors ? "Account is active." : "Waiting for admin approval.");

        } catch (Exception e) {
            log.error("❌ Error during vendor registration for email {}: {}", request.getEmail(), e.getMessage());
            return "Registration failed. Please try again.";
        }
    }

    // ================================
    // VENDOR LOGIN (ENHANCED)
    // ================================

    @Transactional
    public AuthResponse login(String email, String password) {
        try {
            log.info("Vendor login attempt for email: {}", email);

            Vendor vendor = vendorRepository.findByEmail(email.toLowerCase().trim())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

            // Check if account is locked
            if (isAccountLocked(vendor)) {
                throw new RuntimeException("Account is temporarily locked due to multiple failed login attempts. Please try again later.");
            }

            // Check if account is active
            if (!vendor.isActive()) {
                throw new AccountInactiveException("Account is deactivated. Please contact support.");
            }

            // Check if account is approved
            if (!vendor.isApproved()) {
                throw new RuntimeException("Account is pending admin approval. Please wait for approval.");
            }

            // Validate password
            if (!passwordEncoder.matches(password, vendor.getPassword())) {
                handleFailedLoginAttempt(vendor);
                throw new InvalidCredentialsException("Invalid email or password");
            }

            // Reset failed login attempts on successful login
            if (vendor.getFailedLoginAttempts() > 0) {
                vendor.resetFailedLoginAttempts();
            }

            // Update last login
            vendor.updateLastLogin();
            vendorRepository.save(vendor);

            // Generate tokens
            String accessToken = jwtUtil.generateAccessToken(email, "VENDOR");
            String refreshToken = jwtUtil.generateRefreshToken(email);

            // Store refresh token
            updateRefreshToken(email, refreshToken);

            log.info("✅ Vendor logged in successfully: {}", email);
            return new AuthResponse(accessToken, refreshToken, "VENDOR", vendor, vendor.getId());

        } catch (RuntimeException e) {
            // ✅ SIMPLIFIED: Catch all RuntimeException types
            log.warn("❌ Vendor login failed for email {}: {}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected vendor login error for email {}: {}", email, e.getMessage());
            throw new RuntimeException("Login failed. Please try again.");
        }
    }


    // ================================
    // VENDOR PROFILE UPDATE (NEW)
    // ================================

    @Transactional
    public Vendor updateVendorProfile(String email, UpdateVendorProfileRequest request) {
        try {
            log.info("Updating vendor profile for email: {}", email);

            Vendor vendor = vendorRepository.findByEmail(email.toLowerCase().trim())
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));

            // Check if vendor is active
            if (!vendor.isActive()) {
                throw new RuntimeException("Cannot update profile of inactive vendor");
            }

            // Update fields if provided (null-safe updates)
            boolean hasChanges = false;

            if (isValidField(request.getFullName())) {
                vendor.setFullName(request.getFullName().trim());
                hasChanges = true;
            }

            if (isValidField(request.getMobileOptional())) {
                vendor.setMobileOptional(request.getMobileOptional().trim());
                hasChanges = true;
            }

            if (isValidField(request.getUpiId())) {
                vendor.setUpiId(request.getUpiId().trim());
                hasChanges = true;
            }

            if (isValidField(request.getBusinessDescription())) {
                vendor.setBusinessDescription(request.getBusinessDescription().trim());
                hasChanges = true;
            }

            if (isValidField(request.getBusinessAddress())) {
                vendor.setBusinessAddress(request.getBusinessAddress().trim());
                hasChanges = true;
            }

            if (isValidField(request.getPinCode())) {
                vendor.setPinCode(request.getPinCode().trim());
                hasChanges = true;
            }

            if (isValidField(request.getGstNumber())) {
                vendor.setGstNumber(request.getGstNumber().trim());
                hasChanges = true;
            }

            if (!hasChanges) {
                throw new RuntimeException("No valid fields provided for update");
            }

            // Update timestamp
            vendor.setUpdatedAt(LocalDateTime.now());

            Vendor savedVendor = vendorRepository.save(vendor);

            log.info("✅ Vendor profile updated successfully for email: {}", email);
            return savedVendor;

        } catch (Exception e) {
            log.error("❌ Failed to update vendor profile for email {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to update vendor profile: " + e.getMessage());
        }
    }

    // ================================
    // TOKEN MANAGEMENT (ENHANCED)
    // ================================

    /**
     * Store refresh token in database with improved error handling
     */
    private void updateRefreshToken(String email, String refreshToken) {
        try {
            email = email.toLowerCase().trim();

            // Delete old refresh tokens for this vendor using compatible approach
            List<RefreshToken> existingTokens = refreshTokenRepository.findByEmailAndUserType(email, "VENDOR");
            if (!existingTokens.isEmpty()) {
                refreshTokenRepository.deleteAll(existingTokens);
                log.debug("Deleted {} old refresh tokens for vendor: {}", existingTokens.size(), email);
            }

            // Save new refresh token with VENDOR type
            RefreshToken refreshTokenEntity = new RefreshToken(email, refreshToken, "VENDOR");
            refreshTokenRepository.save(refreshTokenEntity);

            log.debug("✅ Refresh token saved for vendor: {}", email);

        } catch (Exception e) {
            log.error("❌ Failed to save refresh token for vendor {}: {}", email, e.getMessage());
            // Don't throw exception - login should succeed even if token storage fails
        }
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        try {
            log.debug("Vendor token refresh request");

            if (!jwtUtil.isTokenValid(refreshToken)) {
                throw new RuntimeException("Invalid or expired refresh token");
            }

            if (!jwtUtil.validateRefreshToken(refreshToken)) {
                throw new RuntimeException("Invalid token type. Refresh token expected.");
            }

            String email = jwtUtil.extractUsername(refreshToken);

            RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            if (!stored.getEmail().equals(email) || !"VENDOR".equals(stored.getUserType())) {
                throw new RuntimeException("Invalid refresh token");
            }

            // Verify vendor still exists and is active
            Vendor vendor = vendorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));

            if (!vendor.isActive()) {
                throw new RuntimeException("Vendor account is deactivated");
            }

            // Generate new tokens
            String newAccessToken = jwtUtil.generateAccessToken(email, "VENDOR");
            String newRefreshToken = jwtUtil.generateRefreshToken(email);

            // Update refresh token
            updateRefreshToken(email, newRefreshToken);

            log.info("✅ Vendor tokens refreshed successfully: {}", email);
            return new AuthResponse(newAccessToken, newRefreshToken, "VENDOR", vendor, vendor.getId());

        } catch (RuntimeException e) {
            log.warn("❌ Vendor token refresh failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Unexpected error during vendor token refresh: {}", e.getMessage());
            throw new RuntimeException("Token refresh failed. Please login again.");
        }
    }

    // ================================
    // LOGOUT (ENHANCED)
    // ================================

    @Transactional
    public void logout(String email) {
        try {
            email = email.toLowerCase().trim();

            // Delete refresh tokens using compatible approach
            List<RefreshToken> tokensToDelete = refreshTokenRepository.findByEmailAndUserType(email, "VENDOR");
            if (!tokensToDelete.isEmpty()) {
                refreshTokenRepository.deleteAll(tokensToDelete);
                log.info("✅ Vendor logged out successfully: {} (deleted {} tokens)", email, tokensToDelete.size());
            } else {
                log.info("✅ Vendor logged out: {} (no tokens found)", email);
            }
        } catch (Exception e) {
            log.error("❌ Error during vendor logout for email {}: {}", email, e.getMessage());
        }
    }

    // ================================
    // VENDOR MANAGEMENT (ENHANCED)
    // ================================

    public Vendor getVendorByEmail(String email) {
        return vendorRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
    }

    public boolean vendorExists(String email) {
        return vendorRepository.findByEmail(email.toLowerCase().trim()).isPresent();
    }

    @Transactional
    public String updateUpiId(String email, String upiId) {
        try {
            Vendor vendor = getVendorByEmail(email);

            if (!vendor.isActive()) {
                throw new RuntimeException("Cannot update UPI ID of inactive vendor");
            }

            vendor.setUpiId(upiId != null ? upiId.trim() : null);
            vendor.setUpdatedAt(LocalDateTime.now());
            vendorRepository.save(vendor);

            log.info("✅ UPI ID updated for vendor: {}", email);
            return "UPI ID updated successfully";

        } catch (Exception e) {
            log.error("❌ Error updating UPI ID for vendor {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to update UPI ID: " + e.getMessage());
        }
    }

    @Transactional
    public String verifyVendor(String email) {
        try {
            Vendor vendor = getVendorByEmail(email);
            vendor.markAsVerified();
            vendorRepository.save(vendor);

            log.info("✅ Vendor verified: {}", email);
            return "Vendor verified successfully";

        } catch (Exception e) {
            log.error("❌ Error verifying vendor {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to verify vendor: " + e.getMessage());
        }
    }

    @Transactional
    public String approveVendor(String email) {
        try {
            Vendor vendor = getVendorByEmail(email);
            vendor.markAsApproved();
            vendorRepository.save(vendor);

            log.info("✅ Vendor approved: {}", email);
            return "Vendor approved successfully";

        } catch (Exception e) {
            log.error("❌ Error approving vendor {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to approve vendor: " + e.getMessage());
        }
    }

    // ================================
    // UTILITY METHODS (NEW)
    // ================================

    private boolean isValidField(String field) {
        return field != null && !field.trim().isEmpty();
    }

    private void handleFailedLoginAttempt(Vendor vendor) {
        try {
            vendor.recordFailedLogin();
            vendorRepository.save(vendor);

            if (vendor.getFailedLoginAttempts() >= maxLoginAttempts) {
                log.warn("⚠️ Vendor account locked due to {} failed login attempts: {}",
                        maxLoginAttempts, vendor.getEmail());
            }
        } catch (Exception e) {
            log.error("Error handling failed login attempt for vendor {}: {}", vendor.getEmail(), e.getMessage());
        }
    }

    private boolean isAccountLocked(Vendor vendor) {
        try {
            return vendor.isAccountLocked();
        } catch (Exception e) {
            // Fallback logic if method doesn't exist
            return vendor.getFailedLoginAttempts() >= maxLoginAttempts;
        }
    }

    private String validatePassword(String password) {
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters long";
        }
        if (password.length() > 128) {
            return "Password cannot exceed 128 characters";
        }
        return "VALID";
    }

    // ================================
    // STATISTICS & REPORTING (NEW)
    // ================================

    public VendorStats getVendorStats() {
        try {
            long totalVendors = vendorRepository.count();
            long activeVendors = vendorRepository.findAll().stream()
                    .mapToLong(v -> v.isActive() ? 1L : 0L)
                    .sum();
            long verifiedVendors = vendorRepository.findAll().stream()
                    .mapToLong(v -> v.isVerified() ? 1L : 0L)
                    .sum();
            long approvedVendors = vendorRepository.findAll().stream()
                    .mapToLong(v -> v.isApproved() ? 1L : 0L)
                    .sum();

            return new VendorStats(totalVendors, activeVendors, verifiedVendors, approvedVendors);
        } catch (Exception e) {
            log.error("Error getting vendor stats: {}", e.getMessage());
            return new VendorStats(0, 0, 0, 0);
        }
    }

    // ================================
    // INNER CLASSES
    // ================================

    public static class VendorStats {
        private final long totalVendors;
        private final long activeVendors;
        private final long verifiedVendors;
        private final long approvedVendors;

        public VendorStats(long totalVendors, long activeVendors, long verifiedVendors, long approvedVendors) {
            this.totalVendors = totalVendors;
            this.activeVendors = activeVendors;
            this.verifiedVendors = verifiedVendors;
            this.approvedVendors = approvedVendors;
        }

        // Getters
        public long getTotalVendors() { return totalVendors; }
        public long getActiveVendors() { return activeVendors; }
        public long getVerifiedVendors() { return verifiedVendors; }
        public long getApprovedVendors() { return approvedVendors; }

        @Override
        public String toString() {
            return String.format("VendorStats{total=%d, active=%d, verified=%d, approved=%d}",
                    totalVendors, activeVendors, verifiedVendors, approvedVendors);
        }
    }
}
