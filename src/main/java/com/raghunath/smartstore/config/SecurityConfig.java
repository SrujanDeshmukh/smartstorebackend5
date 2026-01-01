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
                        // ✅ PUBLIC ENDPOINTS
                        .requestMatchers(
                                "/auth/register",
                                "/auth/user/**",
                                "/vendor/register",
                                "/vendor/login",
                                "/unified-auth/login",
                                "/unified-auth/refresh",
                                "/unified-auth/status",
                                "/otp/**",
                                "/public/**",
                                "/health",
                                "/error"
                        ).permitAll()

                        // ✅ PROTECTED ENDPOINTS (AUTHENTICATION MANDATORY)
                        .requestMatchers(
                                "/auth/logout",
                                "/auth/refresh",
                                "/vendor/profile/**",
                                "/vendor/upi",           // 🔒 AUTHENTICATION REQUIRED
                                "/vendor/logout",
                                "/unified-auth/logout",
                                "/unified-auth/logout-all"
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

    // ❌ REMOVED: PasswordEncoder bean (already exists in PasswordConfig.java)
}
