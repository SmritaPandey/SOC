package com.qshield.av.repository;
import com.qshield.av.model.ScanResult;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface ScanResultRepository extends JpaRepository<ScanResult, Long> {
    Page<ScanResult> findByVerdictOrderByTimestampDesc(String verdict, Pageable p);
    long countByVerdict(String verdict);
}
