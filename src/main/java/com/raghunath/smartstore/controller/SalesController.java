package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.entity.Sales;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/vendor/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SalesController {

    private final SalesService salesService;
    private final JwtUtil jwtUtil;

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<Sales>> getShopSales(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(salesService.getShopSales(email, shopId));
    }

    @GetMapping("/shop/{shopId}/date-range")
    public ResponseEntity<List<Sales>> getSalesByDateRange(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(salesService.getSalesByDateRange(email, shopId, startDate, endDate));
    }
}
