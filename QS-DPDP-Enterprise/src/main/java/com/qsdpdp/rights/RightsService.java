package com.qsdpdp.rights;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Rights Request Service (DSR Management)
 * Handles data subject rights per DPDP Act 2023
 * Mandatory 30-day response deadline
 * 
 * @version 1.0.0
 * @since Phase 2
 */
@Service
public class RightsService {

    private static final Logger logger = LoggerFactory.getLogger(RightsService.class);

    private static final int RESPONSE_DEADLINE_DAYS = 30;

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;

    private boolean initialized = false;
    private int requestCounter = 0;

    @Autowired
    public RightsService(DatabaseManager dbManager, AuditService auditService, EventBus eventBus) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing Rights Service...");
        loadRequestCounter();
        eventBus.subscribe("rights.*", this::handleRightsEvent);
        initialized = true;
        logger.info("Rights Service initialized");
    }

    private void loadRequestCounter() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rights_requests");
            if (rs.next())
                requestCounter = rs.getInt(1);
        } catch (Exception e) {
            logger.warn("Could not load request counter", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REQUEST MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public RightsRequest submitRequest(RightsRequestDTO dto) {
        logger.info("Submitting rights request: {} for principal {}",
                dto.getRequestType(), dto.getDataPrincipalId());

        String refNumber = generateReferenceNumber();

        RightsRequest request = RightsRequest.builder()
                .id(UUID.randomUUID().toString())
                .referenceNumber(refNumber)
                .dataPrincipalId(dto.getDataPrincipalId())
                .requestType(dto.getRequestType())
                .description(dto.getDescription())
                .priority(dto.getPriority())
                .build();

        request.setDeadline(LocalDateTime.now().plusDays(RESPONSE_DEADLINE_DAYS));

        saveRequest(request);

        auditService.log("RIGHTS_REQUEST_SUBMITTED", "RIGHTS", dto.getActorId(),
                String.format("Rights request %s: type=%s principal=%s",
                        refNumber, dto.getRequestType(), dto.getDataPrincipalId()));

        eventBus.publish(new ComplianceEvent("rights.request_submitted",
                Map.of("requestId", request.getId(), "type", dto.getRequestType().name())));

        logger.info("Rights request submitted: {}", refNumber);
        return request;
    }

    private String generateReferenceNumber() {
        requestCounter++;
        return String.format("DSR-%d-%05d", LocalDateTime.now().getYear(), requestCounter);
    }

    private void saveRequest(RightsRequest request) {
        String sql = """
                    INSERT INTO rights_requests (id, reference_number, data_principal_id, request_type,
                        description, status, priority, assigned_to, received_at, deadline)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, request.getId());
            stmt.setString(2, request.getReferenceNumber());
            stmt.setString(3, request.getDataPrincipalId());
            stmt.setString(4, request.getRequestType().name());
            stmt.setString(5, request.getDescription());
            stmt.setString(6, request.getStatus().name());
            stmt.setString(7, request.getPriority().name());
            stmt.setString(8, request.getAssignedTo());
            stmt.setString(9, request.getReceivedAt().toString());
            stmt.setString(10, request.getDeadline().toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save rights request", e);
        }
    }

    public RightsRequest acknowledgeRequest(String requestId, String actorId) {
        RightsRequest request = getRequestById(requestId);
        if (request == null)
            throw new IllegalArgumentException("Request not found");

        request.setStatus(RequestStatus.ACKNOWLEDGED);
        request.setAcknowledgedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        updateRequest(request, "status", request.getStatus().name());
        updateRequest(request, "acknowledged_at", request.getAcknowledgedAt().toString());

        auditService.log("RIGHTS_REQUEST_ACKNOWLEDGED", "RIGHTS", actorId,
                "Acknowledged request: " + request.getReferenceNumber());

        return request;
    }

    public RightsRequest assignRequest(String requestId, String assignee, String actorId) {
        RightsRequest request = getRequestById(requestId);
        if (request == null)
            throw new IllegalArgumentException("Request not found");

        request.setAssignedTo(assignee);
        request.setStatus(RequestStatus.IN_PROGRESS);
        request.setUpdatedAt(LocalDateTime.now());

        String sql = "UPDATE rights_requests SET assigned_to = ?, status = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assignee);
            stmt.setString(2, RequestStatus.IN_PROGRESS.name());
            stmt.setString(3, request.getUpdatedAt().toString());
            stmt.setString(4, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to assign request", e);
        }

        auditService.log("RIGHTS_REQUEST_ASSIGNED", "RIGHTS", actorId,
                String.format("Assigned request %s to %s", request.getReferenceNumber(), assignee));

        return request;
    }

    public RightsRequest completeRequest(String requestId, String response, String evidencePackage, String actorId) {
        RightsRequest request = getRequestById(requestId);
        if (request == null)
            throw new IllegalArgumentException("Request not found");

        request.setStatus(RequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        request.setResponse(response);
        request.setEvidencePackage(evidencePackage);
        request.setUpdatedAt(LocalDateTime.now());

        String sql = "UPDATE rights_requests SET status = ?, completed_at = ?, response = ?, evidence_package = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, RequestStatus.COMPLETED.name());
            stmt.setString(2, request.getCompletedAt().toString());
            stmt.setString(3, response);
            stmt.setString(4, evidencePackage);
            stmt.setString(5, request.getUpdatedAt().toString());
            stmt.setString(6, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to complete request", e);
        }

        auditService.log("RIGHTS_REQUEST_COMPLETED", "RIGHTS", actorId,
                String.format("Completed request %s", request.getReferenceNumber()));

        eventBus.publish(new ComplianceEvent("rights.request_completed",
                Map.of("requestId", requestId, "type", request.getRequestType().name())));

        return request;
    }

    public RightsRequest rejectRequest(String requestId, String reason, String actorId) {
        RightsRequest request = getRequestById(requestId);
        if (request == null)
            throw new IllegalArgumentException("Request not found");

        request.setStatus(RequestStatus.REJECTED);
        request.setResponse(reason);
        request.setCompletedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        String sql = "UPDATE rights_requests SET status = ?, completed_at = ?, response = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, RequestStatus.REJECTED.name());
            stmt.setString(2, request.getCompletedAt().toString());
            stmt.setString(3, reason);
            stmt.setString(4, request.getUpdatedAt().toString());
            stmt.setString(5, requestId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reject request", e);
        }

        auditService.log("RIGHTS_REQUEST_REJECTED", "RIGHTS", actorId,
                String.format("Rejected request %s: %s", request.getReferenceNumber(), reason));

        return request;
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════

    public RightsRequest getRequestById(String id) {
        String sql = """
                    SELECT r.*, dp.name as principal_name, dp.email as principal_email
                    FROM rights_requests r
                    LEFT JOIN data_principals dp ON r.data_principal_id = dp.id
                    WHERE r.id = ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return mapRequest(rs);
        } catch (SQLException e) {
            logger.error("Failed to get request", e);
        }
        return null;
    }

    public List<RightsRequest> getPendingRequests() {
        return getRequestsByStatus(
                List.of(RequestStatus.PENDING, RequestStatus.ACKNOWLEDGED, RequestStatus.IN_PROGRESS));
    }

    public List<RightsRequest> getOverdueRequests() {
        List<RightsRequest> requests = new ArrayList<>();
        String sql = """
                    SELECT r.*, dp.name as principal_name, dp.email as principal_email
                    FROM rights_requests r
                    LEFT JOIN data_principals dp ON r.data_principal_id = dp.id
                    WHERE r.status NOT IN ('COMPLETED', 'REJECTED', 'WITHDRAWN')
                    AND r.deadline < datetime('now')
                    ORDER BY r.deadline ASC
                """;

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next())
                requests.add(mapRequest(rs));
        } catch (SQLException e) {
            logger.error("Failed to get overdue requests", e);
        }
        return requests;
    }

    public List<RightsRequest> getRequestsByStatus(List<RequestStatus> statuses) {
        List<RightsRequest> requests = new ArrayList<>();
        String placeholders = String.join(",", Collections.nCopies(statuses.size(), "?"));
        String sql = """
                    SELECT r.*, dp.name as principal_name, dp.email as principal_email
                    FROM rights_requests r
                    LEFT JOIN data_principals dp ON r.data_principal_id = dp.id
                    WHERE r.status IN (%s)
                    ORDER BY r.deadline ASC
                """.formatted(placeholders);

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < statuses.size(); i++) {
                stmt.setString(i + 1, statuses.get(i).name());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                requests.add(mapRequest(rs));
        } catch (SQLException e) {
            logger.error("Failed to get requests by status", e);
        }
        return requests;
    }

    public List<RightsRequest> getAllRequests(int offset, int limit) {
        List<RightsRequest> requests = new ArrayList<>();
        String sql = """
                    SELECT r.*, dp.name as principal_name, dp.email as principal_email
                    FROM rights_requests r
                    LEFT JOIN data_principals dp ON r.data_principal_id = dp.id
                    ORDER BY r.received_at DESC
                    LIMIT ? OFFSET ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                requests.add(mapRequest(rs));
        } catch (SQLException e) {
            logger.error("Failed to get requests", e);
        }
        return requests;
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    public RightsStatistics getStatistics() {
        RightsStatistics stats = new RightsStatistics();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM rights_requests");
            if (rs.next())
                stats.setTotalRequests(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM rights_requests WHERE status NOT IN ('COMPLETED', 'REJECTED', 'WITHDRAWN')");
            if (rs.next())
                stats.setPendingRequests(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM rights_requests WHERE status = 'COMPLETED'");
            if (rs.next())
                stats.setCompletedRequests(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM rights_requests WHERE status NOT IN ('COMPLETED', 'REJECTED', 'WITHDRAWN') AND deadline < datetime('now')");
            if (rs.next())
                stats.setOverdueRequests(rs.getInt(1));

            // Compliance rate (completed within deadline)
            rs = stmt.executeQuery(
                    """
                                SELECT COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM rights_requests WHERE status = 'COMPLETED'), 0)
                                FROM rights_requests
                                WHERE status = 'COMPLETED' AND completed_at <= deadline
                            """);
            if (rs.next())
                stats.setComplianceRate(rs.getDouble(1));

            // Average resolution days
            rs = stmt.executeQuery("""
                        SELECT AVG(julianday(completed_at) - julianday(received_at))
                        FROM rights_requests WHERE completed_at IS NOT NULL
                    """);
            if (rs.next())
                stats.setAvgResolutionDays(rs.getDouble(1));

            // By type breakdown
            Map<String, Integer> byType = new HashMap<>();
            rs = stmt.executeQuery("SELECT request_type, COUNT(*) FROM rights_requests GROUP BY request_type");
            while (rs.next()) {
                byType.put(rs.getString(1), rs.getInt(2));
            }
            stats.setRequestsByType(byType);

        } catch (SQLException e) {
            logger.error("Failed to get rights statistics", e);
        }

        return stats;
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private void updateRequest(RightsRequest request, String field, String value) {
        String sql = "UPDATE rights_requests SET " + field + " = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.setString(2, LocalDateTime.now().toString());
            stmt.setString(3, request.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update request", e);
        }
    }

    private static LocalDateTime parseDateTime(String value) {
        if (value == null) return null;
        // SQLite may store datetime with space separator instead of 'T'
        return LocalDateTime.parse(value.replace(' ', 'T'));
    }

    private RightsRequest mapRequest(ResultSet rs) throws SQLException {
        RightsRequest request = new RightsRequest();
        request.setId(rs.getString("id"));
        request.setReferenceNumber(rs.getString("reference_number"));
        request.setDataPrincipalId(rs.getString("data_principal_id"));
        request.setRequestType(RightType.valueOf(rs.getString("request_type")));
        request.setDescription(rs.getString("description"));
        request.setStatus(RequestStatus.valueOf(rs.getString("status")));
        request.setPriority(RequestPriority.valueOf(rs.getString("priority")));
        request.setAssignedTo(rs.getString("assigned_to"));

        request.setReceivedAt(parseDateTime(rs.getString("received_at")));
        request.setAcknowledgedAt(parseDateTime(rs.getString("acknowledged_at")));
        request.setDeadline(parseDateTime(rs.getString("deadline")));
        request.setCompletedAt(parseDateTime(rs.getString("completed_at")));

        request.setResponse(rs.getString("response"));
        request.setEvidencePackage(rs.getString("evidence_package"));
        request.setNotes(rs.getString("notes"));

        try {
            request.setDataPrincipalName(rs.getString("principal_name"));
            request.setDataPrincipalEmail(rs.getString("principal_email"));
        } catch (SQLException e) {
            // Joined columns may not exist
        }

        return request;
    }

    private void handleRightsEvent(ComplianceEvent event) {
        logger.debug("Handling rights event: {}", event.getType());
    }

    public boolean isInitialized() {
        return initialized;
    }
}
