package com.qshield.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByProductOrderByTimestampDesc(String product);
    List<AuditEvent> findByUserIdOrderByTimestampDesc(String userId);
    List<AuditEvent> findByTimestampBetweenOrderByTimestampDesc(Instant from, Instant to);
    Optional<AuditEvent> findFirstByOrderByIdDesc();
    long countByProductAndTimestampAfter(String product, Instant after);
}
