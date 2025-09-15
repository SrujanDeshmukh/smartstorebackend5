package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.ShopRequest;
import com.raghunath.smartstore.entity.Shop;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendor/shops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShopController {

    private final ShopService shopService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<String> addShop(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody ShopRequest request) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String role = jwtUtil.extractRole(actualToken);

        if (!"VENDOR".equals(role)) {
            throw new RuntimeException("Access denied. Vendor role required.");
        }

        String email = jwtUtil.extractUsername(actualToken);
        return ResponseEntity.ok(shopService.addShop(email, request));
    }

    @GetMapping
    public ResponseEntity<List<Shop>> getVendorShops(@RequestHeader("Authorization") String token) {
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(shopService.getVendorShops(email));
    }

    @GetMapping("/{shopId}")
    public ResponseEntity<Shop> getShop(@PathVariable String shopId) {
        return ResponseEntity.ok(shopService.getShopById(shopId));
    }

    @PutMapping("/{shopId}")
    public ResponseEntity<String> updateShop(
            @PathVariable String shopId,
            @Valid @RequestBody ShopRequest request) {

        return ResponseEntity.ok(shopService.updateShop(shopId, request));
    }

    @DeleteMapping("/{shopId}")
    public ResponseEntity<String> deleteShop(@PathVariable String shopId) {
        return ResponseEntity.ok(shopService.deleteShop(shopId));
    }
}
