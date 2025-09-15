package com.raghunath.smartstore.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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

    private String shopType; // grocery, pharmacy, electronics, etc.
    private String contactNumber;
    private String description;
    private String gstNumber;

    private Double latitude;
    private Double longitude;

    private Boolean isActive = true;
    private LocalDateTime createdAt;

    public Shop() {
        this.createdAt = LocalDateTime.now();
    }
}
