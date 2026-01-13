package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.ShopRequest;
import com.raghunath.smartstore.dto.ShopResponse;
import com.raghunath.smartstore.entity.Shop;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public ResponseEntity<List<ShopResponse>> getVendorShops(@RequestHeader("Authorization") String token) {
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        List<Shop> shops =  shopService.getVendorShops(email);
        List<ShopResponse> responses = shops.stream()
                .map(shop -> shopService.convertToShopResponse(shop, false))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{shopId}")
    public ResponseEntity<ShopResponse> getShop(@PathVariable String shopId) {
        return ResponseEntity.ok(shopService.getShopDetailsById(shopId));
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
