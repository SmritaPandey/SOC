package com.qsdpdp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * PostgreSQL Database Adapter
 *
 * @version 1.0.0
 * @since Phase B
 */
public class PostgreSQLAdapter implements DatabaseAdapter {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLAdapter.class);

    private String jdbcUrl;
    private Properties connProps;
    private boolean ready = false;

    @Override
    public DatabaseType getType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public String getDisplayName() {
        return "PostgreSQL";
    }

    @Override
    public void initialize(DatabaseConfig config) throws Exception {
        logger.info("Initializing PostgreSQL adapter...");
        Class.forName("org.postgresql.Driver");

        int port = config.getEffectivePort();
        StringBuilder url = new StringBuilder("jdbc:postgresql://")
                .append(config.getHost()).append(":").append(port)
                .append("/").append(config.getName());

        if (config.isSsl()) {
            url.append("?sslmode=").append(config.getSslMode());
        }
        if (!config.getAdditionalParams().isEmpty()) {
            url.append(url.toString().contains("?") ? "&" : "?")
                    .append(config.getAdditionalParams());
        }

        jdbcUrl = url.toString();
        connProps = new Properties();
        connProps.setProperty("user", config.getUsername());
        connProps.setProperty("password", config.getPassword());

        // Test connection
        try (Connection conn = DriverManager.getConnection(jdbcUrl, connProps)) {
            ready = conn.isValid(5);
        }

        logger.info("PostgreSQL adapter ready: {}", config.getHost());
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!ready)
            throw new SQLException("PostgreSQL adapter not initialized");
        return DriverManager.getConnection(jdbcUrl, connProps);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void shutdown() {
        ready = false;
    }

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public String translateDDL(String sqliteDDL) {
        return sqliteDDL
                .replace("INTEGER PRIMARY KEY", "SERIAL PRIMARY KEY")
                .replace("AUTOINCREMENT", "")
                .replace("TEXT", "TEXT") // same
                .replace("INTEGER DEFAULT 0", "BOOLEAN DEFAULT FALSE")
                .replace("INTEGER DEFAULT 1", "BOOLEAN DEFAULT TRUE")
                .replace("REAL", "DOUBLE PRECISION");
    }

    @Override
    public String translateInsertOrIgnore(String sql) {
        return sql.replace("INSERT OR IGNORE", "INSERT INTO")
                .replaceFirst("VALUES", "VALUES") + " ON CONFLICT DO NOTHING";
    }

    @Override
    public String translatePragma(String pragma) {
        return "SELECT 1"; // No pragmas in PostgreSQL
    }

    @Override
    public String translateDatetime(String expr) {
        return expr.replace("datetime(CURRENT_TIMESTAMP, '+30 days')",
                "CURRENT_TIMESTAMP + INTERVAL '30 days'");
    }
}
