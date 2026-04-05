package com.qsdpdp.iam;

import java.util.*;

/**
 * Role definition for RBAC aligned with DPDP organizational structure
 * 
 * @version 1.0.0
 * @since Module 11
 */
public enum Role {

    // System Roles
    SUPER_ADMIN("Super Administrator", "Full system access", 100),

    // Privacy Organization
    DPO("Data Protection Officer", "DPDP mandated DPO role", 90),
    PRIVACY_MANAGER("Privacy Manager", "Privacy program management", 80),
    PRIVACY_ANALYST("Privacy Analyst", "Privacy operations", 60),

    // Compliance
    COMPLIANCE_HEAD("Compliance Head", "Overall compliance oversight", 85),
    COMPLIANCE_OFFICER("Compliance Officer", "Compliance monitoring", 70),
    COMPLIANCE_ANALYST("Compliance Analyst", "Compliance analysis", 55),

    // Security
    CISO("Chief Information Security Officer", "Security oversight", 85),
    SECURITY_MANAGER("Security Manager", "Security operations", 75),
    SECURITY_ANALYST("Security Analyst", "Security monitoring", 60),
    SOC_OPERATOR("SOC Operator", "Security operations center", 50),

    // IT Operations
    IT_ADMIN("IT Administrator", "IT system administration", 70),
    DB_ADMIN("Database Administrator", "Database management", 65),
    SYSTEM_OPERATOR("System Operator", "Day-to-day operations", 50),

    // Business Users
    DEPARTMENT_HEAD("Department Head", "Department oversight", 60),
    BUSINESS_USER("Business User", "Standard business access", 40),
    DATA_STEWARD("Data Steward", "Data quality management", 55),

    // External
    AUDITOR("Auditor", "Audit access (read-only)", 45),
    REGULATOR("Regulator", "Regulatory access", 95),

    // Minimal Access
    VIEWER("Viewer", "Read-only access", 20),
    GUEST("Guest", "Minimal guest access", 10);

    private final String displayName;
    private final String description;
    private final int accessLevel;

    Role(String displayName, String description, int accessLevel) {
        this.displayName = displayName;
        this.description = description;
        this.accessLevel = accessLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    public boolean canAccess(Role requiredRole) {
        return this.accessLevel >= requiredRole.accessLevel;
    }

    public static List<Role> getPrivacyRoles() {
        return Arrays.asList(DPO, PRIVACY_MANAGER, PRIVACY_ANALYST);
    }

    public static List<Role> getSecurityRoles() {
        return Arrays.asList(CISO, SECURITY_MANAGER, SECURITY_ANALYST, SOC_OPERATOR);
    }

    public static List<Role> getComplianceRoles() {
        return Arrays.asList(COMPLIANCE_HEAD, COMPLIANCE_OFFICER, COMPLIANCE_ANALYST);
    }
}
