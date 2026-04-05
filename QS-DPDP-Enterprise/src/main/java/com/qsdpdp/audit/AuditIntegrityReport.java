package com.qsdpdp.audit;

/**
 * Audit Integrity Report
 */
public class AuditIntegrityReport {

    private final boolean valid;
    private final int totalEntries;
    private final int validEntries;
    private final int invalidEntries;
    private final String firstInvalidId;

    public AuditIntegrityReport(boolean valid, int totalEntries, int validEntries,
            int invalidEntries, String firstInvalidId) {
        this.valid = valid;
        this.totalEntries = totalEntries;
        this.validEntries = validEntries;
        this.invalidEntries = invalidEntries;
        this.firstInvalidId = firstInvalidId;
    }

    public boolean isValid() {
        return valid;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public int getValidEntries() {
        return validEntries;
    }

    public int getInvalidEntries() {
        return invalidEntries;
    }

    public String getFirstInvalidId() {
        return firstInvalidId;
    }

    @Override
    public String toString() {
        return String.format("AuditIntegrity[valid=%s, total=%d, valid=%d, invalid=%d]",
                valid, totalEntries, validEntries, invalidEntries);
    }
}
