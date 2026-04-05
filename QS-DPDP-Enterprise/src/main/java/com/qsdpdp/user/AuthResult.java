package com.qsdpdp.user;

/**
 * Authentication result
 */
public class AuthResult {
    private boolean success;
    private String message;
    private String sessionToken;

    public AuthResult(boolean success, String message, String sessionToken) {
        this.success = success;
        this.message = message;
        this.sessionToken = sessionToken;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
}
