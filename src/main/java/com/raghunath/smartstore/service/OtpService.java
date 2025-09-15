package com.raghunath.smartstore.service;

import com.raghunath.smartstore.config.AppProperties;
import com.raghunath.smartstore.entity.OtpRecord;
import com.raghunath.smartstore.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final AsyncEmailService asyncEmailService;
    private final AppProperties appProperties;

    @Value("${app.otp.validity.minutes:5}")
    private int otpValidityMinutes;

    @Value("${app.otp.max.attempts:3}")
    private int maxOtpAttempts;

    @Value("${app.otp.resend.cooldown.seconds:60}")
    private int resendCooldownSeconds;

    // ================================
    // OTP GENERATION & SENDING
    // ================================

    /**
     * Generate and send OTP asynchronously
     * @param email Recipient email
     * @return Success message
     */
    public String sendOtp(String email) {
        int validityMinutes = appProperties.getOtp().getValidityMinutes();
        try {
            // Validate email format
            if (!isValidEmail(email)) {
                return "Invalid email format";
            }

            // Check for existing OTP and cooldown period
            String cooldownCheck = checkResendCooldown(email);
            if (!cooldownCheck.equals("OK")) {
                return cooldownCheck;
            }

            // Generate new OTP
            String otp = generateOtp();

            // Delete any existing OTP for this email (resend logic)
            otpRepository.deleteByEmail(email);

            // Create and save new OTP record
            OtpRecord otpRecord = new OtpRecord();
            otpRecord.setEmail(email);
            otpRecord.setOtp(otp);
            otpRecord.setExpirationTime(LocalDateTime.now().plusMinutes(otpValidityMinutes));
            otpRecord.setAttempts(0); // Reset attempts
            otpRecord.setCreatedAt(LocalDateTime.now());

            otpRepository.save(otpRecord);

            // Send email asynchronously (non-blocking)
            CompletableFuture<Boolean> emailResult = asyncEmailService.sendHtmlOtpEmailAsync(email, otp);

            // Handle email result asynchronously
            emailResult.whenComplete((success, throwable) -> {
                if (success) {
                    log.info("✅ OTP sent successfully to: {}", email);
                } else {
                    log.error("❌ Failed to send OTP email to: {}", email);
                }
            });

            log.info("OTP generated and queued for sending to: {}", email);
            return "OTP sent successfully"; // Returns immediately due to async processing

        } catch (Exception e) {
            log.error("Error in sendOtp for email {}: {}", email, e.getMessage());
            return "Failed to send OTP. Please try again.";
        }
    }

    /**
     * Resend OTP with cooldown protection
     * @param email Recipient email
     * @return Success/error message
     */
    public String resendOtp(String email) {
        try {
            Optional<OtpRecord> existingOtp = otpRepository.findByEmail(email);

            if (existingOtp.isPresent()) {
                OtpRecord record = existingOtp.get();

                // Check cooldown period (prevent spam)
                LocalDateTime cooldownTime = record.getCreatedAt().plusSeconds(resendCooldownSeconds);
                if (LocalDateTime.now().isBefore(cooldownTime)) {
                    long secondsLeft = java.time.Duration.between(LocalDateTime.now(), cooldownTime).getSeconds();
                    return String.format("Please wait %d seconds before requesting another OTP", secondsLeft);
                }
            }

            // Send new OTP
            return sendOtp(email);

        } catch (Exception e) {
            log.error("Error in resendOtp for email {}: {}", email, e.getMessage());
            return "Failed to resend OTP. Please try again.";
        }
    }

    // ================================
    // OTP VERIFICATION
    // ================================

    /**
     * Verify OTP with enhanced security checks
     * @param email User email
     * @param otp OTP to verify
     * @return Verification result
     */
    public OtpVerificationResult verifyOtp(String email, String otp) {
        try {
            Optional<OtpRecord> recordOpt = otpRepository.findByEmail(email);

            if (recordOpt.isEmpty()) {
                return new OtpVerificationResult(false, "No OTP found for this email");
            }

            OtpRecord record = recordOpt.get();

            // Check if expired
            if (record.getExpirationTime().isBefore(LocalDateTime.now())) {
                otpRepository.deleteByEmail(email);
                return new OtpVerificationResult(false, "OTP has expired. Please request a new one.");
            }

            // Check max attempts
            if (record.getAttempts() >= maxOtpAttempts) {
                otpRepository.deleteByEmail(email);
                return new OtpVerificationResult(false, "Maximum verification attempts exceeded. Please request a new OTP.");
            }

            // Increment attempt count
            record.setAttempts(record.getAttempts() + 1);
            otpRepository.save(record);

            // Verify OTP
            if (record.getOtp().equals(otp)) {
                otpRepository.deleteByEmail(email); // One-time use
                log.info("✅ OTP verified successfully for email: {}", email);
                return new OtpVerificationResult(true, "OTP verified successfully");
            } else {
                int attemptsLeft = maxOtpAttempts - record.getAttempts();
                return new OtpVerificationResult(false,
                        String.format("Invalid OTP. %d attempts remaining.", attemptsLeft));
            }

        } catch (Exception e) {
            log.error("Error verifying OTP for email {}: {}", email, e.getMessage());
            return new OtpVerificationResult(false, "OTP verification failed. Please try again.");
        }
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Generate 4-digit OTP
     * @return Random 4-digit OTP
     */
    private String generateOtp() {
        return String.valueOf(1000 + new Random().nextInt(9000));
    }

    /**
     * Validate email format
     * @param email Email to validate
     * @return true if valid
     */
    private boolean isValidEmail(String email) {
        return email != null &&
                email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$") &&
                email.length() <= 100;
    }

    /**
     * Check resend cooldown period
     * @param email Email to check
     * @return "OK" or error message
     */
    private String checkResendCooldown(String email) {
        Optional<OtpRecord> existingOtp = otpRepository.findByEmail(email);

        if (existingOtp.isPresent()) {
            OtpRecord record = existingOtp.get();
            LocalDateTime cooldownTime = record.getCreatedAt().plusSeconds(resendCooldownSeconds);

            if (LocalDateTime.now().isBefore(cooldownTime)) {
                long secondsLeft = java.time.Duration.between(LocalDateTime.now(), cooldownTime).getSeconds();
                return String.format("Please wait %d seconds before requesting a new OTP", secondsLeft);
            }
        }

        return "OK";
    }

    /**
     * Get OTP status for email
     * @param email Email to check
     * @return OTP status information
     */
    public OtpStatusResult getOtpStatus(String email) {
        try {
            Optional<OtpRecord> recordOpt = otpRepository.findByEmail(email);

            if (recordOpt.isEmpty()) {
                return new OtpStatusResult(false, "No OTP found", 0, 0);
            }

            OtpRecord record = recordOpt.get();

            // Check if expired
            if (record.getExpirationTime().isBefore(LocalDateTime.now())) {
                return new OtpStatusResult(false, "OTP expired", 0, 0);
            }

            long timeLeftSeconds = java.time.Duration.between(LocalDateTime.now(), record.getExpirationTime()).getSeconds();
            int attemptsLeft = maxOtpAttempts - record.getAttempts();

            return new OtpStatusResult(true, "OTP active", timeLeftSeconds, attemptsLeft);

        } catch (Exception e) {
            log.error("Error getting OTP status for email {}: {}", email, e.getMessage());
            return new OtpStatusResult(false, "Error checking OTP status", 0, 0);
        }
    }

    // ================================
    // SCHEDULED CLEANUP
    // ================================

    /**
     * Cleanup expired OTPs - runs every 2 minutes
     * Optimized to use bulk deletion instead of individual deletions
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void deleteExpiredOtps() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<OtpRecord> expiredRecords = otpRepository.findByExpirationTimeBefore(now);

            if (!expiredRecords.isEmpty()) {
                otpRepository.deleteAll(expiredRecords);
                log.info("🗑️ Cleaned up {} expired OTP records", expiredRecords.size());
            }

        } catch (Exception e) {
            log.error("Error during OTP cleanup: {}", e.getMessage());
        }
    }

    /**
     * Get OTP service statistics
     * @return Service statistics
     */
    public OtpServiceStats getServiceStats() {
        try {
            long totalOtps = otpRepository.count();
            long activeOtps = otpRepository.countByExpirationTimeAfter(LocalDateTime.now());

            return new OtpServiceStats(totalOtps, activeOtps, otpValidityMinutes, maxOtpAttempts);

        } catch (Exception e) {
            log.error("Error getting service stats: {}", e.getMessage());
            return new OtpServiceStats(0, 0, otpValidityMinutes, maxOtpAttempts);
        }
    }

    // ================================
    // INNER CLASSES FOR RESULTS
    // ================================

    /**
     * OTP Verification Result
     */
    public static class OtpVerificationResult {
        private final boolean success;
        private final String message;

        public OtpVerificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    /**
     * OTP Status Result
     */
    public static class OtpStatusResult {
        private final boolean active;
        private final String status;
        private final long timeLeftSeconds;
        private final int attemptsLeft;

        public OtpStatusResult(boolean active, String status, long timeLeftSeconds, int attemptsLeft) {
            this.active = active;
            this.status = status;
            this.timeLeftSeconds = timeLeftSeconds;
            this.attemptsLeft = attemptsLeft;
        }

        public boolean isActive() { return active; }
        public String getStatus() { return status; }
        public long getTimeLeftSeconds() { return timeLeftSeconds; }
        public int getAttemptsLeft() { return attemptsLeft; }
    }

    /**
     * OTP Service Statistics
     */
    public static class OtpServiceStats {
        private final long totalOtps;
        private final long activeOtps;
        private final int validityMinutes;
        private final int maxAttempts;

        public OtpServiceStats(long totalOtps, long activeOtps, int validityMinutes, int maxAttempts) {
            this.totalOtps = totalOtps;
            this.activeOtps = activeOtps;
            this.validityMinutes = validityMinutes;
            this.maxAttempts = maxAttempts;
        }

        public long getTotalOtps() { return totalOtps; }
        public long getActiveOtps() { return activeOtps; }
        public int getValidityMinutes() { return validityMinutes; }
        public int getMaxAttempts() { return maxAttempts; }
    }
}
