package com.raghunath.smartstore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EmployeeRequest {
    @NotBlank(message = "Employee name is required")
    private String employeeName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String employeeMobile;

    @NotBlank(message = "Employee password is required")
    private String employeePassword;
}
