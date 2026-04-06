package com.qshield.edr.repository;
import com.qshield.edr.model.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface EndpointRepository extends JpaRepository<Endpoint, Long> {
    Optional<Endpoint> findByHostname(String hostname);
    long countByStatus(String status);
}
