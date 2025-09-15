package com.raghunath.smartstore.service;

import com.raghunath.smartstore.entity.Order;
import com.raghunath.smartstore.entity.Vendor;
import com.raghunath.smartstore.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final VendorService vendorService;

    public List<Order> getPendingOrders(String vendorEmail, String shopId) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return orderRepository.findByVendorIdAndShopIdAndStatus(vendor.getId(), shopId, "PENDING");
    }

    public List<Order> getCompletedOrders(String vendorEmail, String shopId) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return orderRepository.findByVendorIdAndShopIdAndStatus(vendor.getId(), shopId, "COMPLETED");
    }

    public List<Order> getAllOrders(String vendorEmail, String shopId) {
        Vendor vendor = vendorService.getVendorByEmail(vendorEmail);
        return orderRepository.findByVendorIdAndShopId(vendor.getId(), shopId);
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public String completeOrder(String orderId) {
        Order order = getOrderById(orderId);
        order.setStatus("COMPLETED");
        orderRepository.save(order);
        return "Order completed successfully";
    }

    public String cancelOrder(String orderId) {
        Order order = getOrderById(orderId);
        order.setStatus("CANCELLED");
        orderRepository.save(order);
        return "Order cancelled successfully";
    }

    private String generateOrderOtp() {
        return String.valueOf(1000 + new Random().nextInt(9000));
    }

    private String generateQrCode(String orderId) {
        return "QR_" + orderId + "_" + System.currentTimeMillis();
    }
}
