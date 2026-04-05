package com.qsdpdp.api;

import java.util.*;

/**
 * API Response model
 * 
 * @version 1.0.0
 * @since Module 13
 */
public class APIResponse {

    private String requestId;
    private int statusCode;
    private boolean success;
    private String message;
    private Map<String, Object> data;
    private String error;
    private Map<String, Object> meta;

    public APIResponse() {
        this.data = new HashMap<>();
        this.meta = new HashMap<>();
    }

    public APIResponse(int statusCode, boolean success, String message) {
        this();
        this.statusCode = statusCode;
        this.success = success;
        this.message = message;
    }

    public static APIResponse success(Map<String, Object> data) {
        APIResponse resp = new APIResponse(200, true, "Success");
        resp.data = data;
        return resp;
    }

    public static APIResponse created(Map<String, Object> data) {
        APIResponse resp = new APIResponse(201, true, "Created");
        resp.data = data;
        return resp;
    }

    public static APIResponse badRequest(String message) {
        return new APIResponse(400, false, message);
    }

    public static APIResponse unauthorized(String message) {
        APIResponse resp = new APIResponse(401, false, message);
        resp.error = "UNAUTHORIZED";
        return resp;
    }

    public static APIResponse forbidden(String message) {
        APIResponse resp = new APIResponse(403, false, message);
        resp.error = "FORBIDDEN";
        return resp;
    }

    public static APIResponse notFound(String message) {
        APIResponse resp = new APIResponse(404, false, message);
        resp.error = "NOT_FOUND";
        return resp;
    }

    public static APIResponse tooManyRequests(String message) {
        APIResponse resp = new APIResponse(429, false, message);
        resp.error = "RATE_LIMITED";
        return resp;
    }

    public static APIResponse error(int statusCode, String message) {
        APIResponse resp = new APIResponse(statusCode, false, message);
        resp.error = "INTERNAL_ERROR";
        return resp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
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

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }
}
