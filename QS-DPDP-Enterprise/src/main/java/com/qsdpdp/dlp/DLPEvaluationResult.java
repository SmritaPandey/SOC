package com.qsdpdp.dlp;

import com.qsdpdp.pii.PIIScanResult;
import com.qsdpdp.pii.PIIType;
import java.util.HashSet;
import java.util.Set;

/**
 * DLP Evaluation Result - outcome of content evaluation against DLP policies
 * 
 * @version 1.0.0
 * @since Module 7
 */
public class DLPEvaluationResult {

    private boolean allowed;
    private DLPAction action;
    private DLPPolicy matchedPolicy;
    private Set<PIIType> matchedDataTypes;
    private DLPIncident incident;
    private PIIScanResult piiResult;
    private String user;
    private String destination;
    private String channel;
    private String message;

    public DLPEvaluationResult() {
        this.allowed = true;
        this.action = DLPAction.ALLOW;
        this.matchedDataTypes = new HashSet<>();
    }

    public static DLPEvaluationResult allowed() {
        DLPEvaluationResult result = new DLPEvaluationResult();
        result.setAllowed(true);
        result.setAction(DLPAction.ALLOW);
        return result;
    }

    public static DLPEvaluationResult blocked(DLPPolicy policy, String reason) {
        DLPEvaluationResult result = new DLPEvaluationResult();
        result.setAllowed(false);
        result.setAction(DLPAction.BLOCK);
        result.setMatchedPolicy(policy);
        result.setMessage(reason);
        return result;
    }

    public boolean isAllowed() { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }
    public DLPAction getAction() { return action; }
    public void setAction(DLPAction action) { this.action = action; }
    public DLPPolicy getMatchedPolicy() { return matchedPolicy; }
    public void setMatchedPolicy(DLPPolicy policy) { this.matchedPolicy = policy; }
    public Set<PIIType> getMatchedDataTypes() { return matchedDataTypes; }
    public void setMatchedDataTypes(Set<PIIType> types) { this.matchedDataTypes = types; }
    public DLPIncident getIncident() { return incident; }
    public void setIncident(DLPIncident incident) { this.incident = incident; }
    public PIIScanResult getPiiResult() { return piiResult; }
    public void setPiiResult(PIIScanResult result) { this.piiResult = result; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
