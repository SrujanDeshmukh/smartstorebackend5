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
    private String id;

    @NotBlank(message = "Vendor ID is required")
    private String vendorId;

    @NotBlank(message = "Shop ID is required")
    private String shopId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotEmpty(message = "At least one product must be selected")
    private List<String> productIds;

    @NotBlank(message = "Description is required")
    private String description;

    private String imageUrl; // You'll add this manually after designing

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Boolean isActive = true;
    private Boolean isApproved = false; // You'll update this in DB directly

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Advertisement() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
