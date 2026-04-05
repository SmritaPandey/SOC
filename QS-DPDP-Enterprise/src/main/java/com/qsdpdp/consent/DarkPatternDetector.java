package com.qsdpdp.consent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Dark Pattern Detector — DPDP Act S.18 Compliance
 * 
 * Detects manipulative UI/UX patterns in consent flows:
 * - Pre-checked consent boxes
 * - Misleading button labels
 * - Hidden options / forced bundling
 * - Urgency/scarcity pressure
 * - Confusing double negatives
 * - Asymmetric choice design
 * - Privacy Zuckering (oversharing nudges)
 * 
 * DPDP Act S.18: "No person shall engage in any practice
 * to gain consent through misleading or deceptive design."
 * 
 * @version 1.0.0
 * @since Phase 1 — Consent Enhancement
 */
@Service
public class DarkPatternDetector {

    private static final Logger logger = LoggerFactory.getLogger(DarkPatternDetector.class);

    // Pattern categories with detection rules
    private static final Map<String, List<DarkPatternRule>> RULES = new LinkedHashMap<>();

    static {
        // 1. Pre-ticked checkboxes
        RULES.put("PRESELECTION", List.of(
            new DarkPatternRule("PRE-001", "Pre-checked consent checkbox",
                "Consent checkbox must NOT be pre-selected (DPDP S.6(1))",
                Pattern.compile("(?i)(checked|selected|default[=:]true|opt.?in.?default)", Pattern.CASE_INSENSITIVE),
                "CRITICAL"),
            new DarkPatternRule("PRE-002", "Auto-enrollment without consent",
                "User must not be auto-enrolled in data sharing",
                Pattern.compile("(?i)(auto.?enroll|auto.?subscribe|auto.?opt)", Pattern.CASE_INSENSITIVE),
                "CRITICAL")
        ));

        // 2. Misleading labels
        RULES.put("MISDIRECTION", List.of(
            new DarkPatternRule("MIS-001", "Confusing double negative",
                "Consent text must not use double negatives",
                Pattern.compile("(?i)(don'?t\\s+not|not\\s+un|never\\s+not|isn'?t\\s+not)", Pattern.CASE_INSENSITIVE),
                "HIGH"),
            new DarkPatternRule("MIS-002", "Accept-only design",
                "Must provide equal prominence to Accept and Reject",
                Pattern.compile("(?i)(accept.?only|no.?reject|must.?agree|forced.?consent)", Pattern.CASE_INSENSITIVE),
                "CRITICAL"),
            new DarkPatternRule("MIS-003", "Emotional manipulation",
                "Must not use guilt or shame to influence consent",
                Pattern.compile("(?i)(you.?will.?miss|don'?t.?miss|you'?re.?sure|are.?you.?really)", Pattern.CASE_INSENSITIVE),
                "HIGH")
        ));

        // 3. Hidden options
        RULES.put("HIDDEN_OPTIONS", List.of(
            new DarkPatternRule("HID-001", "Buried opt-out",
                "Opt-out must be equally prominent as opt-in",
                Pattern.compile("(?i)(hidden|collapsed|tiny.?text|small.?print|buried)", Pattern.CASE_INSENSITIVE),
                "HIGH"),
            new DarkPatternRule("HID-002", "Forced bundling",
                "Cannot bundle unrelated consents together",
                Pattern.compile("(?i)(bundle|all.?or.?nothing|package.?deal|combined.?consent)", Pattern.CASE_INSENSITIVE),
                "CRITICAL")
        ));

        // 4. Urgency/scarcity
        RULES.put("URGENCY", List.of(
            new DarkPatternRule("URG-001", "False urgency",
                "Must not create artificial time pressure for consent",
                Pattern.compile("(?i)(limited.?time|hurry|act.?now|expire.?soon|last.?chance|countdown)", Pattern.CASE_INSENSITIVE),
                "MEDIUM"),
            new DarkPatternRule("URG-002", "False scarcity",
                "Must not imply limited availability to force consent",
                Pattern.compile("(?i)(only.?\\d+.?left|running.?out|almost.?gone|exclusive.?offer)", Pattern.CASE_INSENSITIVE),
                "MEDIUM")
        ));

        // 5. Privacy Zuckering
        RULES.put("OVERSHARING", List.of(
            new DarkPatternRule("OVR-001", "Excessive data request",
                "Requesting more data than necessary for purpose",
                Pattern.compile("(?i)(share.?everything|full.?access|all.?data|complete.?profile)", Pattern.CASE_INSENSITIVE),
                "HIGH"),
            new DarkPatternRule("OVR-002", "Social pressure",
                "Must not use social proof to pressure consent",
                Pattern.compile("(?i)(everyone.?shares|\\d+.?users.?agreed|most.?people|friends.?shared)", Pattern.CASE_INSENSITIVE),
                "MEDIUM")
        ));

        // 6. Obstruction
        RULES.put("OBSTRUCTION", List.of(
            new DarkPatternRule("OBS-001", "Complex withdrawal",
                "Consent withdrawal must be as easy as granting (DPDP S.6(4))",
                Pattern.compile("(?i)(call.?us|write.?letter|visit.?office|contact.?support.?to.?cancel)", Pattern.CASE_INSENSITIVE),
                "CRITICAL"),
            new DarkPatternRule("OBS-002", "Confirmshaming",
                "Must not shame users for declining consent",
                Pattern.compile("(?i)(no.?thanks.?I.?don'?t|I.?don'?t.?care|not.?interested.?in.?saving)", Pattern.CASE_INSENSITIVE),
                "HIGH")
        ));
    }

