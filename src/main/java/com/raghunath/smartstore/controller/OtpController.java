package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.service.OtpService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class OtpController {

    @Autowired
    private OtpService otpService;

    // Send otp api
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            otpService.sendOtp(email);
            response.put("status", "success");
            response.put("message", "OTP sent successfully to " + email);
            return ResponseEntity.ok(response);
        } catch (MessagingException e) {
            response.put("status", "error");
            response.put("message", "Failed to send OTP");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestParam String email, @RequestParam String otp){
        Map<String, Object> response = new HashMap<>();
        boolean isVerified = otpService.verifyOtp(email, otp);

        if(isVerified) {
            response.put("Status", "success");
            response.put("message", "OTP Verified Successfully");
            return ResponseEntity.ok(response);
        }
        else{
            response.put("status", "error");
            response.put("message", "Invalid or Expired OTP");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}
