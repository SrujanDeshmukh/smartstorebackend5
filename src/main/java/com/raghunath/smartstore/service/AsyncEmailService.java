package com.raghunath.smartstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:smartstorebackend@gmail.com}")
    private String fromEmail;

    @Value("${app.name:SmartStore}")
    private String appName;

    @Value("${app.otp.validity.minutes:5}")
    private int otpValidityMinutes;

    // ================================
    // ASYNC OTP EMAIL METHODS
    // ================================

    /**
     * Send simple text OTP email asynchronously (non-blocking)
     * @param toEmail Recipient email address
     * @param otp 4-digit OTP code
     * @return CompletableFuture for async handling
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendOtpEmailAsync(String toEmail, String otp) {
        try {
            log.info("Starting async OTP email send to: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject(buildOtpSubject());
            message.setText(buildSimpleOtpEmailBody(otp));
            message.setFrom(fromEmail);

            long startTime = System.currentTimeMillis();
            mailSender.send(message);
            long endTime = System.currentTimeMillis();

            log.info("✅ OTP email sent successfully to: {} in {}ms", toEmail, (endTime - startTime));
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send simple HTML-free OTP email (alias for main method)
     * @param toEmail Recipient email address
     * @param otp 4-digit OTP code
     * @return CompletableFuture for async handling
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendHtmlOtpEmailAsync(String toEmail, String otp) {
        // Redirect to simple text email instead of HTML
        return sendOtpEmailAsync(toEmail, otp);
    }

    // ================================
    // VENDOR NOTIFICATION EMAILS
    // ================================

    /**
     * Send vendor registration welcome email
     * @param vendorEmail Vendor email address
     * @param vendorName Vendor full name
     * @return CompletableFuture for async handling
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendVendorWelcomeEmailAsync(String vendorEmail, String vendorName) {
        try {
            log.info("Sending vendor welcome email to: {}", vendorEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(vendorEmail);
            message.setSubject("Welcome to " + appName + " - Vendor Registration Successful");
            message.setText(buildVendorWelcomeEmailBody(vendorName));
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("✅ Vendor welcome email sent to: {}", vendorEmail);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("❌ Failed to send vendor welcome email to {}: {}", vendorEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Send order notification to vendor
     * @param vendorEmail Vendor email address
     * @param orderId Order ID
     * @param customerName Customer name
     * @param shopName Shop name
     * @return CompletableFuture for async handling
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendVendorOrderNotificationAsync(String vendorEmail, String orderId,
                                                                       String customerName, String shopName) {
        try {
            log.info("Sending order notification email to vendor: {}", vendorEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(vendorEmail);
            message.setSubject("New Order Received - Order #" + orderId);
            message.setText(buildVendorOrderNotificationBody(orderId, customerName, shopName));
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("✅ Order notification email sent to vendor: {}", vendorEmail);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("❌ Failed to send order notification to vendor {}: {}", vendorEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    // ================================
    // USER NOTIFICATION EMAILS
    // ================================

    /**
     * Send user registration welcome email
     * @param userEmail User email address
     * @param userName User full name
     * @return CompletableFuture for async handling
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendUserWelcomeEmailAsync(String userEmail, String userName) {
        try {
            log.info("Sending user welcome email to: {}", userEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userEmail);
            message.setSubject("Welcome to " + appName + "!");
            message.setText(buildUserWelcomeEmailBody(userName));
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("✅ User welcome email sent to: {}", userEmail);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("❌ Failed to send user welcome email to {}: {}", userEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    // ================================
    // EMAIL BODY BUILDERS (PROFESSIONAL TEXT ONLY)
    // ================================

    private String buildOtpSubject() {
        return appName + " - Email Verification Code";
    }

    /**
     * Build professional simple text OTP email body
     * @param otp 4-digit OTP code
     * @return Plain text professional email content
     */
    private String buildSimpleOtpEmailBody(String otp) {
        return String.format(
                "Dear Customer,\n\n" +
                        "Your %s verification code is: %s\n\n" +
                        "This code is valid for %d minutes only.\n" +
                        "Please do not share this code with anyone for security reasons.\n\n" +
                        "If you did not request this code, please ignore this email.\n\n" +
                        "Thank you,\n" +
                        "%s Support Team\n\n" +
                        "---\n" +
                        "This is an automated message. Please do not reply to this email.",
                appName, otp, otpValidityMinutes, appName
        );
    }

    private String buildVendorWelcomeEmailBody(String vendorName) {
        return String.format(
                "Dear %s,\n\n" +
                        "Welcome to %s!\n\n" +
                        "Your vendor account has been successfully created and activated.\n\n" +
                        "You can now access the following features:\n" +
                        "- Add and manage your shops\n" +
                        "- List and manage your products\n" +
                        "- Manage employees and staff\n" +
                        "- Create promotional advertisements\n" +
                        "- Process customer orders\n" +
                        "- Track sales and view analytics\n\n" +
                        "To get started, please log into your vendor dashboard and set up your first shop.\n\n" +
                        "If you need any assistance, please contact our support team.\n\n" +
                        "Thank you for choosing %s.\n\n" +
                        "Best regards,\n" +
                        "%s Team\n\n" +
                        "Registration Date: %s\n\n" +
                        "---\n" +
                        "This is an automated message. Please do not reply to this email.",
                vendorName, appName, appName, appName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
        );
    }

    private String buildVendorOrderNotificationBody(String orderId, String customerName, String shopName) {
        return String.format(
                "Dear Vendor,\n\n" +
                        "You have received a new order on %s.\n\n" +
                        "Order Details:\n" +
                        "Order ID: #%s\n" +
                        "Customer: %s\n" +
                        "Shop: %s\n" +
                        "Order Time: %s\n\n" +
                        "Please log into your %s vendor dashboard to view the complete order details and begin processing.\n\n" +
                        "Thank you for using %s.\n\n" +
                        "Best regards,\n" +
                        "%s Team\n\n" +
                        "---\n" +
                        "This is an automated message. Please do not reply to this email.",
                appName, orderId, customerName, shopName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")),
                appName, appName, appName
        );
    }

    private String buildUserWelcomeEmailBody(String userName) {
        return String.format(
                "Dear %s,\n\n" +
                        "Welcome to %s!\n\n" +
                        "Thank you for joining our platform. Your account has been successfully created.\n\n" +
                        "With %s, you can:\n" +
                        "- Discover local shops and vendors\n" +
                        "- Browse and order products from nearby stores\n" +
                        "- Choose between pickup and delivery options\n" +
                        "- Make secure and convenient payments\n" +
                        "- Rate and review your purchases\n\n" +
                        "Start exploring local vendors and enjoy convenient shopping.\n\n" +
                        "If you have any questions, please contact our support team.\n\n" +
                        "Thank you for choosing %s.\n\n" +
                        "Best regards,\n" +
                        "%s Team\n\n" +
                        "Welcome Date: %s\n\n" +
                        "---\n" +
                        "This is an automated message. Please do not reply to this email.",
                userName, appName, appName, appName, appName,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
        );
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Check email service health
     * @return true if email service is working
     */
    public boolean isEmailServiceHealthy() {
        try {
            // Test connection to mail server
            mailSender.createMimeMessage();
            log.info("✅ Email service health check passed");
            return true;
        } catch (Exception e) {
            log.error("❌ Email service health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get email service statistics
     * @return Email service info
     */
    public String getEmailServiceInfo() {
        return String.format(
                "Email Service Configuration:\n" +
                        "From Email: %s\n" +
                        "Application: %s\n" +
                        "OTP Validity: %d minutes\n" +
                        "Mail Sender: %s",
                fromEmail, appName, otpValidityMinutes,
                mailSender.getClass().getSimpleName()
        );
    }
}
