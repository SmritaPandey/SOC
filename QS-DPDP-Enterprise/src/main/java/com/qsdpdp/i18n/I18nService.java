package com.qsdpdp.i18n;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internationalization (i18n) Service for QS-DPDP Enterprise
 * Supports all 22 Indian Scheduled Languages (Eighth Schedule) + English
 * Provides translations for UI labels, DPDP Act terms, consent notices,
 * and module-specific text.
 *
 * @version 1.0.0
 * @since Sprint 1
 */
@Service
public class I18nService {

    private static final Logger logger = LoggerFactory.getLogger(I18nService.class);

    @Autowired
    private DatabaseManager dbManager;

    private String currentLanguage = "en";
    private final Map<String, Map<String, String>> translationCache = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════
    // SUPPORTED LANGUAGES — 22 Scheduled + English
    // ═══════════════════════════════════════════════════════════

    public enum Language {
        EN("en", "English", "English", "Latin"),
        HI("hi", "Hindi", "\u0939\u093f\u0928\u094d\u0926\u0940", "Devanagari"),
        BN("bn", "Bengali", "\u09ac\u09be\u0982\u09b2\u09be", "Bengali"),
        TE("te", "Telugu", "\u0c24\u0c46\u0c32\u0c41\u0c17\u0c41", "Telugu"),
        MR("mr", "Marathi", "\u092e\u0930\u093e\u0920\u0940", "Devanagari"),
        TA("ta", "Tamil", "\u0ba4\u0bae\u0bbf\u0bb4\u0bcd", "Tamil"),
        GU("gu", "Gujarati", "\u0a97\u0ac1\u0a9c\u0ab0\u0abe\u0aa4\u0ac0", "Gujarati"),
        UR("ur", "Urdu", "\u0627\u0631\u062f\u0648", "Perso-Arabic"),
        KN("kn", "Kannada", "\u0c95\u0ca8\u0ccd\u0ca8\u0ca1", "Kannada"),
        OR("or", "Odia", "\u0b13\u0b21\u0b3c\u0b3f\u0b06", "Odia"),
        ML("ml", "Malayalam", "\u0d2e\u0d32\u0d2f\u0d3e\u0d33\u0d02", "Malayalam"),
        PA("pa", "Punjabi", "\u0a2a\u0a70\u0a1c\u0a3e\u0a2c\u0a40", "Gurmukhi"),
        AS("as", "Assamese", "\u0985\u09b8\u09ae\u09c0\u09af\u09bc\u09be", "Bengali"),
        MAI("mai", "Maithili", "\u092e\u0948\u0925\u093f\u0932\u0940", "Devanagari"),
        SA("sa", "Sanskrit", "\u0938\u0902\u0938\u094d\u0915\u0943\u0924\u092e\u094d", "Devanagari"),
        KOK("kok", "Konkani", "\u0915\u094b\u0902\u0915\u0923\u0940", "Devanagari"),
        NE("ne", "Nepali", "\u0928\u0947\u092a\u093e\u0932\u0940", "Devanagari"),
        SD("sd", "Sindhi", "\u0938\u093f\u0928\u094d\u0927\u0940", "Devanagari"),
        KS("ks", "Kashmiri", "\u0915\u0936\u094d\u092e\u0940\u0930\u0940", "Perso-Arabic"),
        DOI("doi", "Dogri", "\u0921\u094b\u0917\u0930\u0940", "Devanagari"),
        MNI("mni", "Manipuri", "\u09ae\u09c8\u09a4\u09c8\u09b2\u09cb\u09a8\u09cd", "Bengali"),
        BO("bo", "Bodo", "\u092c\u094b\u0921\u094b", "Devanagari"),
        SAT("sat", "Santali", "\u1c65\u1c5f\u1c71\u1c5b\u1c5f\u1c62\u1c64", "Ol Chiki");

        public final String code;
        public final String name;
        public final String nativeName;
        public final String script;

        Language(String code, String name, String nativeName, String script) {
            this.code = code;
            this.name = name;
            this.nativeName = nativeName;
            this.script = script;
        }

