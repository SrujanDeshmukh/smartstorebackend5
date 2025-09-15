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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public String register(@Valid VendorRegisterRequest request) {
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
        return "Vendor registered successfully";
    }

    public AuthResponse login(String email, String password) {
        Vendor vendor = vendorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, vendor.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(email, "VENDOR");
        String refreshToken = jwtUtil.generateRefreshToken(email);

        // Store refresh token
        refreshTokenRepository.deleteByEmail(email);
        refreshTokenRepository.save(new RefreshToken(email, refreshToken));

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String email = jwtUtil.extractUsername(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (!stored.getEmail().equals(email)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtUtil.generateAccessToken(email, "VENDOR");
        return new AuthResponse(newAccessToken, refreshToken);
    }

    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }

    public Vendor getVendorByEmail(String email) {
        return vendorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));
    }

    public String updateUpiId(String email, String upiId) {
        Vendor vendor = getVendorByEmail(email);
        vendor.setUpiId(upiId);
        vendorRepository.save(vendor);
        return "UPI ID updated successfully";
    }
}
