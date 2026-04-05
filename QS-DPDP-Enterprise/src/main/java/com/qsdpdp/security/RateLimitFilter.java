package com.qsdpdp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter for QS-SOC Platform.
 * Implements per-IP sliding window rate limiting to prevent:
 * - Brute-force password attacks (OWASP A07)
 * - API abuse / DDoS amplification (NIST AC-7)
 * - Credential stuffing attacks
 *
 * Features:
 * - Configurable max requests per window
 * - Automatic cleanup of stale entries
 * - X-RateLimit-* response headers
 * - Stricter limits for auth endpoints
 *
 * @version 1.0.0
 * @since Phase 1 — Production Security Hardening
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int maxRequestsPerWindow;
    private final int authMaxRequestsPerWindow;
    private final long windowSizeMs;

    // IP -> [count, windowStart]
    private final Map<String, long[]> requestCounts = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${qsdpdp.security.rate-limit.max-requests:100}") int maxRequestsPerWindow,
            @Value("${qsdpdp.security.rate-limit.auth-max-requests:10}") int authMaxRequestsPerWindow,
            @Value("${qsdpdp.security.rate-limit.window-seconds:60}") int windowSeconds) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.authMaxRequestsPerWindow = authMaxRequestsPerWindow;
        this.windowSizeMs = windowSeconds * 1000L;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getServletPath();
        boolean isAuthEndpoint = path.contains("/auth/login") || path.contains("/auth/register");

        int limit = isAuthEndpoint ? authMaxRequestsPerWindow : maxRequestsPerWindow;
        String rateLimitKey = clientIp + (isAuthEndpoint ? ":auth" : ":api");

        long now = System.currentTimeMillis();
        long[] entry = requestCounts.computeIfAbsent(rateLimitKey, k -> new long[]{0, now});

        synchronized (entry) {
            // Reset window if expired
            if (now - entry[1] > windowSizeMs) {
                entry[0] = 0;
                entry[1] = now;
            }

            entry[0]++;

            long remaining = Math.max(0, limit - entry[0]);
            long resetTime = (entry[1] + windowSizeMs - now) / 1000;

            // Set rate limit headers (RFC 6585 / draft-ietf-httpapi-ratelimit-headers)
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));

            if (entry[0] > limit) {
                logger.warn("Rate limit exceeded for IP: {} on path: {} ({}/{})",
                        clientIp, path, entry[0], limit);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Rate limit exceeded. Try again in "
                        + resetTime + " seconds.\",\"retryAfter\":" + resetTime + "}");
                return;
            }
        }

        filterChain.doFilter(request, response);

        // Periodic cleanup of stale entries (every ~1000 requests)
        if (requestCounts.size() > 10000) {
            cleanupStaleEntries();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(e -> (now - e.getValue()[1]) > windowSizeMs * 2);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/css")
                || path.startsWith("/js")
                || path.startsWith("/images")
                || path.startsWith("/static")
                || path.equals("/favicon.ico");
    }
}
