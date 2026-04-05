package com.qsdpdp.pii;

import java.util.*;

/**
 * Scan Profile - Configurable scan configuration
 * Supports predefined profiles (Quick, Full, Custom, Scheduled)
 * with granular control over scan scope and behavior
 * 
 * @version 2.0.0
 * @since Phase 7
 */
public class ScanProfile {

    private String id;
    private String name;
    private String description;
    private ScanTarget targetType;
    private String targetPath; // Drive letter, folder path, UNC path, etc.
    private boolean recursive;
    private int maxDepth;
    private long maxFileSizeBytes;
    private Set<String> includeExtensions;
    private Set<String> excludeExtensions;
    private Set<String> excludePaths;
    private Set<PIIType> targetPIITypes; // null = all types
    private double minimumConfidence;
    private boolean scanArchives; // ZIP, RAR, 7z
    private boolean scanHiddenFiles;
    private boolean scanSystemFiles;
    private boolean followSymlinks;
    private int threadCount;
    private ScanSchedule schedule;
    private ProfileType profileType;

    public enum ProfileType {
        QUICK_SCAN, FULL_SYSTEM_SCAN, CUSTOM_SCAN, SCHEDULED_SCAN
    }

    public ScanProfile() {
        this.id = UUID.randomUUID().toString();
        this.recursive = true;
        this.maxDepth = Integer.MAX_VALUE;
        this.maxFileSizeBytes = 100 * 1024 * 1024; // 100MB
        this.includeExtensions = new HashSet<>();
        this.excludeExtensions = new HashSet<>();
        this.excludePaths = new HashSet<>();
        this.minimumConfidence = 0.5;
        this.scanArchives = false;
        this.scanHiddenFiles = false;
        this.scanSystemFiles = false;
        this.followSymlinks = false;
        this.threadCount = Runtime.getRuntime().availableProcessors();
        this.profileType = ProfileType.CUSTOM_SCAN;
    }

    // ═══════════════════════════════════════════════════════════
    // PREDEFINED PROFILES
    // ═══════════════════════════════════════════════════════════

    /**
     * Quick Scan: user folders, common doc types, limited depth
     */
    public static ScanProfile quickScan() {
        ScanProfile profile = new ScanProfile();
        profile.setName("Quick Scan");
        profile.setDescription("Scan user documents, desktop, downloads for PII in common file types");
        profile.setProfileType(ProfileType.QUICK_SCAN);
        profile.setTargetType(ScanTarget.FOLDER);
        profile.setTargetPath(System.getProperty("user.home"));
        profile.setMaxDepth(5);
        profile.setMaxFileSizeBytes(50 * 1024 * 1024);
        profile.getIncludeExtensions().addAll(Set.of(
            ".txt", ".csv", ".json", ".xml", ".xlsx", ".xls", ".doc", ".docx",
            ".pdf", ".html", ".log", ".sql", ".md", ".yml", ".yaml"
        ));
        profile.getExcludeExtensions().addAll(Set.of(
            ".exe", ".dll", ".sys", ".bin", ".iso", ".img", ".mp3", ".mp4",
            ".avi", ".mkv", ".jpg", ".jpeg", ".png", ".gif", ".bmp"
        ));
        profile.setThreadCount(Math.min(4, Runtime.getRuntime().availableProcessors()));
        return profile;
    }

    /**
     * Full System Scan: all drives, all file types, maximum depth
     */
    public static ScanProfile fullSystemScan() {
        ScanProfile profile = new ScanProfile();
        profile.setName("Full System Scan");
        profile.setDescription("Complete scan of all local drives for PII in all scannable files");
        profile.setProfileType(ProfileType.FULL_SYSTEM_SCAN);
        profile.setTargetType(ScanTarget.SYSTEM);
        profile.setMaxDepth(Integer.MAX_VALUE);
        profile.setMaxFileSizeBytes(200 * 1024 * 1024);
        profile.setScanHiddenFiles(true);
        profile.setScanArchives(true);
        profile.getExcludeExtensions().addAll(Set.of(
            ".exe", ".dll", ".sys", ".bin", ".iso", ".img"
        ));
        profile.getExcludePaths().addAll(Set.of(
            "Windows", "Program Files", "Program Files (x86)", 
            "$Recycle.Bin", "System Volume Information"
        ));
        profile.setThreadCount(Runtime.getRuntime().availableProcessors());
        return profile;
    }

