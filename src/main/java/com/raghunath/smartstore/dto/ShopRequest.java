package com.raghunath.smartstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalTime;


@Data
public class ShopRequest {
    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Shop address is required")
    private String shopAddress;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Shop type is required")
    private String shopType;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be 10 digits")
    private String contactNumber;


    private String description;

    private String gstNumber;

    private Double latitude;
    private Double longitude;

    private String email;
    private LocalTime openingTime;
    private LocalTime closingTime;

    @NotBlank(message = "Banner URL is required")
    private String bannerUrl;
}
