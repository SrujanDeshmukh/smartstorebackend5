package com.raghunath.smartstore.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShopRequest {
    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Shop address is required")
    private String shopAddress;

    @NotBlank(message = "City is required")
    private String city;

    private String shopType;
    private String contactNumber;
    private String description;
    private String gstNumber;
    private Double latitude;
    private Double longitude;
}
