package com.qsdpdp.consent;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Consent Template Registry — Sector-Specific, Version-Controlled, Multi-Language Templates
 *
 * Provides standardised consent templates that comply with DPDP Act requirements.
 * Templates are code-bound, multi-language, and version-controlled.
 *
 * CRUD: Create, Edit, Clone, Archive
 *
 * @version 1.0.0
 * @since Phase 7 — Consent Template Enhancement
 */
@Service
public class ConsentTemplateRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ConsentTemplateRegistry.class);

    @Autowired private DatabaseManager dbManager;
    @Autowired private AuditService auditService;

    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_templates (
                    template_id TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    template_code TEXT NOT NULL,
                    template_name TEXT NOT NULL,
                    sector TEXT NOT NULL,
                    purpose_id TEXT,
                    language_code TEXT DEFAULT 'en',
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    data_categories TEXT,
                    processing_actions TEXT,
                    legal_basis TEXT DEFAULT 'CONSENT',
                    retention_description TEXT,
                    withdrawal_instructions TEXT,
                    rights_summary TEXT,
                    layered_notice_short TEXT,
                    layered_notice_detailed TEXT,
                    regulatory_reference TEXT,
                    is_mandatory INTEGER DEFAULT 0,
                    dark_pattern_verified INTEGER DEFAULT 1,
                    accessibility_verified INTEGER DEFAULT 1,
                    status TEXT DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP,
                    created_by TEXT,
                    PRIMARY KEY (template_id, version, language_code)
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ct_sector ON consent_templates(sector)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ct_code ON consent_templates(template_code)");

            seedTemplates(conn);
            initialized = true;
            logger.info("ConsentTemplateRegistry initialized");

        } catch (SQLException e) {
            logger.error("Failed to initialize ConsentTemplateRegistry", e);
        }
    }

    // ── CRUD OPERATIONS ──

    public ConsentTemplate createTemplate(ConsentTemplate template) {
        if (!initialized) initialize();
        template.templateId = "TPL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        template.version = 1;
        template.status = "ACTIVE";
        persistTemplate(template);
        auditService.log("TEMPLATE_CREATED", "CONSENT_TEMPLATES", template.createdBy,
                "Template created: " + template.templateCode + " sector=" + template.sector);
        return template;
    }

    public ConsentTemplate editTemplate(String templateId, ConsentTemplate updates) {
        if (!initialized) initialize();
        ConsentTemplate current = getLatestTemplate(templateId, updates.languageCode != null ? updates.languageCode : "en");
        if (current == null) throw new IllegalArgumentException("Template not found: " + templateId);

        // New version
        ConsentTemplate newVersion = new ConsentTemplate();
        newVersion.templateId = templateId;
        newVersion.version = current.version + 1;
        newVersion.templateCode = current.templateCode;
        newVersion.templateName = updates.templateName != null ? updates.templateName : current.templateName;
        newVersion.sector = current.sector;
        newVersion.purposeId = updates.purposeId != null ? updates.purposeId : current.purposeId;
        newVersion.languageCode = updates.languageCode != null ? updates.languageCode : current.languageCode;
        newVersion.title = updates.title != null ? updates.title : current.title;
        newVersion.description = updates.description != null ? updates.description : current.description;
        newVersion.dataCategories = updates.dataCategories != null ? updates.dataCategories : current.dataCategories;
        newVersion.processingActions = updates.processingActions != null ? updates.processingActions : current.processingActions;
        newVersion.legalBasis = current.legalBasis;
        newVersion.retentionDescription = updates.retentionDescription != null ? updates.retentionDescription : current.retentionDescription;
        newVersion.withdrawalInstructions = updates.withdrawalInstructions != null ? updates.withdrawalInstructions : current.withdrawalInstructions;
        newVersion.rightsSummary = updates.rightsSummary != null ? updates.rightsSummary : current.rightsSummary;
        newVersion.layeredNoticeShort = updates.layeredNoticeShort != null ? updates.layeredNoticeShort : current.layeredNoticeShort;
        newVersion.layeredNoticeDetailed = updates.layeredNoticeDetailed != null ? updates.layeredNoticeDetailed : current.layeredNoticeDetailed;
        newVersion.status = "ACTIVE";
        newVersion.createdBy = updates.createdBy;

        persistTemplate(newVersion);
        return newVersion;
    }

    public ConsentTemplate cloneTemplate(String templateId, String newSector, String newLanguage) {
        if (!initialized) initialize();
        ConsentTemplate source = getLatestTemplate(templateId, "en");
        if (source == null) throw new IllegalArgumentException("Template not found: " + templateId);

        ConsentTemplate clone = new ConsentTemplate();
        clone.templateId = "TPL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        clone.version = 1;
        clone.templateCode = source.templateCode + "_" + newSector.toLowerCase();
        clone.templateName = source.templateName + " (" + newSector + ")";
        clone.sector = newSector;
        clone.purposeId = source.purposeId;
        clone.languageCode = newLanguage != null ? newLanguage : "en";
        clone.title = source.title;
        clone.description = source.description;
        clone.dataCategories = source.dataCategories;
        clone.processingActions = source.processingActions;
        clone.legalBasis = source.legalBasis;
        clone.retentionDescription = source.retentionDescription;
        clone.withdrawalInstructions = source.withdrawalInstructions;
        clone.rightsSummary = source.rightsSummary;
        clone.layeredNoticeShort = source.layeredNoticeShort;
        clone.layeredNoticeDetailed = source.layeredNoticeDetailed;
        clone.status = "ACTIVE";

        persistTemplate(clone);
        return clone;
    }

    public void archiveTemplate(String templateId) {
        String sql = "UPDATE consent_templates SET status = 'ARCHIVED', updated_at = ? WHERE template_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, templateId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to archive template", e);
        }
    }

    // ── QUERIES ──

    public List<ConsentTemplate> getTemplatesBySector(String sector) {
        List<ConsentTemplate> templates = new ArrayList<>();
        String sql = """
            SELECT * FROM consent_templates 
            WHERE sector = ? AND status = 'ACTIVE' AND language_code = 'en'
            AND version = (SELECT MAX(version) FROM consent_templates ct2 
                WHERE ct2.template_id = consent_templates.template_id AND ct2.language_code = consent_templates.language_code)
            ORDER BY template_name
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sector);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) templates.add(mapTemplate(rs));
        } catch (SQLException e) {
            logger.error("Failed to get templates by sector", e);
        }
        return templates;
    }

    public ConsentTemplate getLatestTemplate(String templateId, String languageCode) {
        String sql = "SELECT * FROM consent_templates WHERE template_id = ? AND language_code = ? ORDER BY version DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, templateId);
            ps.setString(2, languageCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapTemplate(rs);
        } catch (SQLException e) {
            logger.error("Failed to get template", e);
        }
        return null;
    }

    public Map<String, Object> getRegistryDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT template_id) FROM consent_templates WHERE status = 'ACTIVE'");
            if (rs.next()) dashboard.put("activeTemplates", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(DISTINCT language_code) FROM consent_templates");
            if (rs.next()) dashboard.put("languages", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(DISTINCT sector) FROM consent_templates WHERE status = 'ACTIVE'");
            if (rs.next()) dashboard.put("sectors", rs.getInt(1));

            List<Map<String, Object>> bySector = new ArrayList<>();
            rs = stmt.executeQuery("SELECT sector, COUNT(DISTINCT template_id) cnt FROM consent_templates WHERE status = 'ACTIVE' AND language_code = 'en' GROUP BY sector");
            while (rs.next()) bySector.add(Map.of("sector", rs.getString(1), "count", rs.getInt(2)));
            dashboard.put("templatesBySector", bySector);

            dashboard.put("status", "OPERATIONAL");
        } catch (SQLException e) {
            dashboard.put("status", "ERROR");
        }
        return dashboard;
    }

    // ── INTERNAL ──

    private void persistTemplate(ConsentTemplate t) {
        String sql = """
            INSERT INTO consent_templates (template_id, version, template_code, template_name, sector, purpose_id,
                language_code, title, description, data_categories, processing_actions, legal_basis,
                retention_description, withdrawal_instructions, rights_summary, layered_notice_short,
                layered_notice_detailed, regulatory_reference, is_mandatory, status, created_by)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.templateId); ps.setInt(2, t.version); ps.setString(3, t.templateCode);
            ps.setString(4, t.templateName); ps.setString(5, t.sector); ps.setString(6, t.purposeId);
            ps.setString(7, t.languageCode); ps.setString(8, t.title); ps.setString(9, t.description);
            ps.setString(10, t.dataCategories); ps.setString(11, t.processingActions);
            ps.setString(12, t.legalBasis); ps.setString(13, t.retentionDescription);
            ps.setString(14, t.withdrawalInstructions); ps.setString(15, t.rightsSummary);
            ps.setString(16, t.layeredNoticeShort); ps.setString(17, t.layeredNoticeDetailed);
            ps.setString(18, t.regulatoryReference); ps.setInt(19, t.isMandatory ? 1 : 0);
            ps.setString(20, t.status); ps.setString(21, t.createdBy);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist template", e);
        }
    }

    private ConsentTemplate mapTemplate(ResultSet rs) throws SQLException {
        ConsentTemplate t = new ConsentTemplate();
        t.templateId = rs.getString("template_id"); t.version = rs.getInt("version");
        t.templateCode = rs.getString("template_code"); t.templateName = rs.getString("template_name");
        t.sector = rs.getString("sector"); t.purposeId = rs.getString("purpose_id");
        t.languageCode = rs.getString("language_code"); t.title = rs.getString("title");
        t.description = rs.getString("description"); t.dataCategories = rs.getString("data_categories");
        t.processingActions = rs.getString("processing_actions"); t.legalBasis = rs.getString("legal_basis");
        t.retentionDescription = rs.getString("retention_description");
        t.withdrawalInstructions = rs.getString("withdrawal_instructions");
        t.rightsSummary = rs.getString("rights_summary");
        t.layeredNoticeShort = rs.getString("layered_notice_short");
        t.layeredNoticeDetailed = rs.getString("layered_notice_detailed");
        t.status = rs.getString("status");
        return t;
    }

    private void seedTemplates(Connection conn) throws SQLException {
        String check = "SELECT COUNT(*) FROM consent_templates";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        Object[][] templates = {
            {"TPL-BFSI-KYC", "bfsi_kyc", "BFSI KYC Consent", "BFSI", "PUR-KYC", "en",
                "Consent for KYC Verification",
                "We collect your identity and address information for mandatory Know Your Customer (KYC) verification as required by RBI regulations. This data will be used solely for identity verification and regulatory compliance.",
                "Identity,Contact,Financial,Address", "read,verify,store", "LEGAL_OBLIGATION",
                "Retained for the duration of your account relationship plus 5 years as per RBI guidelines",
                "You may withdraw this consent by visiting Settings > Privacy > Consent Management. Note: KYC withdrawal may result in account restrictions.",
                "You have the right to access, correct, and erase your personal data under DPDP Act §11-12. File grievances with the DPBI.",
                "We verify your identity using government-issued ID documents for regulatory compliance.",
                true, "DPDP §7(c), RBI KYC Master Direction 2016"},

            {"TPL-BFSI-MKT", "bfsi_marketing", "BFSI Marketing Consent", "BFSI", "PUR-MKT", "en",
                "Consent for Marketing Communications",
                "We would like to send you personalised offers, product recommendations, and promotional communications based on your preferences and transaction history. This is entirely optional.",
                "Contact,Preferences,Transaction", "read,profile,communicate", "CONSENT",
                "Retained until consent is withdrawn",
                "Withdraw anytime: Settings > Privacy > Marketing Preferences, or reply STOP to any message.",
                "You have the right to withdraw this consent at any time without affecting your account. DPDP §6(6).",
                "Optional: Receive personalised offers and promotions.",
                false, "DPDP §6(1)"},

            {"TPL-HEALTH-TREAT", "healthcare_treatment", "Healthcare Treatment Consent", "Healthcare", "PUR-TREAT", "en",
                "Consent for Medical Treatment Data Processing",
                "We process your health information including medical history, diagnoses, treatment plans, and lab results for providing you with medical care. This information is treated with the highest level of confidentiality.",
                "Identity,Health,Biometric,MedicalHistory", "read,write,share_with_treating_doctors", "VITAL_INTEREST",
                "Retained for the lifetime of the patient record or as required by ABDM guidelines",
                "You may restrict processing of health data. Contact DPO at dpo@hospital.in. Note: may affect continuity of care.",
                "Right to access your complete medical record. Right to data portability via ABDM. Right to restrict processing.",
                "Your health data is processed solely for your medical treatment.",
                true, "DPDP §7(c), ABDM Health Data Management Policy"},

            {"TPL-ECOM-ORDER", "ecommerce_order", "E-Commerce Order Processing", "E-Commerce", "PUR-ORDER", "en",
                "Consent for Order Processing",
                "We process your personal information to fulfil your order, including payment processing, shipping, delivery, and customer support. This is necessary to complete your purchase.",
                "Identity,Contact,Transaction,Address", "read,write,share_with_logistics", "CONTRACT",
                "Retained for 24 months after last transaction or as required by tax laws",
                "Withdraw by visiting Account > Privacy Settings > Data Consent.",
                "Access your order data, correct details, request deletion of past orders (subject to legal retention).",
                "We process your data to ship your order and provide support.",
                true, "DPDP §7(a), Consumer Protection Act"},

            {"TPL-TELECOM-SUB", "telecom_subscriber", "Telecom Subscriber Consent", "Telecom", "PUR-SUB", "en",
                "Consent for Subscriber Services",
                "We process your personal data to provide telecommunication services, manage your account, handle billing, and comply with Department of Telecommunications regulatory requirements.",
                "Identity,Contact,Usage,Location,Billing", "read,write,store,share_with_regulator", "CONTRACT",
                "Retained for duration of service plus 2 years as per DoT regulations",
                "Visit MyAccount > Privacy > Manage Consent or call customer care.",
                "Right to access usage records, correct information, port your number, lodge grievances.",
                "Your data is used for providing telecom services and regulatory compliance.",
                true, "DPDP §7(a), DoT License Agreement"},
        };

        String sql = """
            INSERT INTO consent_templates (template_id, version, template_code, template_name, sector, purpose_id,
                language_code, title, description, data_categories, processing_actions, legal_basis,
                retention_description, withdrawal_instructions, rights_summary, layered_notice_short,
                layered_notice_detailed, is_mandatory, regulatory_reference, status)
            VALUES (?,1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'ACTIVE')
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] t : templates) {
                ps.setString(1, (String) t[0]); ps.setString(2, (String) t[1]); ps.setString(3, (String) t[2]);
                ps.setString(4, (String) t[3]); ps.setString(5, (String) t[4]); ps.setString(6, (String) t[5]);
                ps.setString(7, (String) t[6]); ps.setString(8, (String) t[7]); ps.setString(9, (String) t[8]);
                ps.setString(10, (String) t[9]); ps.setString(11, (String) t[10]);
                ps.setString(12, (String) t[11]); ps.setString(13, (String) t[12]);
                ps.setString(14, (String) t[13]); ps.setString(15, (String) t[14]);
                ps.setString(16, (String) t[15]); ps.setInt(17, (Boolean) t[16] ? 1 : 0);
                ps.setString(18, (String) t[17]);
                ps.executeUpdate();
            }
        }
        logger.info("Seeded {} consent templates", templates.length);
    }

    public boolean isInitialized() { return initialized; }

    // DATA CLASS
    public static class ConsentTemplate {
        public String templateId, templateCode, templateName, sector, purposeId, languageCode;
        public int version;
        public String title, description, dataCategories, processingActions, legalBasis;
        public String retentionDescription, withdrawalInstructions, rightsSummary;
        public String layeredNoticeShort, layeredNoticeDetailed, regulatoryReference;
        public boolean isMandatory;
        public String status, createdBy;
    }
}
