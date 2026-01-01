package com.raghunath.smartstore.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private String id;
    private String employeeCode;
    private String employeeName;
    private String email;
    private String employeeMobile;
    private String shopId;
    private String vendorId;
    private String employeeRole;
    private String department;
    private Boolean isActive;
    private Boolean isVerified;
    private Boolean onProbation;
    private Double basicSalary;
    private String shiftTiming;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    // Permissions
    private Boolean canProcessOrders;
    private Boolean canManageInventory;
    private Boolean canAccessReports;
    private Boolean isSupervisor;

    // Performance metrics
    private Double totalSales;
    private Long ordersProcessed;
    private Double customerRating;
}
