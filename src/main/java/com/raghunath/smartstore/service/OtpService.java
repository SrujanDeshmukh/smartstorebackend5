package com.raghunath.smartstore.service;

import com.raghunath.smartstore.entity.OtpRecord;
import com.raghunath.smartstore.repository.OtpRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private OtpRepository otpRepository;

    private String generateOtp() {
        return String.valueOf(1000 + new Random().nextInt(9999));
    }

    public void sendOtp(String email) throws MessagingException{
        String otp = generateOtp();

        otpRepository.deleteByEmail(email); //Remove old otp for the same email
        otpRepository.save(new OtpRecord(email, otp)); //Save new otp

        // format for sending the html email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("smartstorebackend@gmail.com");
        helper.setTo(email);
        helper.setSubject("Your SmartStore OTP code");

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        color: black !important;
                        font-family: Arial, sans-serif;
                    }
                    a, a:visited, a:hover, a:active {
                        color: black !important;
                        text-decoration: none !important;
                    }
                    h1, h2, p {
                        color: black !important;
                    }
                </style>
            </head>
            <body>
                <h2>SmartStore Email Verification</h2>
                <p>Use the following OTP to verify your email:</p>
                <h1 style="color:black;">%s</h1>
                <p>This OTP will expire in 5 minutes.</p>
            </body>
            </html>
        """, otp);

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public boolean verifyOtp(String email, String otp){
        Optional<OtpRecord> recordOpt = otpRepository.findByEmail(email);

        if(recordOpt.isEmpty()){
            return false; // no otp found
        }

        OtpRecord record = recordOpt.get();

        // check expiry (5 minutes)
        if(record.getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now())){
            otpRepository.deleteByEmail(email);
            return false;
        }

        // check match
        if(record.getOtp().equals(otp)){
            otpRepository.deleteByEmail(email);
            return true;
        }

        return false; // wrong otp
    }
}
