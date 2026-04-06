package com.qshield.common.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Filter — Per-IP sliding window rate limiter.
 * NIST AC-7 (Unsuccessful Logon Attempts) + NIST SI-11.
 */
@Component
public class RateLimitFilter implements Filter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final int MAX_AUTH_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60_000;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String clientIp = getClientIp(req);
        String path = req.getServletPath();
        boolean isAuthPath = path.startsWith("/api/v1/auth/");
        int limit = isAuthPath ? MAX_AUTH_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;

        String key = clientIp + (isAuthPath ? ":auth" : ":api");
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter());

        if (counter.isExpired()) {
            counter.reset();
        }

        if (counter.incrementAndGet() > limit) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Rate limit exceeded\",\"retryAfterSeconds\":60}");
            return;
        }

        res.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - counter.get())));
        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        int incrementAndGet() { return count.incrementAndGet(); }
        int get() { return count.get(); }
        boolean isExpired() { return System.currentTimeMillis() - windowStart > WINDOW_MS; }
        void reset() { count.set(0); windowStart = System.currentTimeMillis(); }
    }
}
