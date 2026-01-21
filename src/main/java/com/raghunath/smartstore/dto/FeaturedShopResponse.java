package com.raghunath.smartstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeaturedShopResponse {
    private String shopId;
    private String shopName;
    private String city;
    private String openingTime;
    private String closingTime;
    private Integer totalProducts;
    private Double rating;
    private String bannerUrl;
    private Boolean isApproved;
}
