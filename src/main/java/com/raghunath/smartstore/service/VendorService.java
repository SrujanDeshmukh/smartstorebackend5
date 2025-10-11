package com.raghunath.smartstore.service;

import com.raghunath.smartstore.dto.AuthResponse;
import com.raghunath.smartstore.dto.VendorRegisterRequest;
import com.raghunath.smartstore.entity.RefreshToken;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.RefreshTokenRepository;
import com.raghunath.smartstore.repository.VendorRepository;
import com.raghunath.smartstore.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ================================
    // VENDOR REGISTRATION (SIMPLE)
    // ================================

    public String register(@Valid VendorRegisterRequest request) {
        try {
            log.info("Vendor registration attempt for email: {}", request.getEmail());

            if (vendorRepository.findByEmail(request.getEmail()).isPresent()) {
                return "Email is already registered";
            }

            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return "Passwords do not match";
            }

            Vendor vendor = new Vendor();
            vendor.setFullName(request.getFullName());
            vendor.setEmail(request.getEmail());
            vendor.setMobile(request.getMobile());
            vendor.setMobileOptional(request.getMobileOptional());
            vendor.setPassword(passwordEncoder.encode(request.getPassword()));

            vendorRepository.save(vendor);

            log.info("✅ Vendor registered successfully: {}", vendor.getEmail());
            return "Vendor registered successfully";

        } catch (Exception e) {
            log.error("❌ Error during vendor registration for email {}: {}", request.getEmail(), e.getMessage());
            return "Registration failed. Please try again.";
        }
    }

    // ================================
    // VENDOR LOGIN (FIXED TO STORE TOKEN)
    // ================================

    public AuthResponse login(String email, String password) {
        try {
            log.info("Vendor login attempt for email: {}", email);

            Vendor vendor = vendorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            if (!passwordEncoder.matches(password, vendor.getPassword())) {
                throw new RuntimeException("Invalid email or password");
            }

            String accessToken = jwtUtil.generateAccessToken(email, "VENDOR");
            String refreshToken = jwtUtil.generateRefreshToken(email);

            // ✅ FIX: Store refresh token in database
            updateRefreshToken(email, refreshToken);

            log.info("✅ Vendor logged in successfully: {}", email);
            return new AuthResponse(accessToken, refreshToken);

        } catch (Exception e) {
            log.error("❌ Vendor login failed for email {}: {}", email, e.getMessage());
            throw e;
        }
    }

    // ================================
    // TOKEN MANAGEMENT (SIMPLIFIED)
    // ================================

    /**
     * Store refresh token in database
     */
    private void updateRefreshToken(String email, String refreshToken) {
        try {
            // Delete old refresh tokens for this vendor
            refreshTokenRepository.deleteByEmailAndUserType(email, "VENDOR");

            // Save new refresh token with VENDOR type
            RefreshToken refreshTokenEntity = new RefreshToken(email, refreshToken, "VENDOR");
            refreshTokenRepository.save(refreshTokenEntity);

            log.debug("✅ Refresh token saved for vendor: {}", email);

        } catch (Exception e) {
            log.error("❌ Failed to save refresh token for vendor {}: {}", email, e.getMessage());
        }
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        try {
            if (!jwtUtil.isTokenValid(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }

            String email = jwtUtil.extractUsername(refreshToken);

            RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            if (!stored.getEmail().equals(email) || !"VENDOR".equals(stored.getUserType())) {
                throw new RuntimeException("Invalid refresh token");
            }

            // Verify vendor still exists
            vendorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));

            String newAccessToken = jwtUtil.generateAccessToken(email, "VENDOR");
            String newRefreshToken = jwtUtil.generateRefreshToken(email);

            // Update refresh token
            updateRefreshToken(email, newRefreshToken);

            log.info("✅ Vendor tokens refreshed successfully: {}", email);
            return new AuthResponse(newAccessToken, newRefreshToken);

        } catch (Exception e) {
            log.error("❌ Vendor token refresh failed: {}", e.getMessage());
            throw e;
        }
    }

    // ================================
    // LOGOUT (SIMPLE)
    // ================================

    public void logout(String email) {
        try {
            refreshTokenRepository.deleteByEmailAndUserType(email, "VENDOR");
            log.info("✅ Vendor logged out successfully: {}", email);
        } catch (Exception e) {
            log.error("❌ Error during vendor logout for email {}: {}", email, e.getMessage());
        }
    }

    // ================================
    // VENDOR MANAGEMENT (SIMPLE)
    // ================================

    public Vendor getVendorByEmail(String email) {
        return vendorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
    }

    public String updateUpiId(String email, String upiId) {
        try {
            Vendor vendor = getVendorByEmail(email);
            vendor.setUpiId(upiId);
            vendorRepository.save(vendor);

            log.info("✅ UPI ID updated for vendor: {}", email);
            return "UPI ID updated successfully";

        } catch (Exception e) {
            log.error("❌ Error updating UPI ID for vendor {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to update UPI ID: " + e.getMessage());
        }
    }
}
