package com.qshield.edr.repository;
import com.qshield.edr.model.EndpointEvent;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
public interface EndpointEventRepository extends JpaRepository<EndpointEvent, Long> {
    Page<EndpointEvent> findByHostnameOrderByTimestampDesc(String hostname, Pageable p);
    long countByTimestampAfter(Instant after);
}
