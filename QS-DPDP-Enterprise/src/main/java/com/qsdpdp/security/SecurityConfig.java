package com.qsdpdp.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration for QS-DPDP Enterprise (QShield SOC Platform)
 *
 * SECURITY HARDENING (Phase 1 — Production Readiness):
 * - Profile-aware: dev mode permits Swagger/static, prod enforces JWT on all /api/**
 * - JWT authentication filter with Bearer token extraction
 * - Rate limiting filter (per-IP, stricter on auth endpoints)
 * - Security response headers: CSP, HSTS, X-Frame-Options, X-Content-Type-Options
 * - CORS restricted to configured origins (no wildcards)
 * - CSRF disabled (stateless JWT — no sessions)
 *
 * Standards covered:
 * - OWASP ASVS Level 2 (A01-A10)
 * - NIST SP 800-53: AC-3, AC-7, SC-8, SI-10
 * - IS 15408 (Common Criteria): FIA, FDP, FCS classes
 * - ISO 27001: A.9, A.13, A.14
 *
 * @version 4.0.0
 * @since Phase 1 — Production Security Hardening
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private Environment environment;

    @Value("${qsdpdp.security.cors.allowed-origins:http://localhost:3000,http://localhost:5000,http://localhost:5173,http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        http
            // ── CSRF: disabled for stateless JWT architecture ──
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS: restricted origins ──
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Sessions: stateless (no JSESSIONID) ──
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Security Headers (OWASP A05, NIST SC-8) ──
            .headers(headers -> headers
                // Prevent clickjacking (X-Frame-Options)
                .frameOptions(fo -> fo.deny())
                // Prevent MIME sniffing (X-Content-Type-Options: nosniff)
                .contentTypeOptions(cto -> {})
                // Referrer policy
                .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                // Content-Security-Policy
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none';"
                ))
                // Permissions-Policy
                .permissionsPolicy(pp -> pp.policy(
                    "camera=(), microphone=(), geolocation=(), payment=()"
                ))
            )

            // ── Authorization Rules ──
            .authorizeHttpRequests(auth -> {
                // Always public: static resources
                auth.requestMatchers("/", "/index.html", "/css/**", "/js/**",
                        "/images/**", "/static/**", "/favicon.ico").permitAll();

                // Always public: API docs
                auth.requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll();

                // Always public: health check
                auth.requestMatchers("/actuator/health", "/actuator/info",
                        "/api/v1/health").permitAll();

                // Auth endpoints (login, register)
                auth.requestMatchers("/api/v1/auth/login", "/api/v1/auth/register",
                        "/api/v1/auth/token/refresh").permitAll();

                if (isProd) {
                    // ── PRODUCTION: enforce Role-Based Access (NIST AC-3) ──

                    // Admin-only endpoints
                    auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "MANAGER");
                    auth.requestMatchers("/api/v1/config/**").hasRole("ADMIN");

                    // All other API endpoints require authentication
                    auth.requestMatchers("/api/**").authenticated();

                    // Block actuator details in prod
                    auth.requestMatchers("/actuator/**").hasRole("ADMIN");
                } else {
                    // ── DEV MODE: permit all APIs for Swagger testing ──
                    auth.requestMatchers("/api/**").permitAll();
                    auth.requestMatchers("/actuator/**").permitAll();
                }

                // Default: require authentication
                auth.anyRequest().authenticated();
            })

            // ── Filters ──
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // ── Disable form login and HTTP basic ──
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * CORS configuration — restricted origins, specific headers.
     * OWASP A05 / NIST AC-22 compliant.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Parse allowed origins from config (no wildcards!)
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);

        // Restrict methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Restrict headers (no wildcard)
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "X-Auth-Token",
                "X-Requested-With",
                "Accept",
                "Origin",
                "X-CSRF-Token"
        ));

        // Expose rate limit headers
        config.setExposedHeaders(List.of(
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
