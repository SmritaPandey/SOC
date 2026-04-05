package com.qsdpdp.pii;

/**
 * Scan Target Types - Antivirus-style scan target definitions
 * Defines all possible locations that can be scanned for PII
 * Supports system-wide, drive-level, network, cloud, and memory scanning
 * 
 * @version 2.0.0
 * @since Phase 7
 */
public enum ScanTarget {

    SYSTEM("Full System Scan", "Scan all local drives and accessible storage", true, "4-8 hours"),
    DRIVE("Drive Scan", "Scan an entire logical drive (C:, D:, etc.)", false, "1-4 hours"),
    FOLDER("Folder Scan", "Scan a specific folder and its subfolders", false, "Minutes to hours"),
    NETWORK_PATH("Network Path Scan", "Scan a UNC/SMB network path (\\\\server\\share)", false, "Varies"),
    NETWORK_SHARE("Network Share Discovery", "Enumerate and scan all shares on a host", true, "1-6 hours"),
    FILE("Single File Scan", "Scan a single file for PII", false, "Seconds"),
    DATABASE("Database Scan", "Scan database tables and columns", false, "Minutes to hours"),
    REGISTRY("Registry Scan", "Scan Windows registry for stored PII", true, "10-30 minutes"),
    CLOUD_STORAGE("Cloud Storage Scan", "Scan cloud buckets/containers (S3, Azure, GCS)", false, "Varies"),
    MEMORY("Memory Scan", "Scan running process memory for PII", true, "5-30 minutes"),
    EMAIL_STORE("Email Store Scan", "Scan email archives (PST, MBOX, EML)", false, "Hours"),
    CLIPBOARD("Clipboard Monitor", "Monitor clipboard for PII content", false, "Continuous"),
    TEMP_FILES("Temporary Files Scan", "Scan system temp directories", false, "10-30 minutes"),
    RECYCLE_BIN("Recycle Bin Scan", "Scan recycle bin for recoverable PII", false, "5-15 minutes"),
    BROWSER_DATA("Browser Data Scan", "Scan browser caches, history, saved forms", false, "5-15 minutes");

    private final String displayName;
    private final String description;
    private final boolean requiresElevation;
    private final String estimatedDuration;

    ScanTarget(String displayName, String description, boolean requiresElevation, String estimatedDuration) {
        this.displayName = displayName;
        this.description = description;
        this.requiresElevation = requiresElevation;
        this.estimatedDuration = estimatedDuration;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public boolean isRequiresElevation() { return requiresElevation; }
    public String getEstimatedDuration() { return estimatedDuration; }

    /**
     * Check if this target type supports recursive scanning
     */
    public boolean supportsRecursive() {
        return this == SYSTEM || this == DRIVE || this == FOLDER || 
               this == NETWORK_PATH || this == NETWORK_SHARE || this == CLOUD_STORAGE;
    }

    /**
     * Check if this target is a filesystem-based scan
     */
    public boolean isFileSystemBased() {
        return this == SYSTEM || this == DRIVE || this == FOLDER || 
               this == FILE || this == TEMP_FILES || this == RECYCLE_BIN;
    }

    /**
     * Check if this target is network-based
     */
    public boolean isNetworkBased() {
        return this == NETWORK_PATH || this == NETWORK_SHARE || this == CLOUD_STORAGE;
    }
}
