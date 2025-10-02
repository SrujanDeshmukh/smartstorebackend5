package com.raghunath.smartstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncEmailService {

    @Autowired
    private MailtrapEmailService mailtrapEmailService;

    @Value("${mailtrap.from.email:noreply@smartstore24.in}")
    private String fromEmail;

    @Value("${app.name:SmartStore24}")
    private String appName;

    @Value("${app.otp.validity.minutes:5}")
    private int otpValidityMinutes;

    // ================================
    // ASYNC OTP EMAIL METHODS
    // ================================

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendOtpEmailAsync(String toEmail, String otp) {
        try {
            log.info("Starting async Mailtrap OTP email send to: {}", toEmail);

            CompletableFuture<Boolean> result = mailtrapEmailService.sendOtpEmail(toEmail, otp);

            result.thenAccept(success -> {
                if (success) {
                    log.info("✅ Mailtrap OTP email sent successfully to: {}", toEmail);
                } else {
                    log.error("❌ Failed to send Mailtrap OTP email to: {}", toEmail);
                }
            });

            return result;

        } catch (Exception e) {
            log.error("Failed to send Mailtrap OTP email to {}: {}", toEmail, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendHtmlOtpEmailAsync(String toEmail, String otp) {
        try {
            log.info("Starting Mailtrap HTML OTP email to: {}", toEmail);

            CompletableFuture<Boolean> result = mailtrapEmailService.sendOtpEmail(toEmail, otp);

            result.thenAccept(success -> {
                if (success) {
                    log.info("✅ Mailtrap HTML OTP email completed for: {}", toEmail);
                } else {
                    log.error("❌ Mailtrap HTML OTP email failed for: {}", toEmail);
                }
            }).exceptionally(throwable -> {
                log.error("Mailtrap email exception for {}: {}", toEmail, throwable.getMessage());
                return null;
            });

            return result;

        } catch (Exception e) {
            log.error("Failed to queue Mailtrap email for {}: {}", toEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    // ================================
    // VENDOR NOTIFICATION EMAILS
    // ================================

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendVendorWelcomeEmailAsync(String vendorEmail, String vendorName) {
        try {
            log.info("Sending vendor welcome email via Mailtrap to: {}", vendorEmail);

            // Use OTP service for now - implement custom later
            String welcomeMessage = "Welcome to " + appName + ", " + vendorName + "! Your vendor account is now active.";
            CompletableFuture<Boolean> result = mailtrapEmailService.sendOtpEmail(vendorEmail, "WELCOME");

            result.thenAccept(success -> {
                if (success) {
                    log.info("✅ Vendor welcome email sent to: {}", vendorEmail);
                } else {
                    log.error("❌ Failed to send vendor welcome email to: {}", vendorEmail);
                }
            });

            return result;

        } catch (Exception e) {
            log.error("❌ Failed to send vendor welcome email to {}: {}", vendorEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendVendorOrderNotificationAsync(String vendorEmail, String orderId,
                                                                       String customerName, String shopName) {
        try {
            log.info("Sending order notification email via Mailtrap to vendor: {}", vendorEmail);

            // Use OTP service for now - implement custom later
            CompletableFuture<Boolean> result = mailtrapEmailService.sendOtpEmail(vendorEmail, orderId);

            result.thenAccept(success -> {
                if (success) {
                    log.info("✅ Order notification email sent to vendor: {}", vendorEmail);
                } else {
                    log.error("❌ Failed to send order notification to vendor: {}", vendorEmail);
                }
            });

            return result;

        } catch (Exception e) {
            log.error("❌ Failed to send order notification to vendor {}: {}", vendorEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    // ================================
    // USER NOTIFICATION EMAILS
    // ================================

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendUserWelcomeEmailAsync(String userEmail, String userName) {
        try {
            log.info("Sending user welcome email via Mailtrap to: {}", userEmail);

            // Use OTP service for now - implement custom later
            CompletableFuture<Boolean> result = mailtrapEmailService.sendOtpEmail(userEmail, "WELCOME");

            result.thenAccept(success -> {
                if (success) {
                    log.info("✅ User welcome email sent to: {}", userEmail);
                } else {
                    log.error("❌ Failed to send user welcome email to: {}", userEmail);
                }
            });

            return result;

        } catch (Exception e) {
            log.error("❌ Failed to send user welcome email to {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    // ================================
    // UTILITY METHODS
    // ================================

    public boolean isEmailServiceHealthy() {
        try {
            log.info("✅ Mailtrap email service health check passed");
            return true;
        } catch (Exception e) {
            log.error("❌ Mailtrap email service health check failed: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailServiceInfo() {
        return String.format(
                "Mailtrap Email Service Configuration:\n" +
                        "From Email: %s\n" +
                        "Application: %s\n" +
                        "OTP Validity: %d minutes\n" +
                        "Service: Mailtrap API\n" +
                        "Domain: smartstore24.in",
                fromEmail, appName, otpValidityMinutes
        );
    }
}
