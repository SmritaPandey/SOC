package com.qshield.siem.repository;

import com.qshield.siem.model.SiemAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface SiemAlertRepository extends JpaRepository<SiemAlert, Long> {
    Page<SiemAlert> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<SiemAlert> findBySeverityOrderByCreatedAtDesc(String severity, Pageable pageable);
    long countByStatus(String status);
    long countBySeverityAndCreatedAtAfter(String severity, Instant after);

    @Query("SELECT a.status, COUNT(a) FROM SiemAlert a GROUP BY a.status")
    List<Object[]> countByStatus();

    @Query("SELECT a FROM SiemAlert a WHERE a.status = 'NEW' ORDER BY a.createdAt DESC")
    List<SiemAlert> findActiveAlerts(Pageable pageable);
}
