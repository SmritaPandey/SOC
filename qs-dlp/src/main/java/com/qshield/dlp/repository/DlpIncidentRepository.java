package com.qshield.dlp.repository;
import com.qshield.dlp.model.DlpIncident;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface DlpIncidentRepository extends JpaRepository<DlpIncident, Long> {
    Page<DlpIncident> findByStatusOrderByTimestampDesc(String status, Pageable p);
    long countByStatus(String status);
}
