package com.qshield.xdr.repository;
import com.qshield.xdr.model.XdrCorrelation;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface XdrCorrelationRepository extends JpaRepository<XdrCorrelation, Long> {
    Page<XdrCorrelation> findByStatusOrderByTimestampDesc(String status, Pageable p);
    long countByStatus(String status);
}
