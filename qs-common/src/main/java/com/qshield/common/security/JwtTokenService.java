package com.qshield.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stateless JWT Token Service — shared across all QShield CSOC products.
 * Implements NIST IA-5 (Authenticator Management) and OWASP ASVS 3.5.
 */
@Service
public class JwtTokenService {

    @Value("${qshield.jwt.secret:QShield-CSOC-Default-Secret-Change-In-Production-2026!}")
    private String jwtSecret;

    @Value("${qshield.jwt.access-token-expiry:1800000}")
    private long accessTokenExpiry; // 30 minutes

    @Value("${qshield.jwt.refresh-token-expiry:604800000}")
    private long refreshTokenExpiry; // 7 days

    private SecretKey signingKey;
    private final Set<String> revokedTokens = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String userId, String email, String role, String product) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .claim("product", product)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        if (revokedTokens.contains(token)) {
            throw new JwtException("Token has been revoked");
        }
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public void revokeToken(String token) {
        revokedTokens.add(token);
    }

    public String getUserIdFromToken(String token) {
        return validateToken(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            validateToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
