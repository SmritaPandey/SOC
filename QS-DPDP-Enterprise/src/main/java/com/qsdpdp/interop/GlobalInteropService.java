package com.qsdpdp.interop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Global Interoperability Service
 * Maps between DPDP Act (India), GDPR (EU), OECD Privacy Principles, and ISO 27701.
 *
 * Enables:
 * - Cross-border consent validation
 * - Regulatory framework mapping
 * - Standardized consent artifact generation
 *
 * @version 1.0.0
 * @since Universal Trust OS v3.0
 */
@Service
public class GlobalInteropService {

    private static final Logger logger = LoggerFactory.getLogger(GlobalInteropService.class);

    // DPDP → GDPR mapping matrix
    private static final Map<String, Map<String, String>> FRAMEWORK_MAP = Map.ofEntries(
            Map.entry("CONSENT", Map.of(
                    "dpdp", "S.6 — Consent of Data Principal",
                    "gdpr", "Art.6(1)(a), Art.7 — Lawful basis: Consent",
                    "oecd", "Collection Limitation Principle",
                    "iso27701", "A.7.2.3 — Determining lawful basis")),
            Map.entry("PURPOSE_LIMITATION", Map.of(
                    "dpdp", "S.6(1) — Specific, clear, and lawful purpose",
                    "gdpr", "Art.5(1)(b) — Purpose limitation",
                    "oecd", "Purpose Specification Principle",
                    "iso27701", "A.7.2.1 — Purpose identification")),
            Map.entry("DATA_MINIMIZATION", Map.of(
                    "dpdp", "S.6(1) — Limited to necessary data",
                    "gdpr", "Art.5(1)(c) — Data minimisation",
                    "oecd", "Collection Limitation Principle",
                    "iso27701", "A.7.4.1 — Collection limitation")),
            Map.entry("ACCURACY", Map.of(
                    "dpdp", "S.8(3) — Ensure completeness and accuracy",
                    "gdpr", "Art.5(1)(d) — Accuracy",
                    "oecd", "Data Quality Principle",
                    "iso27701", "A.7.4.3 — PII accuracy and quality")),
            Map.entry("STORAGE_LIMITATION", Map.of(
                    "dpdp", "S.8(7) — Erase when purpose fulfilled",
                    "gdpr", "Art.5(1)(e) — Storage limitation",
                    "oecd", "Use Limitation Principle",
                    "iso27701", "A.7.4.7 — PII retention")),
            Map.entry("SECURITY", Map.of(
                    "dpdp", "S.8(4) — Reasonable security safeguards",
                    "gdpr", "Art.5(1)(f), Art.32 — Integrity and confidentiality",
                    "oecd", "Security Safeguards Principle",
                    "iso27701", "A.7.4.5 — PII protection")),
            Map.entry("ACCOUNTABILITY", Map.of(
                    "dpdp", "S.8 — Obligations of Data Fiduciary",
                    "gdpr", "Art.5(2), Art.24 — Accountability",
                    "oecd", "Accountability Principle",
                    "iso27701", "A.7.2.8 — Records of processing")),
            Map.entry("DATA_SUBJECT_RIGHTS", Map.of(
                    "dpdp", "S.11-13 — Rights of Data Principal",
                    "gdpr", "Art.12-23 — Rights of data subject",
                    "oecd", "Individual Participation Principle",
                    "iso27701", "A.7.3 — PII principal obligations")),
            Map.entry("BREACH_NOTIFICATION", Map.of(
                    "dpdp", "S.8(6) — Notify DPBI of breach",
                    "gdpr", "Art.33-34 — Notification of breach",
                    "oecd", "Openness Principle",
                    "iso27701", "A.7.3.7 — PII breach notification")),
            Map.entry("DPO", Map.of(
                    "dpdp", "S.10(2) — DPO for Significant Data Fiduciary",
                    "gdpr", "Art.37-39 — Data Protection Officer",
                    "oecd", "N/A (organizational measure)",
                    "iso27701", "A.7.2.5 — Privacy officer")),
            Map.entry("DPIA", Map.of(
                    "dpdp", "S.10 — DPIA for Significant Data Fiduciary",
                    "gdpr", "Art.35 — Data Protection Impact Assessment",
                    "oecd", "N/A (risk-based measure)",
                    "iso27701", "A.7.2.5 — Privacy impact assessment")),
            Map.entry("CROSS_BORDER", Map.of(
                    "dpdp", "S.16 — Transfer outside India (notified countries)",
                    "gdpr", "Art.44-49 — Transfer to third countries (adequacy/SCCs)",
                    "oecd", "Transborder Data Flows Principle",
                    "iso27701", "A.7.5 — PII sharing, transfer"))
    );

