package com.qsdpdp.api;

import java.util.*;

/**
 * API Request model
 * 
 * @version 1.0.0
 * @since Module 13
 */
public class APIRequest {

    private String apiKey;
    private String endpoint;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, Object> body;
    private String ipAddress;
    private String userAgent;

    public APIRequest() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        this.body = new HashMap<>();
    }

    public static APIRequest get(String endpoint) {
        APIRequest req = new APIRequest();
        req.endpoint = endpoint;
        req.method = "GET";
        return req;
    }

    public static APIRequest post(String endpoint, Map<String, Object> body) {
        APIRequest req = new APIRequest();
        req.endpoint = endpoint;
        req.method = "POST";
        req.body = body;
        return req;
    }

    public APIRequest withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getQueryParam(String key) {
        return queryParams.get(key);
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public void setBody(Map<String, Object> body) {
        this.body = body;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
