package com.raghunath.smartstore.dto.auth;

import com.raghunath.smartstore.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginProfile {
    private String id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String address;
    private String pinCode;
    private String city;
    private boolean emailVerified;
    private boolean mobileVerified;
    private long totalOrders;

    // ✅ Constructor from User entity (EXACT 7 fields)
    public UserLoginProfile(User user) {
        this.id = user.getId();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.mobileNumber = user.getMobileNumber();
        this.address = user.getAddress();
        this.pinCode = user.getPinCode();
        this.city = user.getCity();
        this.emailVerified = Boolean.TRUE.equals(user.isEmailVerified());
        this.mobileVerified = Boolean.TRUE.equals(user.isMobileVerified());
        this.totalOrders = user.getTotalOrders() != null ? user.getTotalOrders() : 0L;
    }
}
