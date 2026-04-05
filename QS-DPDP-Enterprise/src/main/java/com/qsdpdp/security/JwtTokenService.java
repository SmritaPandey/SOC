package com.qsdpdp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * JWT Token Service for QS-SOC Platform.
 * Handles token generation, validation, and refresh.
 *
 * Security properties:
 * - HMAC-SHA256 signing (256-bit key minimum)
 * - Configurable expiry (default 24h access, 7d refresh)
 * - Role-based claims
 * - Quantum-safe ready (Dilithium signing can be swapped in)
 *
 * @version 1.0.0
 * @since Phase 1 — Production Security Hardening
 */
@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    private final SecretKey signingKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtTokenService(
            @Value("${qsdpdp.security.jwt.secret:#{null}}") String jwtSecret,
            @Value("${qsdpdp.security.jwt.access-token-expiry-ms:86400000}") long accessTokenExpiry,
            @Value("${qsdpdp.security.jwt.refresh-token-expiry-ms:604800000}") long refreshTokenExpiry) {

        if (jwtSecret != null && !jwtSecret.isBlank()) {
            byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } else {
            // Auto-generate secure key for dev mode
            this.signingKey = Jwts.SIG.HS256.key().build();
            logger.warn("JWT secret not configured — using auto-generated key (dev mode only)");
        }

        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    /**
     * Generate an access token for an authenticated user.
     */
    public String generateAccessToken(String userId, String username, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiry);

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .issuer("QShield-SOC-Platform")
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a refresh token.
     */
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiry);

        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .issuer("QShield-SOC-Platform")
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate a token and return its claims.
     *
     * @throws io.jsonwebtoken.JwtException if token is invalid/expired
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer("QShield-SOC-Platform")
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the user ID from a token without full validation
     * (for logging/diagnostics only).
     */
    public String extractUserId(String token) {
        try {
            return validateToken(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }
}
