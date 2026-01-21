package com.raghunath.smartstore.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorLoginProfile {
    private String id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String address;
    private String pinCode;
    private String city;  // Will be null
}