    /**
     * Scan consent text/HTML for dark patterns
     */
    public Map<String, Object> scanConsentFlow(String consentText, String consentHTML,
            Map<String, Object> uiMetadata) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> violations = new ArrayList<>();
        int totalChecks = 0;
        int totalViolations = 0;

        // Scan text content
        String combined = (consentText != null ? consentText : "") + " " +
                (consentHTML != null ? consentHTML : "");

        for (Map.Entry<String, List<DarkPatternRule>> entry : RULES.entrySet()) {
            for (DarkPatternRule rule : entry.getValue()) {
                totalChecks++;
                if (rule.pattern.matcher(combined).find()) {
                    totalViolations++;
                    violations.add(Map.of(
                            "ruleId", rule.id,
                            "category", entry.getKey(),
                            "name", rule.name,
                            "explanation", rule.explanation,
                            "severity", rule.severity,
                            "section", "DPDP Act S.18"
                    ));
                }
            }
        }

        // UI metadata checks
        if (uiMetadata != null) {
            totalChecks += 3;
            // Check accept/reject button size asymmetry
            if (uiMetadata.containsKey("acceptButtonSize") && uiMetadata.containsKey("rejectButtonSize")) {
                double acceptSize = ((Number) uiMetadata.get("acceptButtonSize")).doubleValue();
                double rejectSize = ((Number) uiMetadata.get("rejectButtonSize")).doubleValue();
                if (acceptSize > rejectSize * 1.5) {
                    totalViolations++;
                    violations.add(Map.of("ruleId", "UI-001", "category", "ASYMMETRIC_DESIGN",
                            "name", "Asymmetric button sizing",
                            "explanation", "Accept button is >1.5x larger than reject button",
                            "severity", "HIGH", "section", "DPDP Act S.18"));
                }
            }
            // Check color manipulation
            if (Boolean.TRUE.equals(uiMetadata.get("rejectButtonGrayed"))) {
                totalViolations++;
                violations.add(Map.of("ruleId", "UI-002", "category", "MISDIRECTION",
                        "name", "Grayed-out reject button",
                        "explanation", "Reject option must not be de-emphasized via color",
                        "severity", "HIGH", "section", "DPDP Act S.18"));
            }
            // Check number of clicks to reject
            if (uiMetadata.containsKey("clicksToReject")) {
                int clicks = ((Number) uiMetadata.get("clicksToReject")).intValue();
                if (clicks > 2) {
                    totalViolations++;
                    violations.add(Map.of("ruleId", "UI-003", "category", "OBSTRUCTION",
                            "name", "Excessive steps to reject",
                            "explanation", "Rejecting consent requires " + clicks + " clicks (max 2 allowed)",
                            "severity", "CRITICAL", "section", "DPDP Act S.6(4)"));
                }
            }
        }

        result.put("totalChecks", totalChecks);
        result.put("violations", violations);
        result.put("violationCount", totalViolations);
        result.put("complianceScore", totalChecks > 0 ? Math.round(((double)(totalChecks - totalViolations) / totalChecks) * 100) : 100);
        result.put("status", totalViolations == 0 ? "COMPLIANT" : "NON_COMPLIANT");
        result.put("scannedAt", LocalDateTime.now().toString());
        return result;
    }

    /**
     * Get all dark pattern rules
     */
    public Map<String, Object> getRules() {
        Map<String, Object> rules = new LinkedHashMap<>();
        int total = 0;
        for (Map.Entry<String, List<DarkPatternRule>> entry : RULES.entrySet()) {
            List<Map<String, String>> ruleList = new ArrayList<>();
            for (DarkPatternRule r : entry.getValue()) {
                ruleList.add(Map.of("id", r.id, "name", r.name, "explanation", r.explanation, "severity", r.severity));
                total++;
            }
            rules.put(entry.getKey(), ruleList);
        }
        rules.put("totalRules", total);
        return rules;
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL RULE DTO
    // ═══════════════════════════════════════════════════════════

    private static class DarkPatternRule {
        final String id, name, explanation, severity;
        final Pattern pattern;
        DarkPatternRule(String id, String name, String explanation, Pattern pattern, String severity) {
            this.id = id; this.name = name; this.explanation = explanation;
            this.pattern = pattern; this.severity = severity;
        }
    }
}
