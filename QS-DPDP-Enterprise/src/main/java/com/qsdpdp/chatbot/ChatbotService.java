package com.qsdpdp.chatbot;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.core.ComplianceEngine;
import com.qsdpdp.policy.PolicyService;
import com.qsdpdp.gap.GapAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Chatbot Service - RAG-based DPDP Compliance Consultant
 * Provides explainable AI responses with source citations
 * 
 * @version 1.0.0
 * @since Module 4
 */
@Service
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    private static final String DPDP_SYSTEM_PROMPT = """
            You are an expert DPDP (Digital Personal Data Protection Act 2023, India) compliance consultant.
            You provide accurate, actionable guidance on data protection compliance for Indian organizations.

            Key areas of expertise:
            - Consent management (Section 6-7), including guardian consent for children (Section 9)
            - Breach notification to DPBI (72 hours) and CERT-IN (6 hours)
            - Data principal rights (Section 11-14): access, correction, erasure, grievance, nomination
            - Data Protection Impact Assessments (DPIA)
            - Cross-border data transfers
            - Significant Data Fiduciary obligations

            Rules:
            - Always cite specific DPDP Act sections when applicable
            - Provide step-by-step guidance when asked "how to" questions
            - If unsure, clearly state limitations and recommend consulting a legal expert
            - Respond in the language the user asks in (support Hindi and English)
            - Keep responses concise but comprehensive
            """;

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final ComplianceEngine complianceEngine;
    private final PolicyService policyService;
    private final LLMProvider llmProvider;

    private boolean initialized = false;
    private boolean llmAvailable = false;
    private final Map<String, List<ChatQuery>> sessions = new HashMap<>();

    @Autowired
    public ChatbotService(DatabaseManager dbManager, AuditService auditService,
            EventBus eventBus, ComplianceEngine complianceEngine,
            PolicyService policyService, LLMProvider llmProvider) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
        this.complianceEngine = complianceEngine;
        this.policyService = policyService;
        this.llmProvider = llmProvider;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing Chatbot Service v3.0...");
        createTables();

        // Initialize LLM provider
        if (llmProvider instanceof OllamaLLMProvider ollamaProvider) {
            ollamaProvider.initialize();
            llmAvailable = ollamaProvider.isAvailable();
        }

        initialized = true;
        logger.info("Chatbot Service initialized [LLM={}, articles={}]",
                llmAvailable ? llmProvider.getModelName() : "keyword-fallback",
                DPDPKnowledgeBase.getTotalArticles());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS chat_sessions (
                            id TEXT PRIMARY KEY,
                            user_id TEXT,
                            started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            last_activity TIMESTAMP,
                            query_count INTEGER DEFAULT 0
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS chat_history (
                            id TEXT PRIMARY KEY,
                            session_id TEXT,
                            user_id TEXT,
                            query TEXT,
                            query_type TEXT,
                            response TEXT,
                            confidence REAL,
                            sources TEXT,
                            processing_time_ms INTEGER,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS chat_feedback (
                            id TEXT PRIMARY KEY,
                            history_id TEXT,
                            helpful INTEGER,
                            comment TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (history_id) REFERENCES chat_history(id)
                        )
                    """);

            logger.info("Chatbot tables created");

        } catch (SQLException e) {
            logger.error("Failed to create Chatbot tables", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CHAT PROCESSING
    // ═══════════════════════════════════════════════════════════

    public ChatResponse processQuery(ChatQuery query) {
        long startTime = System.currentTimeMillis();

        logger.debug("Processing query: {} (type: {})", query.getQuery(), query.getType());

        ChatResponse response;

        try {
            // Try LLM-enhanced response first (true RAG: knowledge base + LLM)
            response = tryLLMResponse(query);

            // Fall back to structured handlers if LLM unavailable or returned null
            if (response == null) {
                response = switch (query.getType()) {
                    case EXPLANATION -> handleExplanation(query);
                    case GUIDANCE -> handleGuidance(query);
                    case GENERATION -> handleGeneration(query);
                    case LEGAL_REFERENCE -> handleLegalReference(query);
                    case NAVIGATION -> handleNavigation(query);
                    case TROUBLESHOOTING -> handleTroubleshooting(query);
                    default -> handleGeneral(query);
                };
            }
        } catch (Exception e) {
            logger.error("Error processing query", e);
            response = new ChatResponse(query.getId(),
                    "I apologize, but I encountered an error processing your query. " +
                            "Please try rephrasing or contact support.",
                    0.0);
        }

        response.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        // Persist history
        persistChatHistory(query, response);

        // Audit
        auditService.log("CHATBOT_QUERY", "CHATBOT", query.getUserId(),
                query.getType() + ": " + truncate(query.getQuery(), 50));

        return response;
    }

    /**
     * Attempt LLM-enhanced RAG response.
     * Uses knowledge base articles as context + LLM for natural language generation.
     * Returns null if LLM is unavailable (triggers keyword-matching fallback).
     */
    private ChatResponse tryLLMResponse(ChatQuery query) {
        if (!llmAvailable || llmProvider == null) {
            return null;
        }

        // For NAVIGATION and GENERATION types, prefer structured handlers
        if (query.getType() == ChatQuery.QueryType.NAVIGATION ||
            query.getType() == ChatQuery.QueryType.GENERATION) {
            return null;
        }

        try {
            // Build RAG context from knowledge base
            String context = buildKnowledgeContext(query.getQuery());

            // Call LLM with DPDP system prompt + knowledge context + user query
            String llmResponse = llmProvider.generateResponse(
                    DPDP_SYSTEM_PROMPT, query.getQuery(), context);

            if (llmResponse != null && !llmResponse.isBlank()) {
                ChatResponse response = new ChatResponse(
                        query.getId(), llmResponse, 0.85);
                response.addSource("AI Assistant",
                        "Model: " + llmProvider.getModelName(),
                        "LLM-generated with RAG context");

                // Add knowledge base sources used for grounding
                List<DPDPKnowledgeBase.KnowledgeArticle> articles =
                        DPDPKnowledgeBase.search(query.getQuery());
                for (int i = 0; i < Math.min(2, articles.size()); i++) {
                    response.addSource(articles.get(i).getTitle(),
                            articles.get(i).getReference(), "");
                }

                return response;
            }
        } catch (Exception e) {
            logger.warn("LLM response failed, falling back to keyword matching: {}", e.getMessage());
        }

        return null; // Trigger fallback
    }

    /**
     * Build RAG knowledge context from the DPDP knowledge base.
     * Searches for relevant articles and formats them as LLM context.
     */
    private String buildKnowledgeContext(String query) {
        List<DPDPKnowledgeBase.KnowledgeArticle> articles = DPDPKnowledgeBase.search(query);
        if (articles.isEmpty()) return "";

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < Math.min(3, articles.size()); i++) {
            DPDPKnowledgeBase.KnowledgeArticle article = articles.get(i);
            context.append("--- Article: ").append(article.getTitle())
                    .append(" (").append(article.getReference()).append(") ---\n")
                    .append(article.getContent()).append("\n\n");
        }
        return context.toString();
    }

    private ChatResponse handleExplanation(ChatQuery query) {
        String q = query.getQuery().toLowerCase();

        // Search knowledge base
        List<DPDPKnowledgeBase.KnowledgeArticle> articles = DPDPKnowledgeBase.search(q);

        if (articles.isEmpty()) {
            return ChatResponse.lowConfidence(query.getId(),
                    "I couldn't find a direct answer in my knowledge base. " +
                            "Could you please rephrase your question or be more specific?",
                    "What specific aspect of DPDP compliance would you like to know about?");
        }

        DPDPKnowledgeBase.KnowledgeArticle topArticle = articles.get(0);
        double confidence = Math.min(topArticle.getRelevanceScore() / 20.0, 1.0);

        ChatResponse response = new ChatResponse(query.getId(), topArticle.getContent(), confidence);
        response.addSource(topArticle.getTitle(), topArticle.getReference(),
                topArticle.getSummary(100));

        // Add related articles as additional sources
        for (int i = 1; i < Math.min(3, articles.size()); i++) {
            DPDPKnowledgeBase.KnowledgeArticle related = articles.get(i);
            response.addSource(related.getTitle(), related.getReference(),
                    related.getSummary(80));
        }

        // Add navigation action if applicable
        if (q.contains("consent")) {
            response.addAction("Open Consent Manager", "NAVIGATE", Map.of("module", "consent"));
        }
        if (q.contains("breach")) {
            response.addAction("Open Breach Manager", "NAVIGATE", Map.of("module", "breach"));
        }

        return response;
    }

    private ChatResponse handleGuidance(ChatQuery query) {
        String q = query.getQuery().toLowerCase();

        StringBuilder guidance = new StringBuilder();
        double confidence = 0.8;

        if (q.contains("consent")) {
            guidance.append("""
                    **How to Collect Valid Consent:**

                    1. **Prepare Consent Form**
                       - Navigate to Consent Manager → Templates
                       - Select sector-appropriate template
                       - Customize purposes and data categories

                    2. **Configure Collection**
                       - Enable multilingual support
                       - Set up double opt-in for sensitive data
                       - Configure withdrawal mechanism

                    3. **Deploy**
                       - Integrate via Web/Mobile widget
                       - Test end-to-end flow
                       - Verify audit trail generation

                    4. **Monitor**
                       - Review consent analytics
                       - Track withdrawal rates
                       - Ensure SLA compliance
                    """);
        } else if (q.contains("breach")) {
            guidance.append("""
                    **How to Handle a Data Breach:**

                    1. **Immediate (0-6 hours)**
                       - Navigate to Breach Manager → Report Incident
                       - Classify severity (Critical/High/Medium/Low)
                       - Initiate containment procedures
                       - Notify CERT-In if cyber incident

                    2. **Assessment (6-72 hours)**
                       - Complete impact assessment
                       - Identify affected data principals
                       - Document evidence
                       - Prepare DPBI notification

                    3. **Notification**
                       - Submit to DPBI via portal
                       - Notify affected individuals
                       - Record all notifications

                    4. **Recovery**
                       - Implement remediation
                       - Update controls
                       - Document lessons learned
                    """);
        } else if (q.contains("dpia") || q.contains("impact assessment")) {
            guidance.append("""
                    **How to Conduct a DPIA:**

                    1. **Initiate DPIA**
                       - Navigate to DPIA Manager → New Assessment
                       - Select processing activity
                       - Define scope and objectives

                    2. **Data Mapping**
                       - Document data categories
                       - Map data flows
                       - Identify processors and recipients

                    3. **Risk Assessment**
                       - Identify potential risks
                       - Rate likelihood and impact
                       - Calculate risk scores

                    4. **Mitigation**
                       - Define control measures
                       - Assign responsibilities
                       - Set implementation timeline

                    5. **Approval**
                       - DPO review
                       - Management sign-off
                       - DPBI consultation if required
                    """);
        } else {
            // Generic guidance search
            List<DPDPKnowledgeBase.KnowledgeArticle> articles = DPDPKnowledgeBase.search(q);
            if (!articles.isEmpty()) {
                guidance.append(articles.get(0).getContent());
                confidence = 0.7;
            } else {
                return ChatResponse.lowConfidence(query.getId(),
                        "I need more context to provide specific guidance.",
                        "Please specify what task you need help with (e.g., consent collection, breach handling, DPIA)");
            }
        }

        ChatResponse response = new ChatResponse(query.getId(), guidance.toString(), confidence);
        response.addAction("View Step-by-Step Tutorial", "HELP", Map.of("topic", "guidance"));
        return response;
    }

    private ChatResponse handleGeneration(ChatQuery query) {
        String q = query.getQuery().toLowerCase();

        if (q.contains("policy") || q.contains("sop")) {
            String policyType = extractPolicyType(q);
            String template = generatePolicyTemplate(policyType);

            ChatResponse response = new ChatResponse(query.getId(), template, 0.85);
            response.addSource("Policy Engine", "QS-DPDP Templates", "Auto-generated template");
            response.addAction("Open in Policy Editor", "NAVIGATE",
                    Map.of("module", "policy", "template", policyType));
            return response;
        }

        if (q.contains("consent form") || q.contains("consent template")) {
            String template = generateConsentFormTemplate();
            ChatResponse response = new ChatResponse(query.getId(), template, 0.9);
            response.addSource("Consent Manager", "Template Library", "DPDP-compliant template");
            response.addAction("Create Consent Form", "NAVIGATE", Map.of("module", "consent"));
            return response;
        }

        if (q.contains("notice") || q.contains("privacy notice")) {
            String template = generatePrivacyNoticeTemplate();
            ChatResponse response = new ChatResponse(query.getId(), template, 0.9);
            response.addSource("DPDP Act", "Section 5", "Notice requirements");
            return response;
        }

        return ChatResponse.lowConfidence(query.getId(),
                "I can generate policies, SOPs, consent forms, and notices. " +
                        "Please specify what you'd like me to generate.",
                "Example: 'Generate a data retention policy' or 'Create a consent form template'");
    }

    private ChatResponse handleLegalReference(ChatQuery query) {
        String q = query.getQuery().toLowerCase();

        List<DPDPKnowledgeBase.KnowledgeArticle> articles = DPDPKnowledgeBase.search(q);

        if (articles.isEmpty()) {
            return ChatResponse.lowConfidence(query.getId(),
                    "I couldn't find the specific legal reference. " +
                            "Please try searching for specific sections (e.g., 'Section 6', 'Section 8(6)').",
                    "Which DPDP Act section or clause are you looking for?");
        }

        StringBuilder response = new StringBuilder();
        for (DPDPKnowledgeBase.KnowledgeArticle article : articles.subList(0, Math.min(3, articles.size()))) {
            response.append("### ").append(article.getTitle())
                    .append(" (").append(article.getReference()).append(")\n\n")
                    .append(article.getContent()).append("\n\n---\n\n");
        }

        ChatResponse chatResponse = new ChatResponse(query.getId(), response.toString(), 0.95);
        for (DPDPKnowledgeBase.KnowledgeArticle article : articles.subList(0, Math.min(3, articles.size()))) {
            chatResponse.addSource(article.getTitle(), article.getReference(), "");
        }
        return chatResponse;
    }

    private ChatResponse handleNavigation(ChatQuery query) {
        String q = query.getQuery().toLowerCase();

        String module = null;
        String description = null;

        if (q.contains("consent")) {
            module = "consent";
            description = "Consent Management module lets you collect, manage, and track consents.";
        } else if (q.contains("breach")) {
            module = "breach";
            description = "Breach Management module helps you report and track data breaches.";
        } else if (q.contains("rights") || q.contains("dsar")) {
            module = "rights";
            description = "Rights Management module handles data subject access requests.";
        } else if (q.contains("dpia")) {
            module = "dpia";
            description = "DPIA module helps you conduct Data Protection Impact Assessments.";
        } else if (q.contains("policy")) {
            module = "policy";
            description = "Policy Engine manages your privacy policies and SOPs.";
        } else if (q.contains("scan") || q.contains("pii")) {
            module = "pii-scanner";
            description = "PII Scanner discovers and classifies personal data in your systems.";
        } else if (q.contains("siem") || q.contains("security")) {
            module = "siem";
            description = "SIEM module monitors security events and threats.";
        } else if (q.contains("dlp")) {
            module = "dlp";
            description = "DLP module prevents unauthorized data transfers.";
        } else if (q.contains("gap") || q.contains("assessment")) {
            module = "gap-analysis";
            description = "Gap Analysis helps you assess compliance status.";
        } else if (q.contains("dashboard") || q.contains("home")) {
            module = "dashboard";
            description = "Dashboard shows your overall compliance status.";
        }

        if (module != null) {
            ChatResponse response = new ChatResponse(query.getId(), description, 0.95);
            response.addAction("Go to " + capitalize(module), "NAVIGATE", Map.of("module", module));
            return response;
        }

        return ChatResponse.lowConfidence(query.getId(),
                "I can help you navigate to any module. Available modules include:\n" +
                        "- Dashboard\n- Consent Management\n- Breach Management\n- Rights Management\n" +
                        "- DPIA\n- Policy Engine\n- PII Scanner\n- SIEM\n- DLP\n- Gap Analysis",
                "Which module would you like to access?");
    }

    private ChatResponse handleTroubleshooting(ChatQuery query) {
        String q = query.getQuery().toLowerCase();

        StringBuilder solution = new StringBuilder();

        if (q.contains("consent") && q.contains("not work")) {
            solution.append("""
                    **Consent Collection Troubleshooting:**

                    1. **Check Configuration**
                       - Verify consent form is enabled
                       - Confirm widget is properly embedded
                       - Check purpose categories are defined

                    2. **Test Mode**
                       - Enable debug logging
                       - Check browser console for errors
                       - Verify API connectivity

                    3. **Common Issues**
                       - CORS errors: Configure allowed origins
                       - Invalid consent: Check required fields
                       - Storage errors: Verify database connection

                    If issue persists, please check the audit logs or contact support.
                    """);
        } else if (q.contains("breach") || q.contains("notification")) {
            solution.append("""
                    **Breach Notification Troubleshooting:**

                    1. **Notification Not Sent**
                       - Check email/SMS gateway configuration
                       - Verify recipient addresses
                       - Review notification queue

                    2. **Timeline Issues**
                       - Verify system clock sync
                       - Check SLA configuration
                       - Review escalation rules

                    3. **DPBI/CERT-In Integration**
                       - Verify API credentials
                       - Check network connectivity
                       - Review submission logs
                    """);
        } else {
            solution.append("""
                    **General Troubleshooting Steps:**

                    1. Check system logs (Settings → Logs)
                    2. Verify database connectivity
                    3. Review audit trail for errors
                    4. Check user permissions
                    5. Clear cache and retry

                    For specific issues, please describe:
                    - What action you were performing
                    - What error message you saw
                    - When the issue started
                    """);
        }

        ChatResponse response = new ChatResponse(query.getId(), solution.toString(), 0.75);
        response.addAction("View System Logs", "NAVIGATE", Map.of("module", "settings", "tab", "logs"));
        response.addAction("Contact Support", "ACTION", Map.of("action", "support"));
        return response;
    }

    private ChatResponse handleGeneral(ChatQuery query) {
        // Try knowledge base first
        List<DPDPKnowledgeBase.KnowledgeArticle> articles = DPDPKnowledgeBase.search(query.getQuery());

        if (!articles.isEmpty()) {
            DPDPKnowledgeBase.KnowledgeArticle top = articles.get(0);
            ChatResponse response = new ChatResponse(query.getId(), top.getContent(),
                    Math.min(top.getRelevanceScore() / 15.0, 0.9));
            response.addSource(top.getTitle(), top.getReference(), top.getSummary(100));
            return response;
        }

        // Fallback
        return ChatResponse.lowConfidence(query.getId(),
                "I'm here to help with DPDP compliance. I can:\n\n" +
                        "- **Explain** DPDP Act provisions\n" +
                        "- **Guide** you through compliance tasks\n" +
                        "- **Generate** policies and templates\n" +
                        "- **Navigate** to relevant modules\n" +
                        "- **Troubleshoot** issues\n\n" +
                        "How can I assist you?",
                "Try asking 'What is consent under DPDP?' or 'How do I report a breach?'");
    }

    // ═══════════════════════════════════════════════════════════
    // TEMPLATE GENERATORS
    // ═══════════════════════════════════════════════════════════

    private String extractPolicyType(String query) {
        if (query.contains("retention"))
            return "data_retention";
        if (query.contains("security"))
            return "security";
        if (query.contains("privacy"))
            return "privacy";
        if (query.contains("breach"))
            return "breach_response";
        if (query.contains("access"))
            return "access_control";
        return "general";
    }

    private String generatePolicyTemplate(String type) {
        return """
                # %s Policy

                **Document ID:** POL-%s-%d
                **Version:** 1.0
                **Effective Date:** [Date]
                **Review Date:** [Date + 1 year]
                **Owner:** [Role]
                **Approved By:** [Authority]

                ## 1. Purpose
                [State the purpose of this policy]

                ## 2. Scope
                This policy applies to:
                - [List applicable systems/processes]
                - [List applicable personnel]
                - [List applicable data types]

                ## 3. Policy Statement
                [Core policy requirements aligned with DPDP Act 2023]

                ## 4. Responsibilities
                | Role | Responsibility |
                |------|---------------|
                | DPO | Oversight and review |
                | IT | Implementation |
                | HR | Training |
                | All Staff | Compliance |

                ## 5. Procedures
                [Step-by-step procedures]

                ## 6. DPDP Act Alignment
                This policy addresses:
                - Section [X]: [Requirement]
                - Section [Y]: [Requirement]

                ## 7. Compliance Monitoring
                [How compliance will be monitored]

                ## 8. Exceptions
                [Process for requesting exceptions]

                ## 9. Related Documents
                - [Related policies]
                - [Related procedures]

                ## 10. Revision History
                | Version | Date | Author | Changes |
                |---------|------|--------|---------|
                | 1.0 | [Date] | [Author] | Initial release |
                """.formatted(capitalize(type.replace("_", " ")), type.toUpperCase(),
                System.currentTimeMillis() % 10000);
    }

    private String generateConsentFormTemplate() {
        return """
                # Consent Form Template

                **Organization:** [Organization Name]
                **DPO Contact:** [DPO Email/Phone]

                ---

                ## We Need Your Consent

                [Organization Name] values your privacy. Before we process your personal data,
                we need your informed consent as required under the Digital Personal Data
                Protection Act, 2023.

                ### Personal Data We Collect

                ☐ **Identity Data** - Name, date of birth, ID numbers
                ☐ **Contact Data** - Address, email, phone number
                ☐ **Financial Data** - Bank account, payment details
                ☐ **Other Data** - [Specify]

                ### Why We Need Your Data

                We will use your data only for the following purposes:

                ☐ **Purpose 1:** [Description] - Retention: [Period]
                ☐ **Purpose 2:** [Description] - Retention: [Period]
                ☐ **Purpose 3:** [Description] - Retention: [Period]

                ### Who We Share With

                Your data may be shared with:
                - [Third Party 1] - For [Purpose]
                - [Third Party 2] - For [Purpose]

                ### Your Rights

                You have the right to:
                - Access your data
                - Correct inaccurate data
                - Request erasure (with limitations)
                - Withdraw consent at any time
                - Lodge a complaint with DPBI

                ### Withdrawal of Consent

                You may withdraw consent by:
                - Visiting [Portal URL]
                - Emailing [DPO Email]
                - Calling [Support Number]

                Withdrawal is as easy as giving consent.

                ---

                ## Your Consent

                ☐ I have read and understood this consent notice
                ☐ I consent to the processing of my personal data for the purposes stated above

                **Name:** ______________________
                **Date:** ______________________
                **Signature:** _________________
                """;
    }

    private String generatePrivacyNoticeTemplate() {
        return """
                # Privacy Notice

                **Effective Date:** [Date]
                **Last Updated:** [Date]

                This Privacy Notice explains how [Organization Name] ("we", "us", "our")
                collects, uses, and protects your personal data in accordance with the
                Digital Personal Data Protection Act, 2023 (DPDP Act).

                ## 1. Who We Are

                [Organization Name]
                [Address]
                Data Protection Officer: [DPO Name]
                Contact: [DPO Contact]

                ## 2. Data We Collect

                We collect the following categories of personal data:
                - [Category 1]
                - [Category 2]
                - [Category 3]

                ## 3. How We Use Your Data

                We use your data for:
                - [Purpose 1]
                - [Purpose 2]
                - [Purpose 3]

                Legal basis: [Consent / Legitimate Use]

                ## 4. Data Sharing

                We may share your data with:
                - [Recipient Category 1] for [Purpose]
                - [Recipient Category 2] for [Purpose]

                ## 5. Cross-Border Transfers

                [Describe any international transfers]

                ## 6. Data Retention

                We retain your data for [Period] or until [Condition].

                ## 7. Your Rights

                Under the DPDP Act, you have the right to:
                - Access your personal data (Section 11)
                - Correct inaccurate data (Section 12)
                - Erase your data (Section 12)
                - Grievance redressal (Section 13)
                - Nominate a representative (Section 14)

                ## 8. How to Exercise Your Rights

                Contact our DPO at [Contact] or visit [Portal URL].

                ## 9. Complaints

                If unsatisfied with our response, you may complain to:
                Data Protection Board of India
                [Contact Details]

                ## 10. Updates to This Notice

                We will notify you of material changes.

                ---
                *This notice is available in [Languages]*
                """;
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════

    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty())
            return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private void persistChatHistory(ChatQuery query, ChatResponse response) {
        String sql = """
                    INSERT INTO chat_history (id, session_id, user_id, query, query_type,
                        response, confidence, processing_time_ms, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, response.getId());
            stmt.setString(2, query.getSessionId());
            stmt.setString(3, query.getUserId());
            stmt.setString(4, query.getQuery());
            stmt.setString(5, query.getType().name());
            stmt.setString(6, truncate(response.getResponse(), 2000));
            stmt.setDouble(7, response.getConfidence());
            stmt.setLong(8, response.getProcessingTimeMs());
            stmt.setString(9, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist chat history", e);
        }
    }

    public void submitFeedback(String historyId, boolean helpful, String comment) {
        String sql = "INSERT INTO chat_feedback (id, history_id, helpful, comment) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, historyId);
            stmt.setInt(3, helpful ? 1 : 0);
            stmt.setString(4, comment);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to save feedback", e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
