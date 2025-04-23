package com.example.otpservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration. Allows public access to /register endpoint.
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures the security filter chain.
     *
     * - /register is publicly accessible.
     * - All other endpoints require authentication.
     * - CSRF is disabled (typical for stateless REST APIs).
     *
     * @param http HttpSecurity instance
     * @return configured SecurityFilterChain
     * @throws Exception on error
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disables CSRF protection explicitly
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/register").permitAll() // Open endpoint
                        .anyRequest().authenticated()             // Require auth for others
                );

        return http.build();
    }
}