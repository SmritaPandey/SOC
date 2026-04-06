package com.qshield.idam.service;
import com.qshield.common.audit.AuditService;
import com.qshield.common.security.JwtTokenService;
import com.qshield.idam.model.*; import com.qshield.idam.repository.*;
import org.springframework.data.domain.*; import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest;
import java.time.Instant; import java.time.temporal.ChronoUnit; import java.util.*;

@Service
public class IdamService {
    private final IdamUserRepository userRepo;
    private final JwtTokenService jwtService;
    private final AuditService auditService;
    public IdamService(IdamUserRepository userRepo, JwtTokenService jwtService, AuditService auditService) {
        this.userRepo = userRepo; this.jwtService = jwtService; this.auditService = auditService;
        initDefaultAdmin();
    }
    public Map<String, String> authenticate(String username, String password) {
        IdamUser user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now()))
            throw new RuntimeException("Account locked");
        if (!hashPassword(password).equals(user.getPasswordHash())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= 5) user.setLockedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
            userRepo.save(user);
            auditService.log("IDAM", "AUTH_FAILED", username, null, "Failed attempt #" + user.getFailedAttempts(), "WARN");
            throw new RuntimeException("Invalid credentials");
        }
        user.setFailedAttempts(0); user.setLastLogin(Instant.now()); userRepo.save(user);
        String accessToken = jwtService.generateAccessToken(user.getId().toString(), user.getEmail(), user.getRole(), "CSOC");
        String refreshToken = jwtService.generateRefreshToken(user.getId().toString());
        auditService.log("IDAM", "AUTH_SUCCESS", username, null, "Login successful", "INFO");
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken, "role", user.getRole(), "username", username);
    }
    public IdamUser createUser(IdamUser user) {
        user.setPasswordHash(hashPassword(user.getPasswordHash()));
        auditService.log("IDAM", "USER_CREATED", user.getUsername(), null, "Role: " + user.getRole(), "INFO");
        return userRepo.save(user);
    }
    public Page<IdamUser> getUsers(int page, int size) { return userRepo.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending())); }
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalUsers", userRepo.count()); s.put("adminUsers", userRepo.countByRole("ADMIN"));
        s.put("activeUsers", userRepo.countByEnabled(true)); return s;
    }
    private String hashPassword(String pw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pw.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    private void initDefaultAdmin() {
        if (!userRepo.existsByUsername("admin")) {
            userRepo.save(new IdamUser("admin", "admin@qshield.local", hashPassword("ChangeMe@FirstLogin!"), "ADMIN"));
        }
    }
}
