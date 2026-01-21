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
@Document(collection = "users")
public class User {

    @Id
    private String id; // MongoDB-generated user ID

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Field("full_name")
    private String fullName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Indexed(unique = true) // Ensure unique emails
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    @Field("mobile_number")
    private String mobileNumber;

    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    @NotBlank(message = "Password is required")
    private String password; // This will be bcrypt encoded

    // ✅ REMOVED: refreshToken field (now uses separate RefreshToken collection)
    // private String refreshToken; // ❌ REMOVED - using separate collection now

    // Enhanced user profile fields
    @Size(max = 200, message = "Address cannot exceed 200 characters")
    private String address;

    @Pattern(regexp = "^[0-9]{6}$", message = "PIN code must be 6 digits")
    @Field("pin_code")
    private String pinCode;

    @Size(max = 50, message = "City name cannot exceed 50 characters")
    private String city;

    @Size(max = 50, message = "State name cannot exceed 50 characters")
    private String state;

    // Account status fields - FIXED TYPE CONSISTENCY
    @Builder.Default
    @Field("is_verified")
    private Boolean isVerified = false;

    @Builder.Default
    @Field("is_active")
    private Boolean isActive = true; // ✅ FIXED: Changed from primitive boolean to Boolean

    @Builder.Default
    @Field("email_verified")
    private Boolean emailVerified = false;

    @Builder.Default
    @Field("mobile_verified")
    private Boolean mobileVerified = false;

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
    private LocalDateTime accountLockedUntil; // For temporary locks

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

    // Account management
    @Field("deactivated_at")
    private LocalDateTime deactivatedAt;

    @Field("deactivation_reason")
    private String deactivationReason;

    // User preferences
    @Builder.Default
    @Field("notification_enabled")
    private Boolean notificationEnabled = true;

    @Builder.Default
    @Field("marketing_emails")
    private Boolean marketingEmails = false;

    // User activity metrics
    @Builder.Default
    @Field("total_orders")
    private Long totalOrders = 0L;

    @Builder.Default
    @Field("total_spent")
    private Double totalSpent = 0.0;

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
     * Check if user is verified
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
     * Check if account is locked
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Check if user can place orders (active and verified)
     */
    public boolean canPlaceOrders() {
        return isActive() && isVerified() && !isAccountLocked();
    }

    // ================================
    // SETTER METHODS (Fixed)
    // ================================

    /**
     * Setter for isActive field (Boolean)
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
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
    // ACCOUNT MANAGEMENT METHODS
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
     * Verify email
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verify mobile
     */
    public void verifyMobile() {
        this.mobileVerified = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deactivate user account
     */
    public void deactivate(String reason) {
        this.isActive = false;
        this.deactivatedAt = LocalDateTime.now();
        this.deactivationReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reactivate user account
     */
    public void reactivate() {
        this.isActive = true;
        this.deactivatedAt = null;
        this.deactivationReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update password
     */
    public void updatePassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
        this.passwordUpdatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update user activity metrics
     */
    public void updateActivityMetrics(Long orders, Double spent) {
        this.totalOrders = orders != null ? orders : this.totalOrders;
        this.totalSpent = spent != null ? spent : this.totalSpent;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get user status summary
     */
    public String getStatusSummary() {
        return String.format("User{id='%s', active=%s, verified=%s, locked=%s, canOrder=%s}",
                id, isActive(), isVerified(), isAccountLocked(), canPlaceOrders());
    }

    // ================================
    // BACKWARDS COMPATIBILITY (Deprecated)
    // ================================

    /**
     * @deprecated Use separate RefreshToken collection instead
     * This method is kept for backwards compatibility but always returns null
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public String getRefreshToken() {
        return null; // Always return null - use RefreshToken collection
    }

    /**
     * @deprecated Use separate RefreshToken collection instead
     * This method is kept for backwards compatibility but does nothing
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void setRefreshToken(String refreshToken) {
        // Do nothing - use RefreshToken collection instead
    }

    /**
     * Legacy registeredAt getter for backwards compatibility
     */
    public LocalDateTime getRegisteredAt() {
        return this.createdAt;
    }

    /**
     * Legacy registeredAt setter for backwards compatibility
     */
    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.createdAt = registeredAt;
    }
}
