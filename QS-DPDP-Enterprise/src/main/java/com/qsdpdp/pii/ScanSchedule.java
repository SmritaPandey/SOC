package com.qsdpdp.pii;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Scan Schedule - Cron-like scheduling for recurring PII scans
 * Supports daily, weekly, monthly, and custom interval scheduling
 * 
 * @version 2.0.0
 * @since Phase 7
 */
public class ScanSchedule {

    private String id;
    private Frequency frequency;
    private LocalTime timeOfDay;
    private Set<DayOfWeek> daysOfWeek;
    private int dayOfMonth; // 1-31, for MONTHLY
    private int intervalHours; // for CUSTOM_INTERVAL
    private boolean enabled;
    private LocalDateTime lastRun;
    private LocalDateTime nextRun;
    private int executionCount;
    private int maxExecutions; // 0 = unlimited
    private boolean notifyOnComplete;
    private boolean notifyOnFindings;

    public enum Frequency {
        HOURLY("Every hour"),
        EVERY_4_HOURS("Every 4 hours"),
        EVERY_8_HOURS("Every 8 hours"),
        DAILY("Once daily"),
        WEEKLY("Once per week"),
        BIWEEKLY("Every two weeks"),
        MONTHLY("Once per month"),
        QUARTERLY("Every 3 months"),
        CUSTOM_INTERVAL("Custom interval");

        private final String displayName;
        Frequency(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public ScanSchedule() {
        this.id = UUID.randomUUID().toString();
        this.frequency = Frequency.DAILY;
        this.timeOfDay = LocalTime.of(2, 0); // 2:00 AM default
        this.daysOfWeek = new HashSet<>();
        this.dayOfMonth = 1;
        this.intervalHours = 24;
        this.enabled = true;
        this.executionCount = 0;
        this.maxExecutions = 0;
        this.notifyOnComplete = true;
        this.notifyOnFindings = true;
    }

    // ═══════════════════════════════════════════════════════════
    // FACTORY METHODS
    // ═══════════════════════════════════════════════════════════

    public static ScanSchedule daily(LocalTime time) {
        ScanSchedule s = new ScanSchedule();
        s.setFrequency(Frequency.DAILY);
        s.setTimeOfDay(time);
        return s;
    }

    public static ScanSchedule weekly(DayOfWeek day, LocalTime time) {
        ScanSchedule s = new ScanSchedule();
        s.setFrequency(Frequency.WEEKLY);
        s.setTimeOfDay(time);
        s.getDaysOfWeek().add(day);
        return s;
    }

    public static ScanSchedule monthly(int dayOfMonth, LocalTime time) {
        ScanSchedule s = new ScanSchedule();
        s.setFrequency(Frequency.MONTHLY);
        s.setDayOfMonth(dayOfMonth);
        s.setTimeOfDay(time);
        return s;
    }

    public static ScanSchedule everyNHours(int hours) {
        ScanSchedule s = new ScanSchedule();
        s.setFrequency(Frequency.CUSTOM_INTERVAL);
        s.setIntervalHours(hours);
        return s;
    }

    /**
     * Compute next run time from current time
     */
    public LocalDateTime computeNextRun(LocalDateTime from) {
        return switch (frequency) {
            case HOURLY -> from.plusHours(1).withMinute(0).withSecond(0);
            case EVERY_4_HOURS -> from.plusHours(4).withMinute(0).withSecond(0);
            case EVERY_8_HOURS -> from.plusHours(8).withMinute(0).withSecond(0);
            case DAILY -> from.plusDays(1).with(timeOfDay);
            case WEEKLY -> {
                LocalDateTime next = from.plusWeeks(1).with(timeOfDay);
                if (!daysOfWeek.isEmpty()) {
                    DayOfWeek target = daysOfWeek.iterator().next();
                    next = from.with(java.time.temporal.TemporalAdjusters.next(target)).with(timeOfDay);
                }
                yield next;
            }
            case BIWEEKLY -> from.plusWeeks(2).with(timeOfDay);
            case MONTHLY -> from.plusMonths(1).withDayOfMonth(Math.min(dayOfMonth, 28)).with(timeOfDay);
            case QUARTERLY -> from.plusMonths(3).withDayOfMonth(1).with(timeOfDay);
            case CUSTOM_INTERVAL -> from.plusHours(intervalHours);
        };
    }

    /**
     * Check if another execution is allowed (based on maxExecutions)
     */
    public boolean canExecute() {
        if (!enabled) return false;
        if (maxExecutions > 0 && executionCount >= maxExecutions) return false;
        return true;
    }

    /**
     * Check if schedule is due now
     */
    public boolean isDue(LocalDateTime now) {
        if (!canExecute()) return false;
        if (nextRun == null) {
            nextRun = computeNextRun(now.minusHours(intervalHours > 0 ? intervalHours : 24));
        }
        return now.isAfter(nextRun) || now.isEqual(nextRun);
    }

    /**
     * Mark execution completed, advance next run
     */
    public void markExecuted() {
        this.lastRun = LocalDateTime.now();
        this.executionCount++;
        this.nextRun = computeNextRun(this.lastRun);
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }

    public LocalTime getTimeOfDay() { return timeOfDay; }
    public void setTimeOfDay(LocalTime timeOfDay) { this.timeOfDay = timeOfDay; }

    public Set<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public int getIntervalHours() { return intervalHours; }
    public void setIntervalHours(int intervalHours) { this.intervalHours = intervalHours; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getLastRun() { return lastRun; }
    public void setLastRun(LocalDateTime lastRun) { this.lastRun = lastRun; }

    public LocalDateTime getNextRun() { return nextRun; }
    public void setNextRun(LocalDateTime nextRun) { this.nextRun = nextRun; }

    public int getExecutionCount() { return executionCount; }
    public int getMaxExecutions() { return maxExecutions; }
    public void setMaxExecutions(int maxExecutions) { this.maxExecutions = maxExecutions; }

    public boolean isNotifyOnComplete() { return notifyOnComplete; }
    public void setNotifyOnComplete(boolean notify) { this.notifyOnComplete = notify; }

    public boolean isNotifyOnFindings() { return notifyOnFindings; }
    public void setNotifyOnFindings(boolean notify) { this.notifyOnFindings = notify; }
}
