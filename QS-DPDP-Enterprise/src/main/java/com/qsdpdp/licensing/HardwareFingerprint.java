package com.qsdpdp.licensing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hardware Fingerprint Generator
 * Collects machine identity attributes and produces a unique SHA-256 fingerprint.
 *
 * Components:
 * - CPU ID / processor identifier
 * - Primary MAC address
 * - Hostname
 * - OS name + arch
 * - Docker machine-id (if running in container)
 *
 * The fingerprint is deterministic — same machine always produces the same hash.
 *
 * @version 1.0.0
 * @since Enterprise Deployment
 */
public class HardwareFingerprint {

    private static final Logger logger = LoggerFactory.getLogger(HardwareFingerprint.class);

    private static String cachedFingerprint;

    /**
     * Generate the hardware fingerprint for the current machine.
     * Result is cached after first computation.
     */
    public static String generate() {
        if (cachedFingerprint != null) {
            return cachedFingerprint;
        }

        StringBuilder sb = new StringBuilder();

        // 1. Hostname
        String hostname = getHostname();
        sb.append("HOST:").append(hostname).append("|");

        // 2. OS Info
        String os = System.getProperty("os.name", "unknown") + "-" + System.getProperty("os.arch", "unknown");
        sb.append("OS:").append(os).append("|");

        // 3. Primary MAC Address
        String mac = getPrimaryMacAddress();
        sb.append("MAC:").append(mac).append("|");

        // 4. CPU Identifier
        String cpu = getCpuIdentifier();
        sb.append("CPU:").append(cpu).append("|");

        // 5. Machine ID (Linux/Docker)
        String machineId = getMachineId();
        sb.append("MID:").append(machineId).append("|");

        // 6. Username (additional entropy)
        sb.append("USER:").append(System.getProperty("user.name", "unknown"));

        // Hash everything with SHA-256
        cachedFingerprint = sha256(sb.toString());
        logger.info("Hardware fingerprint generated: {}...{}", 
                cachedFingerprint.substring(0, 8), 
                cachedFingerprint.substring(cachedFingerprint.length() - 8));

        return cachedFingerprint;
    }

    /**
     * Get a human-readable summary of hardware components (for display in admin UI).
     */
    public static Map<String, String> getComponents() {
        Map<String, String> components = new LinkedHashMap<>();
        components.put("hostname", getHostname());
        components.put("os", System.getProperty("os.name", "unknown"));
        components.put("arch", System.getProperty("os.arch", "unknown"));
        components.put("macAddress", getPrimaryMacAddress());
        components.put("cpuIdentifier", getCpuIdentifier());
        components.put("machineId", getMachineId());
        components.put("username", System.getProperty("user.name", "unknown"));
        components.put("fingerprint", generate());
        return components;
    }

    // ═══════════════════════════════════════════════════════════
    // COMPONENT COLLECTORS
    // ═══════════════════════════════════════════════════════════

    private static String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            // Fallback: environment variable
            String h = System.getenv("COMPUTERNAME"); // Windows
            if (h == null) h = System.getenv("HOSTNAME"); // Linux
            return h != null ? h : "unknown-host";
        }
    }

    private static String getPrimaryMacAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<String> macs = new ArrayList<>();

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) continue;

                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length == 6) {
                    String macStr = String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                            mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
                    macs.add(macStr);
                }
            }

            // Sort for determinism, take first
            Collections.sort(macs);
            return macs.isEmpty() ? "00:00:00:00:00:00" : macs.get(0);

        } catch (Exception e) {
            logger.debug("Could not read MAC address: {}", e.getMessage());
            return "00:00:00:00:00:00";
        }
    }

    private static String getCpuIdentifier() {
        String osName = System.getProperty("os.name", "").toLowerCase();

        try {
            if (osName.contains("win")) {
                // Windows: WMIC
                ProcessBuilder pb = new ProcessBuilder("wmic", "cpu", "get", "ProcessorId");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    return br.lines()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty() && !line.equalsIgnoreCase("ProcessorId"))
                            .findFirst()
                            .orElse("unknown-cpu");
                }
            } else {
                // Linux: /proc/cpuinfo
                Path cpuInfo = Path.of("/proc/cpuinfo");
                if (Files.exists(cpuInfo)) {
                    return Files.readAllLines(cpuInfo).stream()
                            .filter(line -> line.startsWith("model name"))
                            .findFirst()
                            .map(line -> line.split(":")[1].trim())
                            .orElse(System.getProperty("os.arch", "unknown-cpu"));
                }
            }
        } catch (Exception e) {
            logger.debug("Could not read CPU ID: {}", e.getMessage());
        }

        return System.getProperty("os.arch", "unknown-cpu") + "-" + 
               Runtime.getRuntime().availableProcessors() + "cores";
    }

    private static String getMachineId() {
        // Try Linux machine-id (also works in Docker if volume mounted)
        try {
            Path machineId = Path.of("/etc/machine-id");
            if (Files.exists(machineId)) {
                return Files.readString(machineId).trim();
            }

            // macOS
            Path dbusId = Path.of("/var/lib/dbus/machine-id");
            if (Files.exists(dbusId)) {
                return Files.readString(dbusId).trim();
            }
        } catch (Exception e) {
            logger.debug("Could not read machine-id: {}", e.getMessage());
        }

        // Windows: use Computer SID or registry
        try {
            String osName = System.getProperty("os.name", "").toLowerCase();
            if (osName.contains("win")) {
                ProcessBuilder pb = new ProcessBuilder("wmic", "csproduct", "get", "UUID");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    return br.lines()
                            .map(String::trim)
                            .filter(line -> !line.isEmpty() && !line.equalsIgnoreCase("UUID"))
                            .findFirst()
                            .orElse("no-machine-id");
                }
            }
        } catch (Exception e) {
            logger.debug("Could not read Windows UUID: {}", e.getMessage());
        }

        return "no-machine-id";
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Reset cached fingerprint (for testing).
     */
    public static void resetCache() {
        cachedFingerprint = null;
    }
}
