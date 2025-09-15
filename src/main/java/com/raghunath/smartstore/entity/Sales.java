package com.raghunath.smartstore.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "sales")
public class Sales {

    @Id
    private String id;

    private String vendorId; // business vendor id
    private String shopId; // shop id
    private String productId;
    private String productName;
    private Integer quantitySold;
    private Double totalAmount;
    private LocalDateTime saleDate;

    public Sales() {
        this.saleDate = LocalDateTime.now();
    }
}
