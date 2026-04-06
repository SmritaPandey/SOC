package com.qshield.soar.repository;

import com.qshield.soar.model.Playbook;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlaybookRepository extends JpaRepository<Playbook, Long> {
    Optional<Playbook> findByPlaybookId(String playbookId);
    List<Playbook> findByEnabledTrue();
    List<Playbook> findByTriggerType(String triggerType);
}
