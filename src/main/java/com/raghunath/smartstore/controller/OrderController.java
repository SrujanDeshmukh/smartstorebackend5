package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.entity.Order;
import com.raghunath.smartstore.security.JwtUtil;
import com.raghunath.smartstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vendor/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    @GetMapping("/shop/{shopId}/pending")
    public ResponseEntity<List<Order>> getPendingOrders(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(orderService.getPendingOrders(email, shopId));
    }

    @GetMapping("/shop/{shopId}/completed")
    public ResponseEntity<List<Order>> getCompletedOrders(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(orderService.getCompletedOrders(email, shopId));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<Order>> getAllOrders(
            @RequestHeader("Authorization") String token,
            @PathVariable String shopId) {

        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtUtil.extractUsername(actualToken);

        return ResponseEntity.ok(orderService.getAllOrders(email, shopId));
    }

    @PutMapping("/{orderId}/complete")
    public ResponseEntity<String> completeOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.completeOrder(orderId));
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }
}
