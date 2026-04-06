package com.qshield.siem.repository;

import com.qshield.siem.model.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    Page<SecurityEvent> findByTimestampBetweenOrderByTimestampDesc(Instant from, Instant to, Pageable pageable);
    Page<SecurityEvent> findBySeverityOrderByTimestampDesc(String severity, Pageable pageable);
    List<SecurityEvent> findBySourceIpAndTimestampAfter(String sourceIp, Instant after);
    long countBySeverity(String severity);
    long countByTimestampAfter(Instant after);

    @Query("SELECT e.severity, COUNT(e) FROM SecurityEvent e WHERE e.timestamp > ?1 GROUP BY e.severity")
    List<Object[]> countBySeverityAfter(Instant after);

    @Query("SELECT e.category, COUNT(e) FROM SecurityEvent e WHERE e.timestamp > ?1 GROUP BY e.category")
    List<Object[]> countByCategoryAfter(Instant after);

    @Query("SELECT e.sourceIp, COUNT(e) FROM SecurityEvent e WHERE e.timestamp > ?1 GROUP BY e.sourceIp ORDER BY COUNT(e) DESC")
    List<Object[]> topSourceIps(Instant after, Pageable pageable);
}
