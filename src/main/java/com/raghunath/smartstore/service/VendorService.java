package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.vendor.VendorRegisterRequest;
import com.raghunath.smartstore.dto.vendor.UpdateVendorProfileRequest;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.exception.NotFoundException;
import com.raghunath.smartstore.repository.VendorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.vendor.auto-approve:false}")
    private boolean autoApproveVendors;

    // ================================
    // VENDOR REGISTRATION (KEEP AS-IS)
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

    // ❌ REMOVED: login() - NOW IN UnifiedAuthService
    // ❌ REMOVED: refreshAccessToken() - NOW IN UnifiedAuthService
    // ❌ REMOVED: logout() - NOW IN UnifiedAuthService
    // ❌ REMOVED: updateRefreshToken() - NOW IN UnifiedAuthService
    // ❌ REMOVED: handleFailedLoginAttempt() - NOW IN UnifiedAuthService
    // ❌ REMOVED: isAccountLocked() - NOW IN UnifiedAuthService

    // ================================
    // VENDOR PROFILE UPDATE (KEEP AS-IS)
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
    // VENDOR MANAGEMENT (KEEP AS-IS)
    // ================================
    public Vendor getVendorByEmail(String email) {
        return vendorRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
    }

    public Vendor getVendorById(String vendorId){
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NotFoundException("Vendor not found"));
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
    // UTILITY METHODS (KEEP AS-IS)
    // ================================
    private boolean isValidField(String field) {
        return field != null && !field.trim().isEmpty();
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
    // STATISTICS & REPORTING (KEEP AS-IS)
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
    // INNER CLASSES (KEEP AS-IS)
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