        public static Language fromCode(String code) {
            for (Language l : values()) {
                if (l.code.equalsIgnoreCase(code)) return l;
            }
            return EN;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CORE TRANSLATIONS — DPDP Act Terms & UI Labels
    // ═══════════════════════════════════════════════════════════

    public void initialize() {
        logger.info("Initializing I18n Service with {} languages...", Language.values().length);
        loadCoreTranslations();
        logger.info("I18n Service initialized. Default language: {}", currentLanguage);
    }

    private void loadCoreTranslations() {
        // English (base language)
        Map<String, String> en = new LinkedHashMap<>();
        // DPDP Act Core Terms
        en.put("dpdp.act.title", "Digital Personal Data Protection Act, 2023");
        en.put("dpdp.data_principal", "Data Principal");
        en.put("dpdp.data_fiduciary", "Data Fiduciary");
        en.put("dpdp.significant_data_fiduciary", "Significant Data Fiduciary");
        en.put("dpdp.consent_manager", "Consent Manager");
        en.put("dpdp.data_processor", "Data Processor");
        en.put("dpdp.dpo", "Data Protection Officer");
        en.put("dpdp.dpbi", "Data Protection Board of India");
        en.put("dpdp.grievance_officer", "Grievance Officer");
        en.put("dpdp.personal_data", "Personal Data");
        en.put("dpdp.consent", "Consent");
        en.put("dpdp.legitimate_use", "Legitimate Use");
        en.put("dpdp.breach", "Personal Data Breach");
        en.put("dpdp.dpia", "Data Protection Impact Assessment");
        en.put("dpdp.rights", "Rights of Data Principal");
        en.put("dpdp.cross_border", "Cross-Border Data Transfer");
        en.put("dpdp.child_data", "Children's Data Protection");
        // Module Names
        en.put("module.dashboard", "Dashboard");
        en.put("module.consent", "Consent Management");
        en.put("module.policy", "Policy Engine");
        en.put("module.gap", "Gap Analysis");
        en.put("module.breach", "Breach Notification");
        en.put("module.dpia", "DPIA & Audit");
        en.put("module.siem", "QS-SIEM");
        en.put("module.dlp", "QS-DLP");
        en.put("module.pii", "PII Scanner");
        en.put("module.rights", "Data Rights");
        en.put("module.reports", "Reports");
        en.put("module.settings", "Settings");
        en.put("module.chatbot", "AI Assistant");
        en.put("module.licensing", "Licensing");
        // UI Common Labels
        en.put("ui.save", "Save");
        en.put("ui.cancel", "Cancel");
        en.put("ui.delete", "Delete");
        en.put("ui.edit", "Edit");
        en.put("ui.add", "Add New");
        en.put("ui.search", "Search");
        en.put("ui.export", "Export");
        en.put("ui.print", "Print");
        en.put("ui.refresh", "Refresh");
        en.put("ui.filter", "Filter");
        en.put("ui.actions", "Actions");
        en.put("ui.status", "Status");
        en.put("ui.created", "Created");
        en.put("ui.updated", "Updated");
        en.put("ui.view", "View");
        en.put("ui.download", "Download");
        en.put("ui.upload", "Upload");
        en.put("ui.confirm", "Confirm");
        en.put("ui.back", "Back");
        en.put("ui.next", "Next");
        en.put("ui.loading", "Loading...");
        en.put("ui.no_data", "No data available");
        // Consent-specific
        en.put("consent.notice", "Consent Notice");
        en.put("consent.collect", "Collect Consent");
        en.put("consent.withdraw", "Withdraw Consent");
        en.put("consent.renew", "Renew Consent");
        en.put("consent.audit_trail", "Consent Audit Trail");
        en.put("consent.guardian", "Guardian Consent");
        // Settings
        en.put("settings.organization", "Organization");
        en.put("settings.hierarchy", "Hierarchy");
        en.put("settings.employees", "Employees");
        en.put("settings.database", "Database");
        en.put("settings.language", "Language");
        en.put("settings.sector", "Sector");
        translationCache.put("en", en);

        // Hindi translations
        Map<String, String> hi = new LinkedHashMap<>();
        hi.put("dpdp.act.title", "\u0921\u093f\u091c\u093f\u091f\u0932 \u0935\u094d\u092f\u0915\u094d\u0924\u093f\u0917\u0924 \u0921\u0947\u091f\u093e \u0938\u0902\u0930\u0915\u094d\u0937\u0923 \u0905\u0927\u093f\u0928\u093f\u092f\u092e, 2023");
        hi.put("dpdp.data_principal", "\u0921\u0947\u091f\u093e \u092a\u094d\u0930\u093f\u0902\u0938\u093f\u092a\u0932");
        hi.put("dpdp.data_fiduciary", "\u0921\u0947\u091f\u093e \u092b\u093f\u0921\u094d\u092f\u0942\u0936\u093f\u092f\u0930\u0940");
        hi.put("dpdp.consent", "\u0938\u0939\u092e\u0924\u093f");
        hi.put("dpdp.breach", "\u0935\u094d\u092f\u0915\u094d\u0924\u093f\u0917\u0924 \u0921\u0947\u091f\u093e \u0909\u0932\u094d\u0932\u0902\u0918\u0928");
        hi.put("dpdp.dpia", "\u0921\u0947\u091f\u093e \u0938\u0902\u0930\u0915\u094d\u0937\u0923 \u092a\u094d\u0930\u092d\u093e\u0935 \u092e\u0942\u0932\u094d\u092f\u093e\u0902\u0915\u0928");
        hi.put("dpdp.rights", "\u0921\u0947\u091f\u093e \u092a\u094d\u0930\u093f\u0902\u0938\u093f\u092a\u0932 \u0915\u0947 \u0905\u0927\u093f\u0915\u093e\u0930");
        hi.put("dpdp.dpo", "\u0921\u0947\u091f\u093e \u0938\u0902\u0930\u0915\u094d\u0937\u0923 \u0905\u0927\u093f\u0915\u093e\u0930\u0940");
        hi.put("module.dashboard", "\u0921\u0948\u0936\u092c\u094b\u0930\u094d\u0921");
        hi.put("module.consent", "\u0938\u0939\u092e\u0924\u093f \u092a\u094d\u0930\u092c\u0902\u0927\u0928");
        hi.put("module.policy", "\u0928\u0940\u0924\u093f \u0907\u0902\u091c\u0928");
        hi.put("module.gap", "\u0905\u0902\u0924\u0930 \u0935\u093f\u0936\u094d\u0932\u0947\u0937\u0923");
        hi.put("module.breach", "\u0909\u0932\u094d\u0932\u0902\u0918\u0928 \u0938\u0942\u091a\u0928\u093e");
        hi.put("module.settings", "\u0938\u0947\u091f\u093f\u0902\u0917\u094d\u0938");
        hi.put("module.reports", "\u0930\u093f\u092a\u094b\u0930\u094d\u091f");
        hi.put("ui.save", "\u0938\u0939\u0947\u091c\u0947\u0902");
        hi.put("ui.cancel", "\u0930\u0926\u094d\u0926 \u0915\u0930\u0947\u0902");
        hi.put("ui.delete", "\u0939\u091f\u093e\u090f\u0902");
        hi.put("ui.edit", "\u0938\u0902\u092a\u093e\u0926\u093f\u0924 \u0915\u0930\u0947\u0902");
        hi.put("ui.search", "\u0916\u094b\u091c\u0947\u0902");
        hi.put("ui.loading", "\u0932\u094b\u0921 \u0939\u094b \u0930\u0939\u093e \u0939\u0948...");
        hi.put("ui.no_data", "\u0915\u094b\u0908 \u0921\u0947\u091f\u093e \u0909\u092a\u0932\u092c\u094d\u0927 \u0928\u0939\u0940\u0902");
        hi.put("consent.notice", "\u0938\u0939\u092e\u0924\u093f \u0938\u0942\u091a\u0928\u093e");
        hi.put("consent.collect", "\u0938\u0939\u092e\u0924\u093f \u090f\u0915\u0924\u094d\u0930 \u0915\u0930\u0947\u0902");
        hi.put("consent.withdraw", "\u0938\u0939\u092e\u0924\u093f \u0935\u093e\u092a\u0938 \u0932\u0947\u0902");
        hi.put("settings.organization", "\u0938\u0902\u0917\u0920\u0928");
        hi.put("settings.hierarchy", "\u092a\u0926\u093e\u0928\u0941\u0915\u094d\u0930\u092e");
        hi.put("settings.employees", "\u0915\u0930\u094d\u092e\u091a\u093e\u0930\u0940");
        hi.put("settings.language", "\u092d\u093e\u0937\u093e");
        translationCache.put("hi", hi);

        // Bengali translations
        Map<String, String> bn = new LinkedHashMap<>();
        bn.put("dpdp.act.title", "\u09a1\u09bf\u099c\u09bf\u099f\u09be\u09b2 \u09ac\u09cd\u09af\u0995\u09cd\u09a4\u09bf\u0997\u09a4 \u09a4\u09a5\u09cd\u09af \u09b8\u0982\u09b0\u0995\u09cd\u09b7\u09a3 \u0986\u0987\u09a8, \u09e8\u09e6\u09e8\u09e9");
        bn.put("dpdp.data_principal", "\u09a4\u09a5\u09cd\u09af \u09ae\u09c2\u09b2\u09a8\u09c0\u09a4\u09bf");
        bn.put("dpdp.consent", "\u09b8\u09ae\u09cd\u09ae\u09a4\u09bf");
        bn.put("module.dashboard", "\u09a1\u09cd\u09af\u09be\u09b6\u09ac\u09cb\u09b0\u09cd\u09a1");
        bn.put("ui.save", "\u09b8\u0982\u09b0\u0995\u09cd\u09b7\u09a3 \u0995\u09b0\u09c1\u09a8");
        bn.put("ui.search", "\u0985\u09a8\u09c1\u09b8\u09a8\u09cd\u09a7\u09be\u09a8");
        translationCache.put("bn", bn);

        // Tamil translations
        Map<String, String> ta = new LinkedHashMap<>();
        ta.put("dpdp.act.title", "\u0b9f\u0bbf\u0b9c\u0bbf\u0b9f\u0bcd\u0b9f\u0bb2\u0bcd \u0ba4\u0ba9\u0bbf\u0baa\u0bcd\u0baa\u0b9f\u0bcd\u0b9f \u0ba4\u0bb0\u0bb5\u0bc1 \u0baa\u0bbe\u0ba4\u0bc1\u0b95\u0bbe\u0baa\u0bcd\u0baa\u0bc1 \u0b9a\u0b9f\u0bcd\u0b9f\u0bae\u0bcd, 2023");
        ta.put("dpdp.consent", "\u0b92\u0baa\u0bcd\u0baa\u0bc1\u0ba4\u0bb2\u0bcd");
        ta.put("module.dashboard", "\u0b95\u0b9f\u0bcd\u0b9f\u0bc1\u0baa\u0bcd\u0baa\u0bb2\u0b95\u0bc8");
        ta.put("ui.save", "\u0b9a\u0bc7\u0bae\u0bbf\u0b95\u0bcd\u0b95");
        ta.put("ui.search", "\u0ba4\u0bc7\u0b9f\u0bc1");
        translationCache.put("ta", ta);

        // Telugu translations
        Map<String, String> te = new LinkedHashMap<>();
        te.put("dpdp.act.title", "\u0c21\u0c3f\u0c1c\u0c3f\u0c1f\u0c32\u0c4d \u0c35\u0c4d\u0c2f\u0c15\u0c4d\u0c24\u0c3f\u0c17\u0c24 \u0c21\u0c47\u0c1f\u0c3e \u0c30\u0c15\u0c4d\u0c37\u0c23 \u0c1a\u0c1f\u0c4d\u0c1f\u0c02, 2023");
        te.put("dpdp.consent", "\u0c38\u0c2e\u0c4d\u0c2e\u0c24\u0c3f");
        te.put("module.dashboard", "\u0c21\u0c4d\u0c2f\u0c3e\u0c37\u0c4d\u200c\u0c2c\u0c4b\u0c30\u0c4d\u0c21\u0c4d");
        te.put("ui.save", "\u0c38\u0c47\u0c35\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f");
        translationCache.put("te", te);

        // Marathi translations
        Map<String, String> mr = new LinkedHashMap<>();
        mr.put("dpdp.act.title", "\u0921\u093f\u091c\u093f\u091f\u0932 \u0935\u0948\u092f\u0915\u094d\u0924\u093f\u0915 \u0921\u0947\u091f\u093e \u0938\u0902\u0930\u0915\u094d\u0937\u0923 \u0915\u093e\u092f\u0926\u093e, \u0968\u0966\u0968\u0969");
        mr.put("dpdp.data_principal", "\u0921\u0947\u091f\u093e \u092a\u094d\u0930\u093f\u0902\u0938\u093f\u092a\u0932");
        mr.put("dpdp.consent", "\u0938\u0902\u092e\u0924\u0940");
        mr.put("dpdp.breach", "\u0935\u0948\u092f\u0915\u094d\u0924\u093f\u0915 \u0921\u0947\u091f\u093e \u0909\u0932\u094d\u0932\u0902\u0918\u0928");
        mr.put("module.dashboard", "\u0921\u0945\u0936\u092c\u094b\u0930\u094d\u0921");
        mr.put("module.consent", "\u0938\u0902\u092e\u0924\u0940 \u0935\u094d\u092f\u0935\u0938\u094d\u0925\u093e\u092a\u0928");
        mr.put("ui.save", "\u091c\u0924\u0928 \u0915\u0930\u093e");
        mr.put("ui.search", "\u0936\u094b\u0927\u093e");
        mr.put("ui.cancel", "\u0930\u0926\u094d\u0926 \u0915\u0930\u093e");
        translationCache.put("mr", mr);

        // Gujarati translations
        Map<String, String> gu = new LinkedHashMap<>();
        gu.put("dpdp.act.title", "\u0aa1\u0abf\u0a9c\u0abf\u0a9f\u0ab2 \u0ab5\u0acd\u0aaf\u0a95\u0acd\u0aa4\u0abf\u0a97\u0aa4 \u0aa1\u0ac7\u0a9f\u0abe \u0ab8\u0a82\u0ab0\u0a95\u0acd\u0ab7\u0aa3 \u0a95\u0abe\u0aaf\u0aa6\u0acb, \u0ae8\u0ae6\u0ae8\u0ae9");
        gu.put("dpdp.consent", "\u0ab8\u0a82\u0aae\u0aa4\u0abf");
        gu.put("dpdp.data_principal", "\u0aa1\u0ac7\u0a9f\u0abe \u0aaa\u0acd\u0ab0\u0abf\u0a82\u0ab8\u0abf\u0aaa\u0ab2");
        gu.put("module.dashboard", "\u0aa1\u0ac7\u0ab6\u0aac\u0acb\u0ab0\u0acd\u0aa1");
        gu.put("ui.save", "\u0ab8\u0abe\u0a9a\u0ab5\u0acb");
        gu.put("ui.search", "\u0ab6\u0acb\u0aa7\u0acb");
        translationCache.put("gu", gu);

        // Urdu translations
        Map<String, String> ur = new LinkedHashMap<>();
        ur.put("dpdp.act.title", "\u0688\u06cc\u062c\u06cc\u0679\u0644 \u0630\u0627\u062a\u06cc \u0688\u06cc\u0679\u0627 \u062a\u062d\u0641\u0638 \u0642\u0627\u0646\u0648\u0646\u060c \u06f2\u06f0\u06f2\u06f3");
        ur.put("dpdp.consent", "\u0631\u0636\u0627\u0645\u0646\u062f\u06cc");
        ur.put("dpdp.data_principal", "\u0688\u06cc\u0679\u0627 \u067e\u0631\u0646\u0633\u067e\u0644");
        ur.put("module.dashboard", "\u0688\u06cc\u0634 \u0628\u0648\u0631\u0688");
        ur.put("ui.save", "\u0645\u062d\u0641\u0648\u0638 \u06a9\u0631\u06cc\u06ba");
        ur.put("ui.search", "\u062a\u0644\u0627\u0634 \u06a9\u0631\u06cc\u06ba");
        translationCache.put("ur", ur);

        // Kannada translations
        Map<String, String> kn = new LinkedHashMap<>();
        kn.put("dpdp.act.title", "\u0ca1\u0cbf\u0c9c\u0cbf\u0c9f\u0cb2\u0ccd \u0cb5\u0cc8\u0caf\u0c95\u0ccd\u0ca4\u0cbf\u0c95 \u0ca1\u0cc7\u0c9f\u0cbe \u0cb8\u0c82\u0cb0\u0c95\u0ccd\u0cb7\u0ca3\u0cc6 \u0c95\u0cbe\u0caf\u0cbf\u0ca6\u0cc6, \u0ce8\u0ce6\u0ce8\u0ce9");
        kn.put("dpdp.consent", "\u0cb8\u0cae\u0ccd\u0cae\u0ca4\u0cbf");
        kn.put("module.dashboard", "\u0ca1\u0ccd\u0caf\u0cbe\u0cb6\u0ccd\u200c\u0cac\u0ccb\u0cb0\u0ccd\u0ca1\u0ccd");
        kn.put("ui.save", "\u0c89\u0cb3\u0cbf\u0cb8\u0cbf");
        kn.put("ui.search", "\u0cb9\u0cc1\u0ca1\u0cc1\u0c95\u0cbf");
        translationCache.put("kn", kn);

        // Odia translations
        Map<String, String> or = new LinkedHashMap<>();
        or.put("dpdp.act.title", "\u0b21\u0b3f\u0b1c\u0b3f\u0b1f\u0b3e\u0b32 \u0b2c\u0b4d\u0b5f\u0b15\u0b4d\u0b24\u0b3f\u0b17\u0b24 \u0b24\u0b25\u0b4d\u0b5f \u0b38\u0b41\u0b30\u0b15\u0b4d\u0b37\u0b3e \u0b06\u0b07\u0b28, \u0b68\u0b66\u0b68\u0b69");
        or.put("dpdp.consent", "\u0b38\u0b2e\u0b4d\u0b2e\u0b24\u0b3f");
        or.put("module.dashboard", "\u0b21\u0b4d\u0b5f\u0b3e\u0b38\u0b4d\u0b2c\u0b4b\u0b30\u0b4d\u0b21");
        or.put("ui.save", "\u0b38\u0b1e\u0b4d\u0b1a\u0b5f \u0b15\u0b30\u0b28\u0b4d\u0b24\u0b41");
        translationCache.put("or", or);

        // Malayalam translations
        Map<String, String> ml = new LinkedHashMap<>();
        ml.put("dpdp.act.title", "\u0d21\u0d3f\u0d1c\u0d3f\u0d31\u0d4d\u0d31\u0d7d \u0d35\u0d4d\u0d2f\u0d15\u0d4d\u0d24\u0d3f\u0d17\u0d24 \u0d21\u0d47\u0d31\u0d4d\u0d31 \u0d38\u0d02\u0d30\u0d15\u0d4d\u0d37\u0d23 \u0d28\u0d3f\u0d2f\u0d2e\u0d02, \u0d68\u0d66\u0d68\u0d69");
        ml.put("dpdp.consent", "\u0d38\u0d2e\u0d4d\u0d2e\u0d24\u0d02");
        ml.put("module.dashboard", "\u0d21\u0d3e\u0d37\u0d4d\u200c\u0d2c\u0d4b\u0d7c\u0d21\u0d4d");
        ml.put("ui.save", "\u0d38\u0d47\u0d35\u0d4d \u0d1a\u0d46\u0d2f\u0d4d\u0d2f\u0d41\u0d15");
        translationCache.put("ml", ml);

        // Punjabi translations
        Map<String, String> pa = new LinkedHashMap<>();
        pa.put("dpdp.act.title", "\u0a21\u0a3f\u0a1c\u0a3f\u0a1f\u0a32 \u0a28\u0a3f\u0a71\u0a1c\u0a40 \u0a21\u0a47\u0a1f\u0a3e \u0a38\u0a41\u0a30\u0a71\u0a16\u0a3f\u0a06 \u0a10\u0a15\u0a1f, \u0a68\u0a66\u0a68\u0a69");
        pa.put("dpdp.consent", "\u0a38\u0a39\u0a3f\u0a2e\u0a24\u0a40");
        pa.put("module.dashboard", "\u0a21\u0a48\u0a38\u0a3c\u0a2c\u0a4b\u0a30\u0a21");
        pa.put("ui.save", "\u0a38\u0a70\u0a2d\u0a3e\u0a32\u0a4b");
        translationCache.put("pa", pa);

        // Assamese translations
        Map<String, String> as = new LinkedHashMap<>();
        as.put("dpdp.act.title", "\u09a1\u09bf\u099c\u09bf\u099f\u09c7\u09b2 \u09ac\u09cd\u09af\u0995\u09cd\u09a4\u09bf\u0997\u09a4 \u09a4\u09a5\u09cd\u09af \u09b8\u09c1\u09f0\u0995\u09cd\u09b7\u09be \u0986\u0987\u09a8, \u09e8\u09e6\u09e8\u09e9");
        as.put("dpdp.consent", "\u09b8\u09ae\u09cd\u09ae\u09a4\u09bf");
        as.put("module.dashboard", "\u09a1\u09c7\u09b6\u09cd\u09ac'\u09f0\u09cd\u09a1");
        as.put("ui.save", "\u09b8\u09be\u0981\u099a\u09bf \u09f0\u09be\u0996\u0995");
        translationCache.put("as", as);

        // Maithili translations
        Map<String, String> mai = new LinkedHashMap<>();
        mai.put("dpdp.act.title", "\u0921\u093f\u091c\u093f\u091f\u0932 \u0935\u094d\u092f\u0915\u094d\u0924\u093f\u0917\u0924 \u0921\u0947\u091f\u093e \u0938\u0902\u0930\u0915\u094d\u0937\u0923 \u0915\u093e\u0928\u0942\u0928, \u0968\u0966\u0968\u0969");
        mai.put("dpdp.consent", "\u0938\u0939\u092e\u0924\u093f");
        mai.put("module.dashboard", "\u0921\u0948\u0936\u092c\u094b\u0930\u094d\u0921");
        mai.put("ui.save", "\u0938\u0902\u091a\u093f\u0924 \u0915\u0930\u0942");
        translationCache.put("mai", mai);

        // Sanskrit translations
        Map<String, String> sa = new LinkedHashMap<>();
        sa.put("dpdp.act.title", "\u0905\u0919\u094d\u0915\u0940\u092f \u0935\u094d\u092f\u0915\u094d\u0924\u093f\u0917\u0924 \u0926\u0924\u094d\u0924\u093e\u0902\u0936 \u0938\u0902\u0930\u0915\u094d\u0937\u0923 \u0935\u093f\u0927\u093f\u0903, \u0968\u0966\u0968\u0969");
        sa.put("dpdp.consent", "\u0905\u0928\u0941\u092e\u0924\u093f\u0903");
        sa.put("module.dashboard", "\u0928\u093f\u092f\u0928\u094d\u0924\u094d\u0930\u0923\u092b\u0932\u0915\u092e\u094d");
        sa.put("ui.save", "\u0938\u0902\u0930\u0915\u094d\u0937\u0924\u0941");
        translationCache.put("sa", sa);

        // Konkani translations
        Map<String, String> kok = new LinkedHashMap<>();
        kok.put("dpdp.act.title", "\u0921\u093f\u091c\u093f\u091f\u0932 \u0935\u0948\u092f\u0915\u094d\u0924\u093f\u0915 \u0921\u0947\u091f\u093e \u0938\u0902\u0930\u0915\u094d\u0937\u0923 \u0915\u093e\u092f\u0926\u094b, \u0968\u0966\u0968\u0969");
        kok.put("dpdp.consent", "\u0938\u0902\u092e\u0924\u0940");
        kok.put("module.dashboard", "\u0921\u0945\u0936\u092c\u094b\u0930\u094d\u0921");
        kok.put("ui.save", "\u0938\u093e\u0902\u092c\u093e\u0933\u093e");
        translationCache.put("kok", kok);

        // Nepali translations
        Map<String, String> ne = new LinkedHashMap<>();
        ne.put("dpdp.act.title", "\u0921\u093f\u091c\u093f\u091f\u0932 \u0935\u094d\u092f\u0915\u094d\u0924\u093f\u0917\u0924 \u0921\u093e\u091f\u093e \u0938\u0941\u0930\u0915\u094d\u0937\u093e \u0910\u0928, \u0968\u0966\u0968\u0969");
        ne.put("dpdp.consent", "\u0938\u0939\u092e\u0924\u093f");
        ne.put("module.dashboard", "\u0921\u094d\u092f\u093e\u0936\u092c\u094b\u0930\u094d\u0921");
        ne.put("ui.save", "\u0938\u0941\u0930\u0915\u094d\u0937\u093f\u0924 \u0917\u0930\u094d\u0928\u0941\u0939\u094b\u0938\u094d");
        translationCache.put("ne", ne);

        // Sindhi translations
        Map<String, String> sd = new LinkedHashMap<>();
        sd.put("dpdp.act.title", "\u0921\u093f\u091c\u093f\u091f\u0932 \u0935\u094d\u092f\u0915\u094d\u0924\u093f\u0917\u0924 \u0921\u0947\u091f\u093e \u0938\u0902\u0930\u0915\u094d\u0937\u0923 \u0915\u093e\u0928\u0942\u0928, \u0968\u0966\u0968\u0969");
        sd.put("dpdp.consent", "\u0938\u0939\u092e\u0924\u093f");
        sd.put("module.dashboard", "\u0921\u0948\u0936\u092c\u094b\u0930\u094d\u0921");
        sd.put("ui.save", "\u092e\u0939\u0942\u091c \u0915\u0930\u093f\u092f\u094b");
        translationCache.put("sd", sd);

        // Kashmiri translations
        Map<String, String> ks = new LinkedHashMap<>();
        ks.put("dpdp.act.title", "\u0921\u093f\u091c\u093f\u091f\u0932 \u0928\u093f\u091c\u0940 \u0921\u0947\u091f\u093e \u092c\u091a\u093e\u0935 \u0915\u093e\u0928\u0942\u0928, \u0968\u0966\u0968\u0969");
        ks.put("dpdp.consent", "\u0930\u091c\u093e\u092e\u0902\u0926\u0940");
        ks.put("module.dashboard", "\u0921\u0948\u0936\u092c\u094b\u0930\u094d\u0921");
        ks.put("ui.save", "\u092e\u0939\u092b\u0942\u091c\u093c \u0915\u0930\u093f\u0935");
        translationCache.put("ks", ks);

        // Dogri translations
        Map<String, String> doi = new LinkedHashMap<>();
        doi.put("dpdp.act.title", "\u0921\u093f\u091c\u093f\u091f\u0932 \u0935\u094d\u092f\u0915\u094d\u0924\u093f\u0917\u0924 \u0921\u0947\u091f\u093e \u0938\u0902\u0930\u0915\u094d\u0937\u0923 \u0915\u093e\u0928\u0942\u0928, \u0968\u0966\u0968\u0969");
        doi.put("dpdp.consent", "\u0938\u0939\u092e\u0924\u0940");
        doi.put("module.dashboard", "\u0921\u0948\u0936\u092c\u094b\u0930\u094d\u0921");
        doi.put("ui.save", "\u0938\u0902\u092d\u093e\u0932\u094b");
        translationCache.put("doi", doi);

        // Manipuri / Meitei translations
        Map<String, String> mni = new LinkedHashMap<>();
        mni.put("dpdp.act.title", "\u09a1\u09bf\u099c\u09bf\u099f\u09c7\u09b2 \u09aa\u09be\u09b0\u09cd\u09b8\u09a8\u09be\u09b2 \u09a1\u09c7\u099f\u09be \u09aa\u09cd\u09b0\u09cb\u099f\u09c7\u0995\u09b6\u09a8 \u098f\u0995\u09cd\u099f, \u09e8\u09e6\u09e8\u09e9");
        mni.put("dpdp.consent", "\u09aa\u09be\u09ae\u09cd\u09ac\u09c8 \u09aa\u09bf\u09b0\u09ac\u09be");
        mni.put("module.dashboard", "\u09a1\u09c7\u09b6\u09cd\u09ac\u09cb\u09b0\u09cd\u09a1");
        mni.put("ui.save", "\u09b6\u09c7\u09ae\u09cd\u09ac\u09bf\u09af\u09c1");
        translationCache.put("mni", mni);

        // Bodo translations
        Map<String, String> bo = new LinkedHashMap<>();
        bo.put("dpdp.act.title", "\u0921\u093f\u091c\u093f\u091f\u0932 \u092e\u0941\u0902\u0921\u093e\u0917\u094d\u0930\u093e \u0921\u0947\u091f\u093e \u091c\u0917\u093e\u0938\u093f\u0928\u093e\u092f \u0916\u093e\u0928\u094d\u0925\u093e\u092f, \u0968\u0966\u0968\u0969");
        bo.put("dpdp.consent", "\u0930\u093e\u091c\u093f\u0916\u0941\u0902\u0917\u093e");
        bo.put("module.dashboard", "\u0921\u0945\u0936\u092c\u094b\u0930\u094d\u0921");
        bo.put("ui.save", "\u0925\u093e\u0928\u093e\u092f");
        translationCache.put("bo", bo);

        // Santali translations (Ol Chiki script)
        Map<String, String> sat = new LinkedHashMap<>();
        sat.put("dpdp.act.title", "\u1c60\u1c5f\u1c77\u1c5b\u1c69\u1c5e\u1c5f\u1c62 \u1c60\u1c5f\u1c77\u1c5b\u1c5f \u1c6b\u1c68\u1c5e\u1c5f\u1c77\u1c64\u1c5f\u1c77 \u1c5f\u1c77\u1c6e\u1c5b, \u1c6d\u1c66\u1c6d\u1c69");
        sat.put("dpdp.consent", "\u1c68\u1c5f\u1c61\u1c5f\u1c77");
        sat.put("module.dashboard", "\u1c60\u1c5f\u1c77\u1c5b\u1c69\u1c5e\u1c5f\u1c62");
        sat.put("ui.save", "\u1c5e\u1c5f\u1c77\u1c5b\u1c5f\u1c63\u1c5f");
        translationCache.put("sat", sat);

        logger.info("Loaded core translations for {} languages", translationCache.size());
    }

    // ═══════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════

    /** Get translation for a key in the current language */
    public String t(String key) {
        return translate(key, currentLanguage);
    }

    /** Get translation for a key in a specific language */
    public String translate(String key, String langCode) {
        Map<String, String> translations = translationCache.getOrDefault(langCode, translationCache.get("en"));
        String result = translations.get(key);
        if (result == null) {
            // Fallback to English
            result = translationCache.get("en").getOrDefault(key, key);
        }
        return result;
    }

    /** Get all translations for a language */
    public Map<String, String> getTranslations(String langCode) {
        // Start with English as base, then overlay requested language
        Map<String, String> result = new LinkedHashMap<>(translationCache.getOrDefault("en", Map.of()));
        if (!"en".equals(langCode)) {
            Map<String, String> langTranslations = translationCache.get(langCode);
            if (langTranslations != null) {
                result.putAll(langTranslations);
            }
        }
        return result;
    }

    /** Get all available languages from the database */
    public List<Map<String, Object>> getAvailableLanguages() {
        List<Map<String, Object>> languages = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT code, name, native_name, script, is_enabled FROM supported_languages ORDER BY sort_order")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> lang = new LinkedHashMap<>();
                lang.put("code", rs.getString("code"));
                lang.put("name", rs.getString("name"));
                lang.put("nativeName", rs.getString("native_name"));
                lang.put("script", rs.getString("script"));
                lang.put("enabled", rs.getInt("is_enabled") == 1);
                languages.add(lang);
            }
        } catch (SQLException e) {
            logger.error("Failed to load available languages", e);
            // Fallback to enum
            for (Language lang : Language.values()) {
                languages.add(Map.of(
                        "code", lang.code, "name", lang.name,
                        "nativeName", lang.nativeName, "script", lang.script,
                        "enabled", true));
            }
        }
        return languages;
    }

    /** Get only enabled languages */
    public List<Map<String, Object>> getEnabledLanguages() {
        List<Map<String, Object>> all = getAvailableLanguages();
        return all.stream().filter(l -> Boolean.TRUE.equals(l.get("enabled"))).toList();
    }

    /** Enable or disable a language */
    public boolean setLanguageEnabled(String code, boolean enabled) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE supported_languages SET is_enabled = ? WHERE code = ?")) {
            stmt.setInt(1, enabled ? 1 : 0);
            stmt.setString(2, code);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to update language: {}", code, e);
            return false;
        }
    }

    /** Set the current session language */
    public void setCurrentLanguage(String langCode) {
        this.currentLanguage = langCode;
        logger.info("Language switched to: {}", langCode);
    }

    /** Get current language code */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /** Get total supported language count */
    public int getSupportedLanguageCount() {
        return Language.values().length;
    }
}
