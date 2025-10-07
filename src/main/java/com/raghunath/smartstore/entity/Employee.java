package com.raghunath.smartstore.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "employees")
public class Employee {

    @Id
    private String id; // employee id

    @NotBlank(message = "Vendor ID is required")
    private String vendorId; // business vendor id

    @NotBlank(message = "Shop ID is required")
    private String shopId; // shop id

    @NotBlank(message = "Employee name is required")
    private String employeeName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String employeeMobile;

    @NotBlank(message = "Employee password is required")
    private String password; // password field named correctly

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;  // added email field

    private Boolean isActive = true;

    private String refreshToken; // added refresh token field

    private LocalDateTime createdAt;

    public Employee() {
        this.createdAt = LocalDateTime.now();
    }

    // Getter for isActive to return primitive boolean
    public boolean isActive() {
        return isActive != null && isActive;
    }
}
