package com.raghunath.smartstore.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "orders")
public class Order {

    @Id
    private String id; // order id

    @NotBlank(message = "User ID is required")
    private String userId; // customer id

    @NotBlank(message = "Vendor ID is required")
    private String vendorId; // business vendor id

    @NotBlank(message = "Shop ID is required")
    private String shopId; // shop id

    private List<OrderItem> items; // products in the order
    private Double totalAmount;
    private String orderType; // "COUNTER" or "HOME_DELIVERY"
    private String status; // "PENDING", "COMPLETED", "CANCELLED"
    private String paymentMethod;
    private String deliveryAddress; // for home delivery
    private String orderOtp; // 4-digit OTP for verification
    private String qrCode; // QR code for order
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public Order() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    @Data
    public static class OrderItem {
        private String productId;
        private String productName;
        private Integer quantity;
        private Double price;
        private Double totalPrice;
    }
}
