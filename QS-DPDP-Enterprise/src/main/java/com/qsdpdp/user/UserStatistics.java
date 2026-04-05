package com.qsdpdp.user;

/**
 * User statistics
 */
public class UserStatistics {
    private int totalUsers;
    private int activeUsers;
    private int lockedUsers;
    private int mfaEnabledUsers;

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getLockedUsers() {
        return lockedUsers;
    }

    public void setLockedUsers(int lockedUsers) {
        this.lockedUsers = lockedUsers;
    }

    public int getMfaEnabledUsers() {
        return mfaEnabledUsers;
    }

    public void setMfaEnabledUsers(int mfaEnabledUsers) {
        this.mfaEnabledUsers = mfaEnabledUsers;
    }
}
