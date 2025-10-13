package com.raghunath.smartstore.dto.vendor;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVendorProfileRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Optional mobile must be 10 digits if provided")
    private String mobileOptional;

    @Pattern(regexp = "^[\\w.\\-]+@[\\w.\\-]+$", message = "Invalid UPI ID format")
    private String upiId;

    @Size(max = 500, message = "Business description cannot exceed 500 characters")
    private String businessDescription;

    @Size(max = 200, message = "Business address cannot exceed 200 characters")
    private String businessAddress;

    @Pattern(regexp = "^[0-9]{6}$", message = "PIN code must be 6 digits")
    private String pinCode;

    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GST number format")
    private String gstNumber;
}
