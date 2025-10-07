package com.raghunath.smartstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class MailtrapEmailService {

    private static final Logger log = LoggerFactory.getLogger(MailtrapEmailService.class);
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    @Value("${mailtrap.api.token}")
    private String apiToken;

    @Value("${mailtrap.from.email}")
    private String fromEmail;

    @Value("${mailtrap.from.name}")
    private String fromName;

    public MailtrapEmailService() {
        this.client = new OkHttpClient.Builder().build();
        this.objectMapper = new ObjectMapper();
    }

    @Async
    public CompletableFuture<Boolean> sendOtpEmail(String toEmail, String otp) {
        try {
            String subject = "SmartStore24 - Email Verification Code";
            String htmlContent = createSimpleOtpEmailTemplate(otp);

            boolean sent = sendEmail(toEmail, subject, htmlContent, "OTP_VERIFICATION");

            if (sent) {
                log.info("✅ OTP email sent successfully to: {}", toEmail);
            } else {
                log.error("❌ Failed to send OTP email to: {}", toEmail);
            }

            return CompletableFuture.completedFuture(sent);

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async
    public CompletableFuture<Boolean> sendCustomEmail(String toEmail, String subject, String htmlContent, String category) {
        try {
            boolean sent = sendEmail(toEmail, subject, htmlContent, category);

            if (sent) {
                log.info("✅ Custom email ({}) sent successfully to: {}", category, toEmail);
            } else {
                log.error("❌ Failed to send custom email ({}) to: {}", category, toEmail);
            }

            return CompletableFuture.completedFuture(sent);

        } catch (Exception e) {
            log.error("Failed to send custom email ({}) to {}: {}", category, toEmail, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    private boolean sendEmail(String toEmail, String subject, String htmlContent, String category) {
        try {
            // Create email payload
            Map<String, Object> emailData = new HashMap<>();

            // From address
            Map<String, String> from = new HashMap<>();
            from.put("email", fromEmail);
            from.put("name", fromName);
            emailData.put("from", from);

            // To address
            Map<String, String> to = new HashMap<>();
            to.put("email", toEmail);
            emailData.put("to", List.of(to));

            // Email content
            emailData.put("subject", subject);
            emailData.put("html", htmlContent);
            emailData.put("category", category);

            // Convert to JSON // 
            String jsonPayload = objectMapper.writeValueAsString(emailData);

            // Create HTTP request
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, jsonPayload);

            Request request = new Request.Builder()
                    .url("https://send.api.mailtrap.io/api/send")
                    .method("POST", body)
                    .addHeader("Authorization", "Bearer " + apiToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Send request
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("Mailtrap API response: {}", response.code());
                    return true;
                } else {
                    log.error("Mailtrap API error: {} - {}", response.code(), response.message());
                    return false;
                }
            }

        } catch (IOException e) {
            log.error("Failed to send email via Mailtrap API: {}", e.getMessage());
            return false;
        }
    }

    private String createSimpleOtpEmailTemplate(String otp) {
        return String.format("""
            <html>
            <head>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        font-size: 14px;
                        line-height: 1.6;
                        color: #333333;
                        margin: 0;
                        padding: 20px;
                        background-color: #ffffff;
                    }
                    .email-content {
                        max-width: 600px;
                        margin: 0 auto;
                    }
                    p {
                        margin: 10px 0;
                    }
                </style>
            </head>
            <body>
                <div class="email-content">
                    <p>Dear Customer,</p>
                    
                    <p>Your SmartStore24 verification code is: <strong>%s</strong></p>
                    
                    <p>This code is valid for 5 minutes only.<br>
                    Please do not share this code with anyone for security reasons.</p>
                    
                    <p>If you did not request this code, please ignore this email.</p>
                    
                    <p>Thank you,<br>
                    SmartStore24 Support Team</p>
                    
                    <p><small>This is an automated message. Please do not reply to this email.</small></p>
                </div>
            </body>
            </html>
            """, otp);
    }
}