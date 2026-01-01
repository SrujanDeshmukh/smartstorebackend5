package com.raghunath.smartstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {

    @NotBlank(message = "Employee name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String employeeName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    private String employeePassword;

    // Optional fields
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String employeeMobile;

    private String employeeRole; // MANAGER, CASHIER, STAFF
    private String department;   // SALES, INVENTORY, etc.
    private Double basicSalary;
    private String shiftTiming;  // MORNING, EVENING, NIGHT
}
