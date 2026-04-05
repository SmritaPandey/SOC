package com.qsdpdp.siem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Syslog + CEF (Common Event Format) Protocol Parser
 * Parses raw syslog messages and ArcSight CEF formatted events
 * into SecurityEvent objects for SIEM ingestion.
 *
 * Supported Formats:
 * - RFC 3164 BSD Syslog
 * - RFC 5424 IETF Syslog
 * - ArcSight CEF (Common Event Format)
 * - Splunk-style key=value
 *
 * @version 3.0.0
 * @since Phase 3
 */
@Component
public class SyslogCEFParser {

    private static final Logger logger = LoggerFactory.getLogger(SyslogCEFParser.class);

    // RFC 3164: <priority>timestamp hostname app[pid]: message
    private static final Pattern SYSLOG_BSD = Pattern.compile(
            "^<(\\d{1,3})>\\s*(\\w{3}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(\\S+)\\s+(\\S+?)(?:\\[(\\d+)])?:\\s+(.*)$"
    );

    // RFC 5424: <priority>version timestamp hostname app-name procid msgid structured-data msg
    private static final Pattern SYSLOG_5424 = Pattern.compile(
            "^<(\\d{1,3})>(\\d)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\[.*?]|-)\\s+(.*)$"
    );

    // CEF: CEF:version|vendor|product|version|signatureId|name|severity|extensions
    private static final Pattern CEF_HEADER = Pattern.compile(
            "^CEF:(\\d+)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|([^|]*)\\|(.*)$"
    );

    // Key=value pattern for CEF extensions
    private static final Pattern KV_PAIR = Pattern.compile(
            "(\\w+)=((?:[^\\s=]|\\\\=)*(?:\\s+(?!\\w+=)(?:[^\\s=]|\\\\=)*)*)"
    );

    private long parsedCount = 0;
    private long failedCount = 0;

