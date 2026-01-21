package com.raghunath.smartstore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityShopsResponse {
    private String city;
    private String shopId1;
    private String shopId2;
}