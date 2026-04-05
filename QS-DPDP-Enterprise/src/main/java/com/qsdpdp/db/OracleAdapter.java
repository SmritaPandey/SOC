package com.qsdpdp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Oracle Database Adapter (Thin Client)
 *
 * @version 1.0.0
 * @since Phase B
 */
public class OracleAdapter implements DatabaseAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OracleAdapter.class);

    private String jdbcUrl;
    private Properties connProps;
    private boolean ready = false;

    @Override
    public DatabaseType getType() {
        return DatabaseType.ORACLE;
    }

    @Override
    public String getDisplayName() {
        return "Oracle Database";
    }

    @Override
    public void initialize(DatabaseConfig config) throws Exception {
        logger.info("Initializing Oracle adapter...");
        Class.forName("oracle.jdbc.OracleDriver");

        int port = config.getEffectivePort();
        jdbcUrl = String.format("jdbc:oracle:thin:@%s:%d:%s",
                config.getHost(), port, config.getName());

        connProps = new Properties();
        connProps.setProperty("user", config.getUsername());
        connProps.setProperty("password", config.getPassword());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, connProps)) {
            ready = conn.isValid(5);
        }

        logger.info("Oracle adapter ready: {}", config.getHost());
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!ready)
            throw new SQLException("Oracle adapter not initialized");
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
                .replace("AUTOINCREMENT", "")
                .replace("TEXT", "VARCHAR2(4000)")
                .replace("REAL", "NUMBER(10,2)")
                .replace("INTEGER", "NUMBER(10)")
                .replace("TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "TIMESTAMP DEFAULT SYSTIMESTAMP");
    }

    @Override
    public String translateInsertOrIgnore(String sql) {
        // Oracle uses MERGE for upserts — simplified version ignores on PK conflict
        return sql.replace("INSERT OR IGNORE", "INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX */");
    }

    @Override
    public String translatePragma(String pragma) {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    public String translateDatetime(String expr) {
        return expr.replace("datetime(CURRENT_TIMESTAMP, '+30 days')",
                "SYSTIMESTAMP + INTERVAL '30' DAY");
    }
}
