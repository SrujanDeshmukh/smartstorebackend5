package com.raghunath.smartstore.controller;

import com.raghunath.smartstore.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/email")
public class EmailController {
    @Autowired
    private EmailService emailService;

    @GetMapping("/send")
    public String sendTestEmail(@RequestParam String to) {
        emailService.sendEmail(to, "Test Email", "Hello from SmartStore Spring Boot!");
        return "Email sent successfully to " + to;
    }
}
