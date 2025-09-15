package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.ProductRequest;
import com.raghunath.smartstore.entity.Product;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendor/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final JwtUtil jwtUtil;

    @PostMapping("/shop/{shopId}")
    public ResponseEntity<String> addProduct(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId,
            @Valid @RequestBody ProductRequest request) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String role = jwtUtil.extractRole(actualToken);

        if (!"VENDOR".equals(role)) {
            throw new RuntimeException("Access denied. Vendor role required.");
        }

        String email = jwtUtil.extractUsername(actualToken);
        return ResponseEntity.ok(productService.addProduct(email, shopId, request));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<Product>> getShopProducts(@PathVariable String shopId) {
        return ResponseEntity.ok(productService.getShopProducts(shopId));
    }

    @GetMapping
    public ResponseEntity<List<Product>> getVendorProducts(@RequestHeader("Authorization") String token) {
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(productService.getVendorProducts(email));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(productService.getProductById(productId));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<String> updateProduct(
            @PathVariable String productId,
            @Valid @RequestBody ProductRequest request) {

        return ResponseEntity.ok(productService.updateProduct(productId, request));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable String productId) {
        return ResponseEntity.ok(productService.deleteProduct(productId));
    }
}
