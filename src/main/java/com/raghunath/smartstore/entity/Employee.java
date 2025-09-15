package com.raghunath.smartstore.entity;

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
    private String employeePassword;

    private Boolean isActive = true;
    private LocalDateTime createdAt;

    public Employee() {
        this.createdAt = LocalDateTime.now();
    }
}
