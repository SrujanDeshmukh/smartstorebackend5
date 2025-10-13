package com.raghunath.smartstore.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "employees")
public class Employee {

    @Id
    private String id; // MongoDB-generated employee ID

    @NotBlank(message = "Vendor ID is required")
    @Indexed // Index for quick vendor-based queries
    @Field("vendor_id")
    private String vendorId; // Reference to vendor/business owner

    @NotBlank(message = "Shop ID is required")
    @Indexed // Index for quick shop-based queries
    @Field("shop_id")
    private String shopId; // Reference to specific shop

    @NotBlank(message = "Employee name is required")
    @Size(min = 2, max = 100, message = "Employee name must be between 2 and 100 characters")
    @Field("employee_name")
    private String employeeName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Indexed(unique = true) // Ensure unique emails across employees
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    @NotBlank(message = "Mobile number is required")
    @Indexed // Index for quick mobile-based lookups
    @Field("employee_mobile")
    private String employeeMobile;

    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    @NotBlank(message = "Password is required")
    private String password; // This will be bcrypt encoded

    // ✅ REMOVED: refreshToken field (now uses separate RefreshToken collection)
    // private String refreshToken; // ❌ REMOVED - using separate collection now

    // Employee role and permissions
    @NotBlank(message = "Employee role is required")
    @Builder.Default
    @Field("employee_role")
    private String employeeRole = "STAFF"; // MANAGER, CASHIER, STAFF, SUPERVISOR

    @Field("department")
    private String department; // SALES, INVENTORY, CUSTOMER_SERVICE, etc.

    @Field("employee_code")
    @Indexed(unique = true) // Unique employee code within organization
    private String employeeCode; // Auto-generated: EMP001, EMP002, etc.

    // Employment details
    @Field("hire_date")
    private LocalDateTime hireDate;

    @Field("probation_end_date")
    private LocalDateTime probationEndDate;

    @Field("employment_type")
    @Builder.Default
    private String employmentType = "FULL_TIME"; // FULL_TIME, PART_TIME, CONTRACT

    @Field("shift_timing")
    private String shiftTiming; // MORNING, EVENING, NIGHT, FLEXIBLE

    // Salary and compensation
    @Field("basic_salary")
    private Double basicSalary;

    @Field("hourly_rate")
    private Double hourlyRate; // For part-time employees

    @Field("commission_rate")
    private Double commissionRate; // Commission percentage on sales

