package com.qsdpdp.dashboard;

import com.qsdpdp.consent.ConsentService;
import com.qsdpdp.consent.ConsentStatistics;
import com.qsdpdp.breach.BreachService;
import com.qsdpdp.breach.BreachStatistics;
import com.qsdpdp.rights.RightsService;
import com.qsdpdp.rights.RightsStatistics;
import com.qsdpdp.dpia.DPIAService;
import com.qsdpdp.dpia.DPIAStatistics;
import com.qsdpdp.policy.PolicyService;
import com.qsdpdp.policy.PolicyStatistics;
import com.qsdpdp.user.UserService;
import com.qsdpdp.user.UserStatistics;
import com.qsdpdp.rag.RAGEvaluator;
import com.qsdpdp.rag.RAGStatus;
import com.qsdpdp.core.ComplianceEngine;
import com.qsdpdp.core.ComplianceScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Dashboard Service
 * Aggregates data from all modules for enterprise dashboard
 */
@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final ConsentService consentService;
    private final BreachService breachService;
    private final RightsService rightsService;
    private final DPIAService dpiaService;
    private final PolicyService policyService;
    private final UserService userService;
    private final RAGEvaluator ragEvaluator;
    private final ComplianceEngine complianceEngine;

    private boolean initialized = false;

    @Autowired
    public DashboardService(ConsentService consentService, BreachService breachService,
            RightsService rightsService, DPIAService dpiaService,
            PolicyService policyService, UserService userService,
            RAGEvaluator ragEvaluator, ComplianceEngine complianceEngine) {
        this.consentService = consentService;
        this.breachService = breachService;
        this.rightsService = rightsService;
        this.dpiaService = dpiaService;
        this.policyService = policyService;
        this.userService = userService;
        this.ragEvaluator = ragEvaluator;
        this.complianceEngine = complianceEngine;
    }

    public void initialize() {
        if (initialized)
            return;
        logger.info("Initializing Dashboard Service...");
        initialized = true;
        logger.info("Dashboard Service initialized");
    }

    /**
     * Get complete dashboard data for the executive view
     */
    public DashboardData getDashboardData() {
        DashboardData data = new DashboardData();

        try {
            // Overall compliance score
            ComplianceScore score = complianceEngine.calculateOverallScore();
            data.setOverallScore(score.getOverallScore());
            data.setOverallStatus(score.getOverallStatus());
            data.setModuleScores(score.getModuleScores());

            // Module statistics
            data.setConsentStats(consentService.getStatistics());
            data.setBreachStats(breachService.getStatistics());
            data.setRightsStats(rightsService.getStatistics());
            data.setDpiaStats(dpiaService.getStatistics());
            data.setPolicyStats(policyService.getStatistics());
            data.setUserStats(userService.getStatistics());

            // Alerts and KPIs
            data.setAlerts(generateAlerts());
            data.setKpis(calculateKPIs());

            data.setGeneratedAt(LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Failed to get dashboard data", e);
        }

        return data;
    }

    /**
     * Generate system alerts based on current state
     */
    public List<Alert> generateAlerts() {
        List<Alert> alerts = new ArrayList<>();

        try {
            // Check for overdue breaches
            int overdueBreaches = breachService.getOverdueBreaches().size();
            if (overdueBreaches > 0) {
                alerts.add(new Alert("CRITICAL", "BREACH",
                        String.format("%d breach notifications are overdue", overdueBreaches),
                        "Review and submit DPBI notifications immediately"));
            }

            // Check for overdue rights requests
            int overdueRights = rightsService.getOverdueRequests().size();
            if (overdueRights > 0) {
                alerts.add(new Alert("HIGH", "RIGHTS",
                        String.format("%d rights requests are past deadline", overdueRights),
                        "Complete overdue requests to maintain compliance"));
            }

            // Check for policies needing review
            int overdueReviews = policyService.getPoliciesRequiringReview().size();
            if (overdueReviews > 0) {
                alerts.add(new Alert("MEDIUM", "POLICY",
                        String.format("%d policies require review", overdueReviews),
                        "Schedule policy reviews"));
            }

            // Check for high-risk DPIAs
            DPIAStatistics dpiaStats = dpiaService.getStatistics();
            if (dpiaStats.getHighRiskDPIAs() > 0) {
                alerts.add(new Alert("HIGH", "DPIA",
                        String.format("%d high-risk DPIAs require attention", dpiaStats.getHighRiskDPIAs()),
                        "Review high-risk processing activities"));
            }

            // Consent rate alert
            ConsentStatistics consentStats = consentService.getStatistics();
            if (consentStats.getActiveRate() < 70) {
                alerts.add(new Alert("MEDIUM", "CONSENT",
                        String.format("Active consent rate is %.1f%%", consentStats.getActiveRate()),
                        "Review consent collection processes"));
            }

        } catch (Exception e) {
            logger.error("Failed to generate alerts", e);
        }

        // Sort by severity
        alerts.sort((a, b) -> getSeverityOrder(b.getSeverity()) - getSeverityOrder(a.getSeverity()));

        return alerts;
    }

    private int getSeverityOrder(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    /**
     * Calculate key performance indicators
     */
    public Map<String, KPI> calculateKPIs() {
        Map<String, KPI> kpis = new LinkedHashMap<>();

        try {
            // Consent KPIs
            ConsentStatistics consentStats = consentService.getStatistics();
            kpis.put("active_consents", new KPI("Active Consents",
                    consentStats.getActiveConsents(),
                    consentStats.getTotalConsents(),
                    consentStats.getActiveRate() >= 80 ? "green"
                            : consentStats.getActiveRate() >= 60 ? "amber" : "red"));

            // Breach KPIs
            BreachStatistics breachStats = breachService.getStatistics();
            kpis.put("dpbi_compliance", new KPI("DPBI Compliance Rate",
                    (int) breachStats.getDpbiComplianceRate(), 100,
                    breachStats.getDpbiComplianceRate() >= 95 ? "green"
                            : breachStats.getDpbiComplianceRate() >= 80 ? "amber" : "red"));

            kpis.put("open_breaches", new KPI("Open Breaches",
                    breachStats.getOpenBreaches(), breachStats.getTotalBreaches(),
                    breachStats.getOpenBreaches() == 0 ? "green"
                            : breachStats.getOpenBreaches() <= 2 ? "amber" : "red"));

            // Rights KPIs
            RightsStatistics rightsStats = rightsService.getStatistics();
            kpis.put("rights_compliance", new KPI("DSR Compliance Rate",
                    (int) rightsStats.getComplianceRate(), 100,
                    rightsStats.getComplianceRate() >= 95 ? "green"
                            : rightsStats.getComplianceRate() >= 80 ? "amber" : "red"));

            kpis.put("pending_requests", new KPI("Pending DSRs",
                    rightsStats.getPendingRequests(), rightsStats.getTotalRequests(),
                    rightsStats.getPendingRequests() <= 5 ? "green"
                            : rightsStats.getPendingRequests() <= 15 ? "amber" : "red"));

            // DPIA KPIs
            DPIAStatistics dpiaStats = dpiaService.getStatistics();
            kpis.put("dpia_approved", new KPI("Approved DPIAs",
                    dpiaStats.getApprovedDPIAs(), dpiaStats.getTotalDPIAs(),
                    dpiaStats.getPendingDPIAs() == 0 ? "green" : "amber"));

            // Policy KPIs
            PolicyStatistics policyStats = policyService.getStatistics();
            kpis.put("active_policies", new KPI("Active Policies",
                    policyStats.getActivePolicies(), policyStats.getTotalPolicies(),
                    policyStats.getOverdueReviews() == 0 ? "green" : "amber"));

        } catch (Exception e) {
            logger.error("Failed to calculate KPIs", e);
        }

        return kpis;
    }

    /**
     * Get trend data for charts
     */
    public Map<String, List<TrendPoint>> getTrendData(int days) {
        Map<String, List<TrendPoint>> trends = new HashMap<>();

        // Placeholder - would query database for historical data
        trends.put("compliance_score", generatePlaceholderTrend(days, 75, 85));
        trends.put("consent_count", generatePlaceholderTrend(days, 700, 850));
        trends.put("breach_count", generatePlaceholderTrend(days, 0, 5));
        trends.put("dsr_count", generatePlaceholderTrend(days, 10, 50));

        return trends;
    }

    private List<TrendPoint> generatePlaceholderTrend(int days, int min, int max) {
        List<TrendPoint> points = new ArrayList<>();
        Random rand = new Random(42); // Consistent seed

        for (int i = days; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            int value = min + rand.nextInt(max - min);
            points.add(new TrendPoint(date, value));
        }

        return points;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
