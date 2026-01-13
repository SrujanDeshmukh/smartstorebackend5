package com.raghunath.smartstore.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Document(collection = "shops")
@CompoundIndex(name = "unique_shop_index",
               def = "{'vendorId': 1, 'shopName': 1, 'shopAddress': 1}",
               unique = true)
public class Shop {

    @Id
    private String id; // shop id

    @NotBlank(message = "Vendor ID is required")
    private String vendorId; // business vendor id

    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Shop address is required")
    private String shopAddress;

    @Indexed
    @NotBlank
    private String city;

    private String shopType; // grocery, pharmacy, electronics, etc.
    private String contactNumber;
    private String description;
    private String gstNumber;

    // Location
    private Double latitude;
    private Double longitude;

    private String email;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Boolean isOpen;

    private String bannerUrl;

    private Boolean isActive = true;
    private Boolean isApproved = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String vendorName;
    private String vendorEmail;
    private String vendorPhone;

    private Integer totalProducts;
    private Double rating = 0.0;

    public Shop() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
        this.isApproved = false;
        this.totalProducts = 0;
        this.rating = 0.0;
    }
}
