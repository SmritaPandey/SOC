package com.qsdpdp.pii;

/**
 * Scan Progress Listener - Callback interface for scan progress
 * Provides real-time updates during PII scanning operations
 * 
 * @version 2.0.0
 * @since Phase 7
 */
public interface ScanProgressListener {

    /**
     * Called periodically during scan with progress information
     */
    void onProgress(int percentComplete, long filesScanned, long totalFiles, String currentFile);

    /**
     * Called when a PII finding is detected during scan
     */
    void onFindingDetected(PIIFinding finding);

    /**
     * Called when a file cannot be scanned due to an error
     */
    void onError(String file, Exception error);

    /**
     * Called when the scan is fully complete
     */
    void onComplete(PIIScanResult result);

    /**
     * Called when scan is starting
     */
    default void onScanStarted(ScanProfile profile) {}

    /**
     * Called when scan is cancelled
     */
    default void onScanCancelled(String scanId, String reason) {}

    /**
     * No-op listener for silent scanning
     */
    static ScanProgressListener silent() {
        return new ScanProgressListener() {
            @Override public void onProgress(int p, long s, long t, String f) {}
            @Override public void onFindingDetected(PIIFinding finding) {}
            @Override public void onError(String file, Exception error) {}
            @Override public void onComplete(PIIScanResult result) {}
        };
    }

    /**
     * Logging listener that prints progress to console
     */
    static ScanProgressListener logging() {
        return new ScanProgressListener() {
            @Override
            public void onProgress(int percentComplete, long filesScanned, long totalFiles, String currentFile) {
                System.out.printf("[%d%%] Scanned %d/%d files - %s%n", percentComplete, filesScanned, totalFiles, currentFile);
            }

            @Override
            public void onFindingDetected(PIIFinding finding) {
                System.out.printf("  ⚠ Found %s in %s (confidence: %.2f)%n",
                    finding.getType(), finding.getSourcePath(), finding.getConfidence());
            }

            @Override
            public void onError(String file, Exception error) {
                System.err.printf("  ✗ Error scanning %s: %s%n", file, error.getMessage());
            }

            @Override
            public void onComplete(PIIScanResult result) {
                System.out.printf("✓ Scan complete: %d findings in %d files%n",
                    result.getTotalFindings(), result.getFilesScanned());
            }
        };
    }
}