    public Map<String, Object> validateCrossBorder(String consentId, String sourceJurisdiction,
                                                     String targetJurisdiction) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transactionId", "INTEROP-" + UUID.randomUUID().toString().substring(0, 8));
        result.put("consentId", consentId);
        result.put("sourceJurisdiction", sourceJurisdiction);
        result.put("targetJurisdiction", targetJurisdiction);

        boolean adequacy = isAdequacyRecognized(sourceJurisdiction, targetJurisdiction);
        result.put("adequacyDecision", adequacy);
        result.put("transferAllowed", adequacy || hasAlternativeMechanism(targetJurisdiction));
        result.put("legalBasis", getLegalBasis(sourceJurisdiction, targetJurisdiction));
        result.put("additionalSafeguards", getRequiredSafeguards(sourceJurisdiction, targetJurisdiction));
        result.put("riskLevel", adequacy ? "LOW" : "HIGH");
        result.put("timestamp", Instant.now().toString());
        return result;
    }

    public Map<String, Object> getGDPRMapping(String consentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("consentId", consentId);
        result.put("mappingStandard", "DPDP Act 2023 ↔ GDPR (EU) 2016/679");
        result.put("equivalenceMatrix", FRAMEWORK_MAP);
        result.put("overallEquivalence", "SUBSTANTIALLY_EQUIVALENT");
        result.put("keyDifferences", List.of(
                "DPDP has no 'legitimate interest' basis — consent or deemed consent only",
                "DPDP penalty structure differs (fixed maxima vs GDPR % turnover)",
                "DPDP covers digital data only; GDPR covers all personal data",
                "DPDP S.16 restricts transfer to notified countries; GDPR uses adequacy + SCCs"
        ));
        return result;
    }

    public Map<String, Object> getSupportedFrameworks() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("frameworks", List.of(
                Map.of("name", "DPDP Act 2023", "jurisdiction", "India", "type", "National Law", "status", "ACTIVE"),
                Map.of("name", "GDPR 2016/679", "jurisdiction", "European Union", "type", "Regulation", "status", "ACTIVE"),
                Map.of("name", "OECD Privacy Principles", "jurisdiction", "International", "type", "Guidelines", "status", "ACTIVE"),
                Map.of("name", "ISO 27701:2019", "jurisdiction", "International", "type", "Standard", "status", "ACTIVE"),
                Map.of("name", "ISO 27001:2022", "jurisdiction", "International", "type", "Standard", "status", "ACTIVE"),
                Map.of("name", "NIST Privacy Framework", "jurisdiction", "USA", "type", "Framework", "status", "ACTIVE"),
                Map.of("name", "APEC CBPR", "jurisdiction", "Asia-Pacific", "type", "Framework", "status", "ACTIVE"),
                Map.of("name", "LGPD", "jurisdiction", "Brazil", "type", "National Law", "status", "MAPPED"),
                Map.of("name", "POPIA", "jurisdiction", "South Africa", "type", "National Law", "status", "MAPPED"),
                Map.of("name", "PDPA", "jurisdiction", "Singapore/Thailand", "type", "National Law", "status", "MAPPED")
        ));
        result.put("totalMappings", FRAMEWORK_MAP.size());
        result.put("interoperabilityLevel", "FULL — All consent artifacts translatable across frameworks");
        return result;
    }

    // ── Helpers ──

    private boolean isAdequacyRecognized(String source, String target) {
        Set<String> indiaAdequate = Set.of("EU", "UK", "JAPAN", "SINGAPORE", "CANADA", "SOUTH_KOREA");
        Set<String> gdprAdequate = Set.of("INDIA", "JAPAN", "CANADA", "NEW_ZEALAND", "ARGENTINA", "SOUTH_KOREA", "UK");
        if ("INDIA".equalsIgnoreCase(source)) return indiaAdequate.contains(target.toUpperCase());
        if ("EU".equalsIgnoreCase(source)) return gdprAdequate.contains(target.toUpperCase());
        return false;
    }

    private boolean hasAlternativeMechanism(String target) {
        return Set.of("USA", "AUSTRALIA", "BRAZIL", "SINGAPORE").contains(target.toUpperCase());
    }

    private String getLegalBasis(String source, String target) {
        if ("INDIA".equalsIgnoreCase(source)) return "DPDP S.16 — Transfer to govt-notified countries only";
        if ("EU".equalsIgnoreCase(source)) return "GDPR Art.46 — Standard Contractual Clauses (SCCs)";
        return "Bilateral agreement / Contractual necessity";
    }

    private List<String> getRequiredSafeguards(String source, String target) {
        List<String> safeguards = new ArrayList<>();
        safeguards.add("Data Processing Agreement (DPA) required");
        safeguards.add("End-to-end encryption mandatory for transit");
        if (!isAdequacyRecognized(source, target)) {
            safeguards.add("Transfer Impact Assessment (TIA) required");
            safeguards.add("Supplementary security measures per EDPB Rec. 01/2020");
        }
        safeguards.add("Data localization check for restricted categories");
        return safeguards;
    }
}
