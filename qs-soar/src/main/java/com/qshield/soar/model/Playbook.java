package com.qshield.soar.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * SOAR Playbook — automated response workflow.
 * Implements NIST IR-4(1) Automated Incident Handling.
 */
@Entity
@Table(name = "playbooks")
public class Playbook {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String playbookId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 64)
    private String triggerType; // ALERT_SEVERITY, CATEGORY, MANUAL, SCHEDULED

    @Column(length = 128)
    private String triggerCondition; // e.g., "severity=CRITICAL AND category=MALWARE"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String actionsJson; // JSON array of ordered actions

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column
    private Instant lastExecuted;

    @Column
    private Integer executionCount = 0;

    @Column
    private Integer avgExecutionTimeMs;

    public Playbook() {}
    public Playbook(String playbookId, String name, String triggerType, String actionsJson) {
        this.playbookId = playbookId; this.name = name; this.triggerType = triggerType; this.actionsJson = actionsJson;
    }

    public Long getId() { return id; }
    public String getPlaybookId() { return playbookId; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getTriggerType() { return triggerType; }
    public String getTriggerCondition() { return triggerCondition; }
    public void setTriggerCondition(String t) { this.triggerCondition = t; }
    public String getActionsJson() { return actionsJson; }
    public void setActionsJson(String a) { this.actionsJson = a; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean e) { this.enabled = e; }
    public Instant getLastExecuted() { return lastExecuted; }
    public void setLastExecuted(Instant l) { this.lastExecuted = l; }
    public Integer getExecutionCount() { return executionCount; }
    public void setExecutionCount(Integer c) { this.executionCount = c; }
    public Integer getAvgExecutionTimeMs() { return avgExecutionTimeMs; }
    public void setAvgExecutionTimeMs(Integer a) { this.avgExecutionTimeMs = a; }
}
