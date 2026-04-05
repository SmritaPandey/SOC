package com.qsdpdp.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Security Configuration for QS-DPDP Enterprise
 * 
 * SECURITY HARDENING:
 * - CORS restricted to localhost only (configurable for production)
 * - Strict HTTP method allowlist
 * - Credential support enabled for session-based auth
 * 
 * @version 1.0.0
 * @since Phase 2 (Security Hardening)
 */
@Configuration
public class WebSecurityConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        // SECURITY FIX C-03: Restrict CORS to known origins only
                        // In production, replace with actual domain(s)
                        .allowedOrigins(
                                "http://localhost:8080",
                                "http://127.0.0.1:8080",
                                "http://localhost:3000", // Web app
                                "http://localhost:4000", // Alt dev frontend
                                "http://localhost:5000"  // Mobile app (PWA)
                )
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders(
                                "Content-Type",
                                "Authorization",
                                "X-Auth-Token",
                                "X-Requested-With")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
