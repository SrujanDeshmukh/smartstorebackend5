package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.AdvertisementRequest;
import com.raghunath.smartstore.entity.Advertisement;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.AdvertisementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendor/advertisements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdvertisementController {

    private final AdvertisementService advertisementService;
    private final JwtUtil jwtUtil;

    @PostMapping("/shop/{shopId}")
    public ResponseEntity<String> createAdvertisement(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId,
            @Valid @RequestBody AdvertisementRequest request) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String role = jwtUtil.extractRole(actualToken);

        if (!"VENDOR".equals(role)) {
            throw new IllegalArgumentException("Access denied. Vendor role required.");
        }

        String email = jwtUtil.extractUsername(actualToken);
        String result = advertisementService.createAdvertisement(email, shopId, request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<Advertisement>> getShopAdvertisements(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(advertisementService.getShopAdvertisements(email, shopId));
    }

    @GetMapping("/{advertisementId}")
    public ResponseEntity<Advertisement> getAdvertisement(
            @RequestHeader("Authorization") String token,
            @PathVariable String advertisementId) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(advertisementService.getAdvertisementById(advertisementId));
    }

    @PutMapping("/{advertisementId}")
    public ResponseEntity<String> updateAdvertisement(
            @RequestHeader("Authorization") String token,
            @PathVariable String advertisementId,
            @Valid @RequestBody AdvertisementRequest request) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String role = jwtUtil.extractRole(actualToken);

        if (!"VENDOR".equals(role)) {
            throw new IllegalArgumentException("Access denied. Vendor role required.");
        }

        String email = jwtUtil.extractUsername(actualToken);
        return ResponseEntity.ok(advertisementService.updateAdvertisement(email, advertisementId, request));
    }

    @DeleteMapping("/{advertisementId}")
    public ResponseEntity<String> deleteAdvertisement(
            @RequestHeader("Authorization") String token,
            @PathVariable String advertisementId) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String role = jwtUtil.extractRole(actualToken);

        if (!"VENDOR".equals(role)) {
            throw new IllegalArgumentException("Access denied. Vendor role required.");
        }

        String email = jwtUtil.extractUsername(actualToken);
        return ResponseEntity.ok(advertisementService.deleteAdvertisement(email, advertisementId));
    }
}
