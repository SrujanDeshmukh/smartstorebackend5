package com.raghunath.smartstore.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdvertisementRequest {

    @NotEmpty(message = "At least one product must be selected")
    @Size(min = 1, message = "Product list cannot be empty")
    private List<String> productIds;

    @NotBlank(message = "Description is required")
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    @Future(message = "Offer end date must be in the future")
    private LocalDateTime offerEndDate;
}
