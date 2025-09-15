package com.raghunath.smartstore.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app")
@Validated
@Data
public class AppProperties {

    private String name = "SmartStore";

    private User user = new User();
    private Otp otp = new Otp();

    @Data
    public static class User {
        private String defaultRole = "USER";
        private int maxLoginAttempts = 5;
        private int accountLockoutMinutes = 30;
    }

    @Data
    public static class Otp {
        @Min(1)
        private int validityMinutes = 5;

        @Min(1)
        private int maxAttempts = 3;

        @Min(10)
        private int resendCooldownSeconds = 60;
    }
}

