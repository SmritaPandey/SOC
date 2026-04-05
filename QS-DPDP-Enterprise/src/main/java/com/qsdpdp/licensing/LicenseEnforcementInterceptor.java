package com.qsdpdp.licensing;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * License Enforcement Interceptor
 *
 * Validates the current license on every API request.
 * Caches validation result and re-checks every 60 minutes.
 * Returns HTTP 403 with license error JSON if validation fails.
 *
 * Whitelisted paths that work without a valid license:
 * - /api/licensing/fingerprint — needed to get fingerprint for license request
 * - /api/licensing/activate — needed to activate the license
 * - /api/licensing/activate-file — needed to upload license file
 * - /api/licensing/status — needed to check current status
 * - /api/v1/health — health check for monitoring
 * - /swagger-ui/** — API docs
 * - /v3/api-docs/** — OpenAPI docs
 *
 * @version 1.0.0
 * @since Enterprise Deployment
 */
@Component
public class LicenseEnforcementInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(LicenseEnforcementInterceptor.class);

    /** Re-validate license every 60 minutes */
    private static final long CACHE_DURATION_SECONDS = 3600;

    /** Paths that are accessible without a valid license */
    private static final Set<String> WHITELISTED_PATHS = Set.of(
            "/api/licensing/fingerprint",
            "/api/licensing/activate",
            "/api/licensing/activate-file",
            "/api/licensing/status",
            "/api/licensing/validate",
            "/api/v1/health",
            "/actuator/health"
    );

    /** Path prefixes that are accessible without a valid license */
    private static final Set<String> WHITELISTED_PREFIXES = Set.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars/",
            "/css/",
            "/js/",
            "/images/",
            "/favicon"
    );

    @Autowired
    private LicensingService licensingService;

    // Cached validation state
    private volatile boolean cachedValid = false;
    private volatile String cachedError = null;
    private volatile long lastCheckTime = 0;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/licensing/fingerprint",
                        "/api/licensing/activate",
                        "/api/licensing/activate-file",
                        "/api/licensing/status",
                        "/api/licensing/validate",
                        "/api/v1/health");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String path = request.getRequestURI();

        // Allow whitelisted paths
        if (isWhitelisted(path)) {
            return true;
        }

        // Allow non-API paths (static resources, frontend)
        if (!path.startsWith("/api/")) {
            return true;
        }

        // Check cached license validity — NEVER throw 500
        try {
            if (isLicenseValid()) {
                return true;
            }
        } catch (Exception e) {
            // If license check fails, allow request in DEMO mode
            logger.debug("License check error, allowing in DEMO mode: {}", e.getMessage());
            return true;
        }

        // License is invalid — block request
        logger.warn("License enforcement blocked request: {} {} — {}", 
                request.getMethod(), path, cachedError);
        sendLicenseError(response, cachedError);
        return false;
    }

    /**
     * Check if the license is valid, using cache.
     */
    private boolean isLicenseValid() {
        long now = Instant.now().getEpochSecond();

        // Use cache if within duration
        if (now - lastCheckTime < CACHE_DURATION_SECONDS) {
            return cachedValid;
        }

        // Re-validate
        synchronized (this) {
            // Double-check after acquiring lock
            if (now - lastCheckTime < CACHE_DURATION_SECONDS) {
                return cachedValid;
            }

            try {
                // Ensure service is initialized
                try { licensingService.initialize(); } catch (Exception e) {
                    logger.debug("License service init skipped: {}", e.getMessage());
                }

                License license = licensingService.getCurrentLicense();

                if (license == null) {
                    // No license = DEMO mode, allow access
                    cachedValid = true;
                    cachedError = null;
                    logger.info("No license found — running in DEMO mode");
                } else if (license.isExpired()) {
                    cachedValid = false;
                    cachedError = "License expired on " + license.getExpiresAt() + 
                                ". Please renew your license.";
                } else if (license.getStatus() == License.LicenseStatus.SUSPENDED) {
                    cachedValid = false;
                    cachedError = "License is suspended. Contact NeurQ AI Labs support.";
                } else if (license.getStatus() == License.LicenseStatus.REVOKED) {
                    cachedValid = false;
                    cachedError = "License has been revoked. Contact NeurQ AI Labs support.";
                } else {
                    // License is ACTIVE or DEMO — both are allowed
                    cachedValid = true;
                    cachedError = null;
                }
            } catch (Exception e) {
                logger.error("License validation error — allowing DEMO mode", e);
                // Default to allowing requests if license check fails
                cachedValid = true;
                cachedError = null;
            }

            lastCheckTime = now;
        }

        return cachedValid;
    }

    /**
     * Reset the validation cache (called after license activation/deactivation).
     */
    public void resetCache() {
        lastCheckTime = 0;
        cachedValid = false;
        cachedError = null;
        logger.info("License enforcement cache reset");
    }

    private boolean isWhitelisted(String path) {
        if (WHITELISTED_PATHS.contains(path)) return true;
        for (String prefix : WHITELISTED_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private void sendLicenseError(HttpServletResponse response, String error) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String json = String.format(
                "{\"error\":\"LICENSE_REQUIRED\",\"message\":\"%s\"," +
                "\"activateUrl\":\"/api/licensing/activate\"," +
                "\"fingerprintUrl\":\"/api/licensing/fingerprint\"}",
                error != null ? error.replace("\"", "'") : "License validation failed");
        response.getWriter().write(json);
    }
}
