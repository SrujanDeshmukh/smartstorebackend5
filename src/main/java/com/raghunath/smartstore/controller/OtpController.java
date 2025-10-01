package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.dto.EmailRequest;
import com.raghunath.smartstore.dto.OtpVerificationRequest;
import com.raghunath.smartstore.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class OtpController {

    private final OtpService otpService;

    // ================================
    // SEND OTP ENDPOINTS
    // ================================

    /**
     * Send OTP to email address
     * @param request Email request containing recipient email
     * @return Response with success/error status
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody EmailRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("OTP send request received for email: {}", request.getEmail());

            String result = otpService.sendOtp(request.getEmail());

            if (result.startsWith("OTP sent successfully") || result.equals("OTP sent successfully")) {
                response.put("status", "success");
                response.put("message", "OTP sent successfully to " + request.getEmail());
                response.put("email", request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                // Handle specific error cases
                response.put("status", "error");
                response.put("message", result);

                if (result.contains("Invalid email format")) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                } else if (result.contains("Please wait")) {
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }

        } catch (Exception e) {
            log.error("Error sending OTP to {}: {}", request.getEmail(), e.getMessage());
            response.put("status", "error");
            response.put("message", "Failed to send OTP. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Resend OTP to email address
     * @param request Email request containing recipient email
     * @return Response with success/error status
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, Object>> resendOtp(@Valid @RequestBody EmailRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("OTP resend request received for email: {}", request.getEmail());

            String result = otpService.resendOtp(request.getEmail());

            if (result.startsWith("OTP sent successfully") || result.equals("OTP sent successfully")) {
                response.put("status", "success");
                response.put("message", "OTP resent successfully to " + request.getEmail());
                response.put("email", request.getEmail());
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", result);

                if (result.contains("Please wait")) {
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }

        } catch (Exception e) {
            log.error("Error resending OTP to {}: {}", request.getEmail(), e.getMessage());
            response.put("status", "error");
            response.put("message", "Failed to resend OTP. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ================================
    // VERIFY OTP ENDPOINT (FIXED)
    // ================================

    /**
     * Verify OTP for email address
     * @param email User email address
     * @param otp OTP code to verify
     * @return Response with verification status
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody OtpVerificationRequest request) {
        try {
            OtpService.OtpVerificationResult result = otpService.verifyOtp(request.getEmail(), request.getOtp());

            Map<String, Object> response = new HashMap<>();
            response.put("status", result.isSuccess() ? "success" : "error");
            response.put("message", result.getMessage());
            response.put("verified", result.isSuccess());
            response.put("email", request.getEmail().toLowerCase());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "OTP verification failed");
            errorResponse.put("verified", false);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // ================================
    // OTP STATUS ENDPOINT
    // ================================

    /**
     * Get OTP status for email address
     * @param email User email address
     * @return Response with OTP status information
     */
    @GetMapping("/otp-status")
    public ResponseEntity<Map<String, Object>> getOtpStatus(
            @RequestParam @Email(message = "Invalid email format") String email) {

        Map<String, Object> response = new HashMap<>();

        try {
            OtpService.OtpStatusResult statusResult = otpService.getOtpStatus(email);

            response.put("status", "success");
            response.put("email", email);
            response.put("otpActive", statusResult.isActive());
            response.put("statusMessage", statusResult.getStatus());
            response.put("timeLeftSeconds", statusResult.getTimeLeftSeconds());
            response.put("attemptsLeft", statusResult.getAttemptsLeft());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting OTP status for {}: {}", email, e.getMessage());
            response.put("status", "error");
            response.put("message", "Failed to get OTP status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ================================
    // SERVICE STATISTICS ENDPOINT
    // ================================

    /**
     * Get OTP service statistics (for admin/monitoring)
     * @return Service statistics
     */
    @GetMapping("/otp-stats")
    public ResponseEntity<Map<String, Object>> getServiceStats() {
        try {
            OtpService.OtpServiceStats stats = otpService.getServiceStats();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalOtps", stats.getTotalOtps());
            response.put("activeOtps", stats.getActiveOtps());
            response.put("validityMinutes", stats.getValidityMinutes());
            response.put("maxAttempts", stats.getMaxAttempts());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting OTP service stats: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to get service statistics");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ================================
    // ERROR HANDLING
    // ================================

    /**
     * Handle validation errors
     * @param ex Validation exception
     * @return Error response
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            jakarta.validation.ConstraintViolationException ex) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "Validation failed: " + ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
