package com.qshield.soar.repository;

import com.qshield.soar.model.Incident;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    Optional<Incident> findByIncidentId(String incidentId);
    Page<Incident> findByStatusOrderByCreatedAtDesc(String status, Pageable p);
    Page<Incident> findBySeverityOrderByCreatedAtDesc(String severity, Pageable p);
    long countByStatus(String status);
    long countBySeverityAndCreatedAtAfter(String severity, Instant after);
    @Query("SELECT i.status, COUNT(i) FROM Incident i GROUP BY i.status")
    List<Object[]> countByStatus();
    @Query("SELECT i.category, COUNT(i) FROM Incident i GROUP BY i.category")
    List<Object[]> countByCategory();
}
