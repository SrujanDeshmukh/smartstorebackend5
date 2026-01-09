package com.raghunath.smartstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopResponse {

    private String shopId;
    private String shopName;
    private String description;
    private String city;
    private String address;
    private String phone;
    private String email;
    private String shopType;
    private String gstNumber;

    private Double latitude;
    private Double longitude;

    // Business hours
    private String openingTime;
    private String closingTime;

    // Status
    private Boolean isOpen;
    private Boolean isActive;
    private Boolean isApproved;

    // Vendor info
    private String vendorName;
    private String vendorId;
    private String vendorEmail;
    private String vendorPhone;

    // Stats
    private Integer totalProducts;
    private Double rating;
}
