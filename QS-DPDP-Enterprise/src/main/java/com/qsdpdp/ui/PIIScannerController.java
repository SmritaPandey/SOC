package com.qsdpdp.ui;

import com.qsdpdp.pii.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller for PII Scanner View
 * 
 * @version 1.0.0
 * @since Phase 6
 */
@Controller
public class PIIScannerController {

    private static final Logger logger = LoggerFactory.getLogger(PIIScannerController.class);

    @Autowired
    private PIIScanner piiScanner;

    // Scan Controls
    @FXML
    private ComboBox<String> scanTypeCombo;
    @FXML
    private TextField sourceField;
    @FXML
    private CheckBox recursiveCheck;
    @FXML
    private CheckBox includeOfficeCheck;
    @FXML
    private CheckBox sensitiveOnlyCheck;
    @FXML
    private HBox progressBox;
    @FXML
    private ProgressBar scanProgress;
    @FXML
    private Label progressLabel;

    // Statistics Labels
    @FXML
    private Label totalScansLabel;
    @FXML
    private Label criticalLabel;
    @FXML
    private Label activeFindingsLabel;
    @FXML
    private Label affectedSourcesLabel;

    // Filters
    @FXML
    private ComboBox<String> riskFilterCombo;
    @FXML
    private ComboBox<String> typeFilterCombo;
    @FXML
    private TextField searchField;

    // Findings Table
    @FXML
    private TableView<PIIFinding> findingsTable;
    @FXML
    private TableColumn<PIIFinding, String> riskColumn;
    @FXML
    private TableColumn<PIIFinding, String> typeColumn;
    @FXML
    private TableColumn<PIIFinding, String> maskedColumn;
    @FXML
    private TableColumn<PIIFinding, String> sourceColumn;
    @FXML
    private TableColumn<PIIFinding, String> lineColumn;
    @FXML
    private TableColumn<PIIFinding, String> confidenceColumn;
    @FXML
    private TableColumn<PIIFinding, String> statusColumn;
    @FXML
    private TableColumn<PIIFinding, Void> actionsColumn;

    // Charts
    @FXML
    private PieChart piiTypeChart;

    // Detail Panel
    @FXML
    private VBox detailsPane;
    @FXML
    private Label detailType;
    @FXML
    private Label detailMasked;
    @FXML
    private Label detailRisk;
    @FXML
    private Label detailSource;
    @FXML
    private Label detailDPDP;
    @FXML
    private TextArea detailContext;

    // Status Bar
    @FXML
    private Label statusLabel;
    @FXML
    private Label lastScanLabel;

    private ObservableList<PIIFinding> findings = FXCollections.observableArrayList();
    private PIIScanResult currentResult;
    private Task<PIIScanResult> currentScanTask;

    @FXML
    public void initialize() {
        logger.info("Initializing PII Scanner Controller");

        // Initialize scanner if needed
        if (!piiScanner.isInitialized()) {
            piiScanner.initialize();
        }

        setupScanTypeCombo();
        setupFilters();
        setupTable();
        refreshStatistics();

        statusLabel.setText("Ready");
    }

    private void setupScanTypeCombo() {
        scanTypeCombo.setItems(FXCollections.observableArrayList(
                "📄 Single File",
                "📁 Directory",
                "📝 Text Input",
                "🗄️ Database Table"));
        scanTypeCombo.getSelectionModel().selectFirst();
    }

    private void setupFilters() {
        riskFilterCombo.setItems(FXCollections.observableArrayList(
                "All Risks", "CRITICAL", "HIGH", "MEDIUM", "LOW"));
        riskFilterCombo.getSelectionModel().selectFirst();

        typeFilterCombo.setItems(FXCollections.observableArrayList(
                "All Types", "AADHAAR", "PAN", "EMAIL", "PHONE",
                "CREDIT_CARD", "BANK_ACCOUNT", "HEALTH_ID"));
        typeFilterCombo.getSelectionModel().selectFirst();

        // Add listeners for filtering
        riskFilterCombo.setOnAction(e -> applyFilters());
        typeFilterCombo.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((obs, old, text) -> applyFilters());
    }

