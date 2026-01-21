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
    private String shopType;
    private String gstNumber;
    private Double latitude;
    private Double longitude;
    private String email;
    private Boolean isOpen;
    private String openingTime;
    private String closingTime;
    private Integer totalProducts;
    private Double rating;
    private String bannerUrl;
    private Boolean isApproved;

    // ✅ VENDOR FIELDS (for detail view - optional null)
    private String vendorName;
    private String vendorId;
    private String vendorEmail;
    private String vendorPhone;
}
