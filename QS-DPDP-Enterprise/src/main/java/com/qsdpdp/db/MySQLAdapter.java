package com.qsdpdp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * MySQL / MariaDB Database Adapter
 *
 * @version 1.0.0
 * @since Phase B
 */
public class MySQLAdapter implements DatabaseAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MySQLAdapter.class);

    private String jdbcUrl;
    private Properties connProps;
    private boolean ready = false;

    @Override
    public DatabaseType getType() {
        return DatabaseType.MYSQL;
    }

    @Override
    public String getDisplayName() {
        return "MySQL / MariaDB";
    }

    @Override
    public void initialize(DatabaseConfig config) throws Exception {
        logger.info("Initializing MySQL adapter...");
        Class.forName("com.mysql.cj.jdbc.Driver");

        int port = config.getEffectivePort();
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(config.getHost()).append(":").append(port)
                .append("/").append(config.getName())
                .append("?useSSL=").append(config.isSsl())
                .append("&serverTimezone=UTC")
                .append("&allowPublicKeyRetrieval=true");

        if (!config.getAdditionalParams().isEmpty()) {
            url.append("&").append(config.getAdditionalParams());
        }

        jdbcUrl = url.toString();
        connProps = new Properties();
        connProps.setProperty("user", config.getUsername());
        connProps.setProperty("password", config.getPassword());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, connProps)) {
            ready = conn.isValid(5);
        }

        logger.info("MySQL adapter ready: {}", config.getHost());
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!ready)
            throw new SQLException("MySQL adapter not initialized");
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
                .replace("AUTOINCREMENT", "AUTO_INCREMENT")
                .replace("TEXT", "TEXT")
                .replace("REAL", "DOUBLE");
    }

    @Override
    public String translateInsertOrIgnore(String sql) {
        return sql.replace("INSERT OR IGNORE", "INSERT IGNORE");
    }

    @Override
    public String translatePragma(String pragma) {
        return "SELECT 1";
    }

    @Override
    public String translateDatetime(String expr) {
        return expr.replace("datetime(CURRENT_TIMESTAMP, '+30 days')",
                "DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)");
    }
}