    private void setupTable() {
        riskColumn.setCellValueFactory(data -> new SimpleStringProperty(getRiskBadge(data.getValue().getRiskLevel())));

        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().getDisplayName()));

        maskedColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMaskedValue()));

        sourceColumn.setCellValueFactory(data -> {
            String source = data.getValue().getSourcePath();
            if (source != null && source.length() > 40) {
                source = "..." + source.substring(source.length() - 37);
            }
            return new SimpleStringProperty(source);
        });

        lineColumn
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getLineNumber())));

        confidenceColumn.setCellValueFactory(
                data -> new SimpleStringProperty(String.format("%.0f%%", data.getValue().getConfidence() * 100)));

        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        // Selection listener for details
        findingsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> showFindingDetails(selected));

        findingsTable.setItems(findings);
    }

    private String getRiskBadge(String risk) {
        return switch (risk) {
            case "CRITICAL" -> "🔴 CRITICAL";
            case "HIGH" -> "🟠 HIGH";
            case "MEDIUM" -> "🟡 MEDIUM";
            case "LOW" -> "🟢 LOW";
            default -> risk;
        };
    }

    private void showFindingDetails(PIIFinding finding) {
        if (finding == null) {
            clearDetails();
            return;
        }

        detailType.setText(finding.getType().getDisplayName());
        detailMasked.setText(finding.getMaskedValue());
        detailRisk.setText(finding.getRiskLevel());
        detailSource.setText(finding.getSourcePath());
        detailDPDP.setText(finding.getType().getDPDPSection());
        detailContext.setText(finding.getContext() != null ? finding.getContext() : "N/A");
    }

    private void clearDetails() {
        detailType.setText("");
        detailMasked.setText("");
        detailRisk.setText("");
        detailSource.setText("");
        detailDPDP.setText("");
        detailContext.setText("");
    }

    private void applyFilters() {
        if (currentResult == null)
            return;

        String riskFilter = riskFilterCombo.getValue();
        String typeFilter = typeFilterCombo.getValue();
        String searchText = searchField.getText().toLowerCase();

        findings.setAll(currentResult.getFindings().stream()
                .filter(f -> "All Risks".equals(riskFilter) || f.getRiskLevel().equals(riskFilter))
                .filter(f -> "All Types".equals(typeFilter) || f.getType().name().equals(typeFilter))
                .filter(f -> searchText.isEmpty() ||
                        f.getSourcePath().toLowerCase().contains(searchText) ||
                        f.getMaskedValue().toLowerCase().contains(searchText))
                .toList());
    }

    @FXML
    private void onBrowse() {
        String scanType = scanTypeCombo.getValue();

        if (scanType.contains("Directory")) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select Directory to Scan");
            File dir = chooser.showDialog(sourceField.getScene().getWindow());
            if (dir != null) {
                sourceField.setText(dir.getAbsolutePath());
            }
        } else {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select File to Scan");
            File file = chooser.showOpenDialog(sourceField.getScene().getWindow());
            if (file != null) {
                sourceField.setText(file.getAbsolutePath());
            }
        }
    }

    @FXML
    private void onStartScan() {
        String source = sourceField.getText();
        if (source.isEmpty()) {
            showError("Please enter a source to scan");
            return;
        }

        String scanType = scanTypeCombo.getValue();
        progressBox.setVisible(true);
        statusLabel.setText("Scanning...");
        scanProgress.setProgress(-1);

        currentScanTask = new Task<>() {
            @Override
            protected PIIScanResult call() throws Exception {
                if (scanType.contains("Directory")) {
                    return piiScanner.scanDirectory(Path.of(source), recursiveCheck.isSelected());
                } else if (scanType.contains("Text")) {
                    return piiScanner.scanText(source, "user-input");
                } else if (scanType.contains("Database")) {
                    String[] parts = source.split("\\.");
                    String table = parts[0];
                    String[] columns = parts.length > 1 ? new String[] { parts[1] } : new String[0];
                    return piiScanner.scanDatabaseTable(table, columns);
                } else {
                    return piiScanner.scanFile(Path.of(source));
                }
            }
        };

        currentScanTask.setOnSucceeded(e -> {
            currentResult = currentScanTask.getValue();
            findings.setAll(currentResult.getFindings());
            updateChart();
            refreshStatistics();

            progressBox.setVisible(false);
            statusLabel.setText("Scan completed - " + currentResult.getTotalFindings() + " findings");
            lastScanLabel.setText("Last Scan: " +
                    currentResult.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        currentScanTask.setOnFailed(e -> {
            progressBox.setVisible(false);
            statusLabel.setText("Scan failed");
            showError("Scan failed: " + currentScanTask.getException().getMessage());
        });

        new Thread(currentScanTask).start();
    }

    @FXML
    private void onCancelScan() {
        if (currentScanTask != null && currentScanTask.isRunning()) {
            currentScanTask.cancel();
            progressBox.setVisible(false);
            statusLabel.setText("Scan cancelled");
        }
    }

    @FXML
    private void onRefresh() {
        refreshStatistics();
        statusLabel.setText("Statistics refreshed");
    }

    @FXML
    private void onExportReport() {
        if (currentResult == null) {
            showError("No scan results to export. Run a scan first.");
            return;
        }

        // TODO: Implement report export in Phase 11
        showInfo("Report export will be available in Phase 11 - Reporting Engine");
    }

    @FXML
    private void onClearFilters() {
        riskFilterCombo.getSelectionModel().selectFirst();
        typeFilterCombo.getSelectionModel().selectFirst();
        searchField.clear();
        applyFilters();
    }

    @FXML
    private void onMarkRemediated() {
        PIIFinding selected = findingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setStatus("REMEDIATED");
            findingsTable.refresh();
            showInfo("Finding marked as remediated");
        }
    }

    @FXML
    private void onMarkFalsePositive() {
        PIIFinding selected = findingsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setStatus("FALSE_POSITIVE");
            findingsTable.refresh();
            showInfo("Finding marked as false positive");
        }
    }

    private void refreshStatistics() {
        PIIScanner.PIIScanStatistics stats = piiScanner.getStatistics();

        Platform.runLater(() -> {
            totalScansLabel.setText(String.valueOf(stats.getTotalScans()));
            criticalLabel.setText(String.valueOf(stats.getCriticalFindings()));
            activeFindingsLabel.setText(String.valueOf(stats.getActiveFindings()));
            affectedSourcesLabel.setText(String.valueOf(stats.getAffectedSources()));
        });
    }

    private void updateChart() {
        if (currentResult == null)
            return;

        ObservableList<PieChart.Data> chartData = FXCollections.observableArrayList();

        for (Map.Entry<PIIType, Integer> entry : currentResult.getFindingsByType().entrySet()) {
            chartData.add(new PieChart.Data(
                    entry.getKey().getDisplayName() + " (" + entry.getValue() + ")",
                    entry.getValue()));
        }

        piiTypeChart.setData(chartData);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
