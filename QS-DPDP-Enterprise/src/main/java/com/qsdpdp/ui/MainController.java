package com.qsdpdp.ui;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.security.SecurityManager;
import com.qsdpdp.consent.ConsentService;
import com.qsdpdp.breach.BreachService;
import com.qsdpdp.rights.RightsService;
import com.qsdpdp.dpia.DPIAService;
import com.qsdpdp.policy.PolicyService;
import com.qsdpdp.user.UserService;
import com.qsdpdp.dashboard.DashboardService;
import com.qsdpdp.core.ComplianceEngine;
import com.qsdpdp.rag.RAGEvaluator;
import com.qsdpdp.rules.RuleEngine;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main Window Controller
 * Manages navigation and module loading
 */
public class MainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Core Services
    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final SecurityManager securityManager;

    // Module Services
    private ConsentService consentService;
    private BreachService breachService;
    private RightsService rightsService;
    private DPIAService dpiaService;
    private PolicyService policyService;
    private UserService userService;
    private DashboardService dashboardService;
    private ComplianceEngine complianceEngine;
    private RAGEvaluator ragEvaluator;
    private RuleEngine ruleEngine;

    // FXML Components
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private VBox sideNav;
    @FXML
    private StackPane contentPane;
    @FXML
    private Label statusLabel;
    @FXML
    private Label userLabel;
    @FXML
    private Label complianceScoreLabel;

    // Dashboard widgets
    @FXML
    private Label totalConsentsLabel;
    @FXML
    private Label activeBreachesLabel;
    @FXML
    private Label pendingRequestsLabel;
    @FXML
    private Label activePoliciesLabel;
    @FXML
    private PieChart complianceChart;
    @FXML
    private BarChart<String, Number> moduleScoresChart;

    public MainController(DatabaseManager dbManager, AuditService auditService,
            EventBus eventBus, SecurityManager securityManager) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
        this.securityManager = securityManager;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Main Controller...");

        try {
            initializeServices();
            loadDashboard();
            setupStatusBar();

            logger.info("Main Controller initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize Main Controller", e);
            showError("Initialization Error", "Failed to initialize application: " + e.getMessage());
        }
    }

    private void initializeServices() {
        ragEvaluator = new RAGEvaluator(dbManager);
        ragEvaluator.initialize();

        ruleEngine = new RuleEngine(dbManager, eventBus);
        ruleEngine.initialize();

        complianceEngine = new ComplianceEngine(dbManager, ragEvaluator, eventBus, auditService, ruleEngine);
        complianceEngine.initialize();

        consentService = new ConsentService(dbManager, auditService, eventBus, securityManager);
        consentService.initialize();

        breachService = new BreachService(dbManager, auditService, eventBus);
        breachService.initialize();

        rightsService = new RightsService(dbManager, auditService, eventBus);
        rightsService.initialize();

        dpiaService = new DPIAService(dbManager, auditService, eventBus);
        dpiaService.initialize();

        policyService = new PolicyService(dbManager, auditService, eventBus);
        policyService.initialize();

        userService = new UserService(dbManager, auditService, securityManager);
        userService.initialize();

        dashboardService = new DashboardService(consentService, breachService,
                rightsService, dpiaService, policyService, userService, ragEvaluator, complianceEngine);
    }

    private void loadDashboard() {
        try {
            // Load statistics
            var consentStats = consentService.getStatistics();
            var breachStats = breachService.getStatistics();
            var rightsStats = rightsService.getStatistics();
            var policyStats = policyService.getStatistics();

            if (totalConsentsLabel != null)
                totalConsentsLabel.setText(String.valueOf(consentStats.getTotalConsents()));
            if (activeBreachesLabel != null)
                activeBreachesLabel.setText(String.valueOf(breachStats.getOpenBreaches()));
            if (pendingRequestsLabel != null)
                pendingRequestsLabel.setText(String.valueOf(rightsStats.getPendingRequests()));
            if (activePoliciesLabel != null)
                activePoliciesLabel.setText(String.valueOf(policyStats.getActivePolicies()));

            // Calculate compliance score
            var score = complianceEngine.calculateOverallScore();
            if (complianceScoreLabel != null)
                complianceScoreLabel.setText(String.format("%.0f%%", score.getOverallScore()));

            // Setup charts
            setupComplianceChart(score);
            setupModuleChart();

            statusLabel.setText("Dashboard loaded successfully");

        } catch (Exception e) {
            logger.error("Failed to load dashboard", e);
            statusLabel.setText("Error loading dashboard");
        }
    }

    private void setupComplianceChart(com.qsdpdp.core.ComplianceScore score) {
        if (complianceChart == null)
            return;

        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList(
                new PieChart.Data("Compliant", score.getOverallScore()),
                new PieChart.Data("Gap", 100 - score.getOverallScore()));
        complianceChart.setData(chartData);
        complianceChart.setTitle("Compliance Status");
    }

    private void setupModuleChart() {
        if (moduleScoresChart == null)
            return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Module Scores");

        // Add module scores
        series.getData().add(new XYChart.Data<>("Consent", 85));
        series.getData().add(new XYChart.Data<>("Breach", 78));
        series.getData().add(new XYChart.Data<>("Rights", 92));
        series.getData().add(new XYChart.Data<>("DPIA", 70));
        series.getData().add(new XYChart.Data<>("Policy", 88));
        series.getData().add(new XYChart.Data<>("Security", 95));

        moduleScoresChart.getData().clear();
        moduleScoresChart.getData().add(series);
    }

    private void setupStatusBar() {
        userLabel.setText("System User");
        statusLabel.setText("Ready");
    }

    // Navigation handlers
    @FXML
    private void onDashboardClick() {
        loadDashboard();
        auditService.log("NAV_DASHBOARD", "UI", "SYSTEM", "Navigated to Dashboard");
    }

    @FXML
    private void onConsentClick() {
        statusLabel.setText("Loading Consent Management...");
        auditService.log("NAV_CONSENT", "UI", "SYSTEM", "Navigated to Consent Management");
    }

    @FXML
    private void onBreachClick() {
        statusLabel.setText("Loading Breach Management...");
        auditService.log("NAV_BREACH", "UI", "SYSTEM", "Navigated to Breach Management");
    }

    @FXML
    private void onRightsClick() {
        statusLabel.setText("Loading Rights Requests...");
        auditService.log("NAV_RIGHTS", "UI", "SYSTEM", "Navigated to Rights Requests");
    }

    @FXML
    private void onDPIAClick() {
        statusLabel.setText("Loading DPIA Management...");
        auditService.log("NAV_DPIA", "UI", "SYSTEM", "Navigated to DPIA Management");
    }

    @FXML
    private void onPolicyClick() {
        statusLabel.setText("Loading Policy Management...");
        auditService.log("NAV_POLICY", "UI", "SYSTEM", "Navigated to Policy Management");
    }

    @FXML
    private void onSettingsClick() {
        statusLabel.setText("Loading Settings...");
        auditService.log("NAV_SETTINGS", "UI", "SYSTEM", "Navigated to Settings");
    }

    @FXML
    private void onRefreshClick() {
        loadDashboard();
        statusLabel.setText("Dashboard refreshed");
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
