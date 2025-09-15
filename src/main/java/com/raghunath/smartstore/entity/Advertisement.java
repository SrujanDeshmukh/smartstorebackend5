package com.raghunath.smartstore.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "advertisements")
public class Advertisement {

    @Id
    private String id; // advertisement id

    @NotBlank(message = "Vendor ID is required")
    private String vendorId; // business vendor id

    @NotBlank(message = "Shop ID is required")
    private String shopId; // shop id

    @NotEmpty(message = "At least one product must be selected")
    private List<String> productIds; // array of product ids on which offer applies

    @NotBlank(message = "Description is required")
    private String description; // e.g., "50% off"

    private LocalDateTime offerEndDate;
    private Boolean isActive = true;
    private LocalDateTime createdAt;

    public Advertisement() {
        this.createdAt = LocalDateTime.now();
    }
}