    /**
     * Auto-detect format and parse a raw log line into a SecurityEvent.
     */
    public SecurityEvent parse(String rawLog) {
        if (rawLog == null || rawLog.isBlank()) return null;

        rawLog = rawLog.trim();

        try {
            SecurityEvent event;

            if (rawLog.startsWith("CEF:")) {
                event = parseCEF(rawLog);
            } else if (rawLog.matches("^<\\d{1,3}>\\d\\s+.*")) {
                event = parseSyslog5424(rawLog);
            } else if (rawLog.matches("^<\\d{1,3}>.*")) {
                event = parseSyslogBSD(rawLog);
            } else {
                event = parseKeyValue(rawLog);
            }

            if (event != null) {
                event.setRawLog(rawLog);
                parsedCount++;
            }
            return event;

        } catch (Exception e) {
            failedCount++;
            logger.debug("Failed to parse log line: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse ArcSight CEF (Common Event Format)
     * Format: CEF:0|Vendor|Product|Version|SignatureID|Name|Severity|Extensions
     */
    public SecurityEvent parseCEF(String raw) {
        Matcher m = CEF_HEADER.matcher(raw);
        if (!m.matches()) return null;

        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        event.setSource(m.group(2) + " " + m.group(3)); // vendor + product

        // Map CEF severity (0-10) to our severity
        int cefSeverity = Integer.parseInt(m.group(7).trim());
        event.setSeverity(mapCEFSeverity(cefSeverity));

        event.setMessage(m.group(6)); // Event name

        // Parse extensions (key=value pairs)
        String extensions = m.group(8);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("cef_vendor", m.group(2));
        metadata.put("cef_product", m.group(3));
        metadata.put("cef_version", m.group(4));
        metadata.put("cef_signature_id", m.group(5));
        metadata.put("cef_name", m.group(6));
        metadata.put("parser", "CEF");

        // Parse extension key-value pairs
        Matcher kvMatcher = KV_PAIR.matcher(extensions);
        while (kvMatcher.find()) {
            String key = kvMatcher.group(1);
            String value = kvMatcher.group(2);

            // Map standard CEF keys
            switch (key) {
                case "src" -> event.setSourceIP(value);
                case "dst" -> event.setDestinationIP(value);
                case "suser" -> event.setUserId(value);
                case "duser" -> metadata.put("destination_user", value);
                case "msg" -> event.setMessage(value);
                case "act" -> event.setAction(value);
                case "cs1" -> metadata.put("custom_string_1", value);
                case "cs2" -> metadata.put("custom_string_2", value);
                case "cn1" -> metadata.put("custom_number_1", value);
                case "fname" -> event.setResource(value);
                default -> metadata.put("cef_" + key, value);
            }
        }

        event.setMetadata(metadata);
        return event;
    }

    /**
     * Parse RFC 3164 BSD Syslog
     * Format: <priority>timestamp hostname application[pid]: message
     */
    public SecurityEvent parseSyslogBSD(String raw) {
        Matcher m = SYSLOG_BSD.matcher(raw);
        if (!m.matches()) return null;

        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID().toString());

        int priority = Integer.parseInt(m.group(1));
        event.setSeverity(mapSyslogSeverity(priority % 8));

        // Parse timestamp (use current year since BSD syslog doesn't include year)
        event.setTimestamp(LocalDateTime.now());
        event.setSource(m.group(4)); // application name
        event.setMessage(m.group(6));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("syslog_hostname", m.group(3));
        metadata.put("syslog_facility", priority / 8);
        metadata.put("syslog_severity", priority % 8);
        metadata.put("syslog_pid", m.group(5));
        metadata.put("parser", "SYSLOG_BSD");
        event.setMetadata(metadata);

        return event;
    }

    /**
     * Parse RFC 5424 IETF Syslog
     * Format: <priority>version timestamp hostname app-name procid msgid sd msg
     */
    public SecurityEvent parseSyslog5424(String raw) {
        Matcher m = SYSLOG_5424.matcher(raw);
        if (!m.matches()) return null;

        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID().toString());

        int priority = Integer.parseInt(m.group(1));
        event.setSeverity(mapSyslogSeverity(priority % 8));

        // Parse ISO 8601 timestamp
        try {
            String ts = m.group(3);
            if (!"-".equals(ts)) {
                event.setTimestamp(LocalDateTime.parse(ts, DateTimeFormatter.ISO_DATE_TIME));
            } else {
                event.setTimestamp(LocalDateTime.now());
            }
        } catch (Exception e) {
            event.setTimestamp(LocalDateTime.now());
        }

        event.setSource(m.group(5)); // app-name
        event.setMessage(m.group(9));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("syslog_hostname", m.group(4));
        metadata.put("syslog_facility", priority / 8);
        metadata.put("syslog_severity", priority % 8);
        metadata.put("syslog_procid", m.group(6));
        metadata.put("syslog_msgid", m.group(7));
        metadata.put("syslog_structured_data", m.group(8));
        metadata.put("parser", "SYSLOG_5424");
        event.setMetadata(metadata);

        return event;
    }

    /**
     * Parse key=value formatted logs (Splunk/generic)
     */
    public SecurityEvent parseKeyValue(String raw) {
        Matcher kvMatcher = KV_PAIR.matcher(raw);
        Map<String, String> pairs = new LinkedHashMap<>();
        while (kvMatcher.find()) {
            pairs.put(kvMatcher.group(1), kvMatcher.group(2));
        }

        if (pairs.isEmpty()) return null;

        SecurityEvent event = new SecurityEvent();
        event.setId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());

        event.setSourceIP(pairs.getOrDefault("src", pairs.get("source_ip")));
        event.setDestinationIP(pairs.getOrDefault("dst", pairs.get("dest_ip")));
        event.setUserId(pairs.getOrDefault("user", pairs.get("userId")));
        event.setAction(pairs.getOrDefault("action", pairs.get("act")));
        event.setMessage(pairs.getOrDefault("msg", pairs.getOrDefault("message", raw)));

        String severity = pairs.getOrDefault("severity", pairs.getOrDefault("level", "MEDIUM"));
        try {
            event.setSeverity(EventSeverity.valueOf(severity.toUpperCase()));
        } catch (Exception e) {
            event.setSeverity(EventSeverity.MEDIUM);
        }

        Map<String, Object> metadata = new HashMap<>(pairs);
        metadata.put("parser", "KEY_VALUE");
        event.setMetadata(metadata);

        return event;
    }

    // ═══ SEVERITY MAPPING ═══

    private EventSeverity mapCEFSeverity(int cefSeverity) {
        if (cefSeverity >= 9) return EventSeverity.CRITICAL;
        if (cefSeverity >= 7) return EventSeverity.HIGH;
        if (cefSeverity >= 4) return EventSeverity.MEDIUM;
        if (cefSeverity >= 1) return EventSeverity.LOW;
        return EventSeverity.INFO;
    }

    private EventSeverity mapSyslogSeverity(int syslogSeverity) {
        return switch (syslogSeverity) {
            case 0, 1 -> EventSeverity.CRITICAL; // Emergency, Alert
            case 2 -> EventSeverity.CRITICAL;     // Critical
            case 3 -> EventSeverity.HIGH;          // Error
            case 4 -> EventSeverity.MEDIUM;        // Warning
            case 5 -> EventSeverity.LOW;           // Notice
            case 6 -> EventSeverity.INFO;          // Informational
            default -> EventSeverity.INFO;         // Debug
        };
    }

    // ═══ STATS ═══
    public long getParsedCount() { return parsedCount; }
    public long getFailedCount() { return failedCount; }
    public Map<String, Object> getStatistics() {
        return Map.of(
                "parsedCount", parsedCount,
                "failedCount", failedCount,
                "supportedFormats", List.of("CEF", "SYSLOG_BSD", "SYSLOG_5424", "KEY_VALUE"));
    }
}
