package com.qsdpdp.db;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Database configuration holder — reads from application.yml
 *
 * @version 1.0.0
 * @since Phase B
 */
@Component
@ConfigurationProperties(prefix = "qsdpdp.database")
public class DatabaseConfig {

    private String type = "sqlite"; // sqlite | postgresql | mysql | oracle | mssql
    private String host = "localhost";
    private int port = 0; // 0 = use default for DB type
    private String name = "qsdpdp_enterprise";
    private String username = "";
    private String password = "";
    private String schema = "public";
    private String dataDir = ""; // SQLite: directory for .db file
    private int maxPoolSize = 10;
    private int minIdle = 2;
    private int connectionTimeoutMs = 30000;
    private boolean ssl = false;
    private String sslMode = "prefer";
    private String additionalParams = "";

    // Flyway
    private boolean flywayEnabled = false;
    private String flywayLocations = "classpath:db/migration";

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int ms) {
        this.connectionTimeoutMs = ms;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getSslMode() {
        return sslMode;
    }

    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }

    public String getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(String p) {
        this.additionalParams = p;
    }

    public boolean isFlywayEnabled() {
        return flywayEnabled;
    }

    public void setFlywayEnabled(boolean b) {
        this.flywayEnabled = b;
    }

    public String getFlywayLocations() {
        return flywayLocations;
    }

    public void setFlywayLocations(String loc) {
        this.flywayLocations = loc;
    }

    /** Resolve the effective port (use DB-type default if zero) */
    public int getEffectivePort() {
        if (port > 0)
            return port;
        return switch (type.toLowerCase()) {
            case "postgresql" -> 5432;
            case "mysql" -> 3306;
            case "oracle" -> 1521;
            case "mssql" -> 1433;
            default -> 0;
        };
    }

    /** Get the DatabaseAdapter.DatabaseType enum */
    public DatabaseAdapter.DatabaseType getDatabaseType() {
        return switch (type.toLowerCase()) {
            case "postgresql", "postgres" -> DatabaseAdapter.DatabaseType.POSTGRESQL;
            case "mysql", "mariadb" -> DatabaseAdapter.DatabaseType.MYSQL;
            case "oracle" -> DatabaseAdapter.DatabaseType.ORACLE;
            case "mssql", "sqlserver" -> DatabaseAdapter.DatabaseType.MSSQL;
            default -> DatabaseAdapter.DatabaseType.SQLITE;
        };
    }
}
