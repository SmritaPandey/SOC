package com.qshield.idam.model;
import jakarta.persistence.*; import java.time.Instant;

@Entity @Table(name = "idam_users")
public class IdamUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 128, unique = true) private String username;
    @Column(nullable = false, length = 256) private String email;
    @Column(nullable = false, length = 256) private String passwordHash;
    @Column(nullable = false, length = 32) private String role; // ADMIN, ANALYST, OPERATOR, VIEWER
    @Column(length = 128) private String fullName;
    @Column(length = 64) private String department;
    @Column(nullable = false) private Boolean enabled = true;
    @Column(nullable = false) private Boolean mfaEnabled = false;
    @Column(length = 64) private String mfaSecret;
    @Column private Instant lastLogin;
    @Column private Integer failedAttempts = 0;
    @Column private Instant lockedUntil;
    @Column private Instant createdAt;
    @Column private Integer riskScore = 0;
    public IdamUser() { this.createdAt = Instant.now(); }
    public IdamUser(String username, String email, String passwordHash, String role) {
        this(); this.username = username; this.email = email; this.passwordHash = passwordHash; this.role = role;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String p) { this.passwordHash = p; }
    public String getRole() { return role; }
    public void setRole(String r) { this.role = r; }
    public String getFullName() { return fullName; }
    public void setFullName(String f) { this.fullName = f; }
    public String getDepartment() { return department; }
    public void setDepartment(String d) { this.department = d; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean e) { this.enabled = e; }
    public Boolean getMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(Boolean m) { this.mfaEnabled = m; }
    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String m) { this.mfaSecret = m; }
    public Instant getLastLogin() { return lastLogin; }
    public void setLastLogin(Instant l) { this.lastLogin = l; }
    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer f) { this.failedAttempts = f; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant l) { this.lockedUntil = l; }
    public Instant getCreatedAt() { return createdAt; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer r) { this.riskScore = r; }
}
