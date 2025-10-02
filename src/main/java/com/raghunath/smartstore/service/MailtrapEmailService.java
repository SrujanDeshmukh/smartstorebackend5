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
            String subject = "SmartStore24 OTP Verification";
            String htmlContent = createOtpEmailTemplate(otp);

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

            // Convert to JSON
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

    private String createOtpEmailTemplate(String otp) {
        return String.format("""
            <html>
            <head>
                <style>
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; font-family: Arial, sans-serif; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }
                    .otp-box { background: white; border: 2px dashed #007bff; padding: 20px; margin: 20px 0; text-align: center; border-radius: 10px; }
                    .otp-code { font-size: 36px; font-weight: bold; color: #007bff; letter-spacing: 8px; margin: 10px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #6c757d; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>SmartStore24</h1>
                        <p>OTP Verification</p>
                    </div>
                    <div class="content">
                        <h2>Your verification code is ready!</h2>
                        <p>Use this One-Time Password to complete your SmartStore24 verification:</p>
                        
                        <div class="otp-box">
                            <div class="otp-code">%s</div>
                            <p style="margin: 0; color: #6c757d;">Enter this code in the app</p>
                        </div>
                        
                        <p><strong>⏰ This OTP expires in 5 minutes</strong></p>
                        <p>If you didn't request this code, please ignore this email.</p>
                        
                        <div class="footer">
                            <p>This email was sent from SmartStore24</p>
                            <p>Secure • Fast • Reliable</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """, otp);
    }
}
