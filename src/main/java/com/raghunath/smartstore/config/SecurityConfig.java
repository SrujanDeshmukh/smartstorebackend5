package com.raghunath.smartstore.config;

import com.raghunath.smartstore.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // ✅ PUBLIC ENDPOINTS (NO AUTHENTICATION REQUIRED)
                        .requestMatchers(
                                // Auth endpoints
                                "/auth/register",
                                "/auth/user/**",
                                "/auth/update-address",
                                "/auth/shops/**",
                                "/vendor/register",
                                "/vendor/login",
                                "/unified-auth/login",
                                "/unified-auth/refresh",
                                "/unified-auth/status",

                                // OTP endpoints
                                "/otp/**",

                                // ✅ NEW - User public endpoints (Krishna's app)
                                "/api/user/**",              // All user endpoints
                                "/api/auth/**",              // Auth endpoints with /api prefix

                                // Other public endpoints
                                "/public/**",
                                "/health",
                                "/error"
                        ).permitAll()

                        // ✅ PROTECTED ENDPOINTS (AUTHENTICATION REQUIRED)
                        .requestMatchers(
                                // Auth logout
                                "/auth/logout",
                                "/auth/refresh",

                                // Vendor endpoints
                                "/vendor/profile/**",
                                "/vendor/upi",
                                "/vendor/logout",
                                "/api/vendor/**",            // Vendor API endpoints

                                // Unified auth
                                "/unified-auth/logout",
                                "/unified-auth/logout-all",

                                // Admin endpoints
                                "/api/admin/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Allow all origins (for development - restrict in production)
        configuration.setAllowedOriginPatterns(List.of("*"));

        // ✅ Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        // ✅ Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // ✅ Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // ✅ Expose authorization header
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count"
        ));

        // ✅ Cache preflight requests for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