    /**
     * Network Scan: scan a UNC path
     */
    public static ScanProfile networkScan(String uncPath) {
        ScanProfile profile = new ScanProfile();
        profile.setName("Network Path Scan");
        profile.setDescription("Scan network share at: " + uncPath);
        profile.setProfileType(ProfileType.CUSTOM_SCAN);
        profile.setTargetType(ScanTarget.NETWORK_PATH);
        profile.setTargetPath(uncPath);
        profile.setMaxDepth(10);
        profile.setMaxFileSizeBytes(100 * 1024 * 1024);
        profile.setFollowSymlinks(false);
        return profile;
    }

    /**
     * Drive Scan: scan a specific drive
     */
    public static ScanProfile driveScan(String driveLetter) {
        ScanProfile profile = new ScanProfile();
        profile.setName("Drive Scan: " + driveLetter);
        profile.setDescription("Complete scan of drive " + driveLetter);
        profile.setProfileType(ProfileType.CUSTOM_SCAN);
        profile.setTargetType(ScanTarget.DRIVE);
        profile.setTargetPath(driveLetter);
        profile.setMaxDepth(Integer.MAX_VALUE);
        profile.setScanHiddenFiles(true);
        profile.getExcludePaths().addAll(Set.of(
            "$Recycle.Bin", "System Volume Information"
        ));
        return profile;
    }

    /**
     * Scheduled Scan: wraps any profile with a schedule
     */
    public static ScanProfile scheduledScan(ScanProfile base, ScanSchedule schedule) {
        base.setProfileType(ProfileType.SCHEDULED_SCAN);
        base.setSchedule(schedule);
        base.setName("Scheduled: " + base.getName());
        return base;
    }

    /**
     * Get all predefined profile templates (for UI listing)
     */
    public static List<ScanProfile> getProfileTemplates() {
        return List.of(
            quickScan(),
            fullSystemScan(),
            driveScan("C:\\"),
            networkScan("\\\\server\\share")
        );
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ScanTarget getTargetType() { return targetType; }
    public void setTargetType(ScanTarget targetType) { this.targetType = targetType; }

    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }

    public boolean isRecursive() { return recursive; }
    public void setRecursive(boolean recursive) { this.recursive = recursive; }

    public int getMaxDepth() { return maxDepth; }
    public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }

    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public void setMaxFileSizeBytes(long maxFileSizeBytes) { this.maxFileSizeBytes = maxFileSizeBytes; }

    public Set<String> getIncludeExtensions() { return includeExtensions; }
    public Set<String> getExcludeExtensions() { return excludeExtensions; }
    public Set<String> getExcludePaths() { return excludePaths; }

    public Set<PIIType> getTargetPIITypes() { return targetPIITypes; }
    public void setTargetPIITypes(Set<PIIType> targetPIITypes) { this.targetPIITypes = targetPIITypes; }

    public double getMinimumConfidence() { return minimumConfidence; }
    public void setMinimumConfidence(double minimumConfidence) { this.minimumConfidence = minimumConfidence; }

    public boolean isScanArchives() { return scanArchives; }
    public void setScanArchives(boolean scanArchives) { this.scanArchives = scanArchives; }

    public boolean isScanHiddenFiles() { return scanHiddenFiles; }
    public void setScanHiddenFiles(boolean scanHiddenFiles) { this.scanHiddenFiles = scanHiddenFiles; }

    public boolean isScanSystemFiles() { return scanSystemFiles; }
    public void setScanSystemFiles(boolean scanSystemFiles) { this.scanSystemFiles = scanSystemFiles; }

    public boolean isFollowSymlinks() { return followSymlinks; }
    public void setFollowSymlinks(boolean followSymlinks) { this.followSymlinks = followSymlinks; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public ScanSchedule getSchedule() { return schedule; }
    public void setSchedule(ScanSchedule schedule) { this.schedule = schedule; }

    public ProfileType getProfileType() { return profileType; }
    public void setProfileType(ProfileType profileType) { this.profileType = profileType; }
}