    // Account status fields
    @Builder.Default
    @Field("is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Field("is_verified")
    private Boolean isVerified = false;

    @Builder.Default
    @Field("email_verified")
    private Boolean emailVerified = false;

    @Builder.Default
    @Field("mobile_verified")
    private Boolean mobileVerified = false;

    @Builder.Default
    @Field("on_probation")
    private Boolean onProbation = true;

    // Permissions and access control
    @Builder.Default
    @Field("can_manage_inventory")
    private Boolean canManageInventory = false;

    @Builder.Default
    @Field("can_process_orders")
    private Boolean canProcessOrders = true;

    @Builder.Default
    @Field("can_handle_returns")
    private Boolean canHandleReturns = false;

    @Builder.Default
    @Field("can_access_reports")
    private Boolean canAccessReports = false;

    @Builder.Default
    @Field("is_supervisor")
    private Boolean isSupervisor = false;

    // Security fields
    @Builder.Default
    @Field("failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Field("last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;

    @Field("last_login_at")
    private LocalDateTime lastLoginAt;

    @Field("password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Field("account_locked_until")
    private LocalDateTime accountLockedUntil;

    // Auditing fields with automatic handling
    @CreatedDate
    @Builder.Default
    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("verified_at")
    private LocalDateTime verifiedAt;

    @Field("hired_at")
    private LocalDateTime hiredAt;

    // Account management
    @Field("terminated_at")
    private LocalDateTime terminatedAt;

    @Field("termination_reason")
    private String terminationReason;

    @Field("deactivated_at")
    private LocalDateTime deactivatedAt;

    @Field("deactivation_reason")
    private String deactivationReason;

    // Performance metrics
    @Builder.Default
    @Field("total_sales")
    private Double totalSales = 0.0;

    @Builder.Default
    @Field("orders_processed")
    private Long ordersProcessed = 0L;

    @Builder.Default
    @Field("customer_rating")
    private Double customerRating = 0.0;

    @Builder.Default
    @Field("performance_score")
    private Double performanceScore = 0.0;

    // Contact and personal information
    @Field("emergency_contact_name")
    private String emergencyContactName;

    @Field("emergency_contact_mobile")
    @Pattern(regexp = "^[0-9]{10}$", message = "Emergency contact must be 10 digits")
    private String emergencyContactMobile;

    @Field("address")
    private String address;

    @Pattern(regexp = "^[0-9]{6}$", message = "PIN code must be 6 digits")
    @Field("pin_code")
    private String pinCode;

    // ================================
    // CUSTOM METHODS
    // ================================

    /**
     * Explicit getter for primitive boolean isActive()
     * Safely handles null Boolean values
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }

    /**
     * Check if employee is verified
     */
    public boolean isVerified() {
        return Boolean.TRUE.equals(this.isVerified);
    }

    /**
     * Check if email is verified
     */
    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(this.emailVerified);
    }

    /**
     * Check if mobile is verified
     */
    public boolean isMobileVerified() {
        return Boolean.TRUE.equals(this.mobileVerified);
    }

    /**
     * Check if employee is on probation
     */
    public boolean isOnProbation() {
        return Boolean.TRUE.equals(this.onProbation);
    }

    /**
     * Check if account is locked
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Check if employee can work (active, verified, not locked)
     */
    public boolean canWork() {
        return isActive() && isVerified() && !isAccountLocked();
    }

    /**
     * Check if employee is supervisor
     */
    public boolean isSupervisor() {
        return Boolean.TRUE.equals(this.isSupervisor);
    }

    /**
     * Check if employee can manage inventory
     */
    public boolean canManageInventory() {
        return Boolean.TRUE.equals(this.canManageInventory);
    }

    /**
     * Check if employee can process orders
     */
    public boolean canProcessOrders() {
        return Boolean.TRUE.equals(this.canProcessOrders);
    }

    // ================================
    // SETTER METHODS (Fixed for UnifiedAuthService)
    // ================================

    /**
     * Setter for isActive field (Boolean)
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();

        if (!Boolean.TRUE.equals(isActive)) {
            this.deactivatedAt = LocalDateTime.now();
        }
    }

    /**
     * Convenience method for deactivation (boolean)
     */
    public void setActive(boolean active) {
        this.isActive = active;
        this.updatedAt = LocalDateTime.now();

        if (!active) {
            this.deactivatedAt = LocalDateTime.now();
        } else {
            this.deactivatedAt = null;
            this.deactivationReason = null;
        }
    }

    // ================================
    // EMPLOYEE MANAGEMENT METHODS
    // ================================

    /**
     * Update last login timestamp
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Reset failed login attempts on successful login
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
    }

    /**
     * Record failed login attempt
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts = (this.failedLoginAttempts == null ? 0 : this.failedLoginAttempts) + 1;
        this.lastFailedLoginAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Lock account if too many failed attempts (5 attempts)
        if (this.failedLoginAttempts >= 5) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(30); // 30-minute lock
        }
    }

    /**
     * Reset failed login attempts
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
        this.accountLockedUntil = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark as verified
     */
    public void markAsVerified() {
        this.isVerified = true;
        this.verifiedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Complete probation period
     */
    public void completeProbation() {
        this.onProbation = false;
        this.probationEndDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Promote to supervisor
     */
    public void promoteToSupervisor() {
        this.isSupervisor = true;
        this.canAccessReports = true;
        this.canManageInventory = true;
        this.canHandleReturns = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Terminate employment
     */
    public void terminate(String reason) {
        this.isActive = false;
        this.terminatedAt = LocalDateTime.now();
        this.terminationReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivate employee account
     */
    public void deactivate(String reason) {
        this.isActive = false;
        this.deactivatedAt = LocalDateTime.now();
        this.deactivationReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reactivate employee account
     */
    public void reactivate() {
        this.isActive = true;
        this.deactivatedAt = null;
        this.deactivationReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update performance metrics
     */
    public void updatePerformanceMetrics(Double sales, Long orders, Double rating, Double score) {
        this.totalSales = sales != null ? sales : this.totalSales;
        this.ordersProcessed = orders != null ? orders : this.ordersProcessed;
        this.customerRating = rating != null ? rating : this.customerRating;
        this.performanceScore = score != null ? score : this.performanceScore;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Generate employee code if not set
     */
    public void generateEmployeeCode() {
        if (this.employeeCode == null || this.employeeCode.isEmpty()) {
            // Format: EMP + vendorId_last3digits + timestamp_last4digits
            String vendorSuffix = vendorId != null && vendorId.length() >= 3
                    ? vendorId.substring(vendorId.length() - 3)
                    : "000";
            String timeSuffix = String.valueOf(System.currentTimeMillis()).substring(6, 10);
            this.employeeCode = "EMP" + vendorSuffix + timeSuffix;
        }
    }

    /**
     * Get employee status summary
     */
    public String getStatusSummary() {
        return String.format("Employee{id='%s', code='%s', active=%s, verified=%s, role='%s', canWork=%s}",
                id, employeeCode, isActive(), isVerified(), employeeRole, canWork());
    }

    // ================================
    // BACKWARDS COMPATIBILITY (Deprecated)
    // ================================

    /**
     * @deprecated Use separate RefreshToken collection instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public String getRefreshToken() {
        return null; // Always return null - use RefreshToken collection
    }

    /**
     * @deprecated Use separate RefreshToken collection instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void setRefreshToken(String refreshToken) {
        // Do nothing - use RefreshToken collection instead
    }

    // ================================
    // INITIALIZATION
    // ================================

    /**
     * Post-construction initialization
     */
    public void initializeEmployee() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.hiredAt == null) {
            this.hiredAt = LocalDateTime.now();
        }
        generateEmployeeCode();

        // Set probation end date (typically 6 months)
        if (this.probationEndDate == null && Boolean.TRUE.equals(this.onProbation)) {
            this.probationEndDate = LocalDateTime.now().plusMonths(6);
        }
    }
}
