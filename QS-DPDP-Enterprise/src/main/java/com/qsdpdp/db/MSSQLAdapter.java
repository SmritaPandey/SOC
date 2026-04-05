package com.qsdpdp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Microsoft SQL Server Database Adapter
 *
 * @version 1.0.0
 * @since Phase B
 */
public class MSSQLAdapter implements DatabaseAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MSSQLAdapter.class);

    private String jdbcUrl;
    private Properties connProps;
    private boolean ready = false;

    @Override
    public DatabaseType getType() {
        return DatabaseType.MSSQL;
    }

    @Override
    public String getDisplayName() {
        return "Microsoft SQL Server";
    }

    @Override
    public void initialize(DatabaseConfig config) throws Exception {
        logger.info("Initializing MS SQL Server adapter...");
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

        int port = config.getEffectivePort();
        StringBuilder url = new StringBuilder("jdbc:sqlserver://")
                .append(config.getHost()).append(":").append(port)
                .append(";databaseName=").append(config.getName())
                .append(";encrypt=").append(config.isSsl())
                .append(";trustServerCertificate=true");

        if (!config.getAdditionalParams().isEmpty()) {
            url.append(";").append(config.getAdditionalParams());
        }

        jdbcUrl = url.toString();
        connProps = new Properties();
        connProps.setProperty("user", config.getUsername());
        connProps.setProperty("password", config.getPassword());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, connProps)) {
            ready = conn.isValid(5);
        }

        logger.info("MS SQL Server adapter ready: {}", config.getHost());
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!ready)
            throw new SQLException("MSSQL adapter not initialized");
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
                .replace("AUTOINCREMENT", "IDENTITY(1,1)")
                .replace("TEXT", "NVARCHAR(MAX)")
                .replace("REAL", "FLOAT")
                .replace("INTEGER DEFAULT 0", "BIT DEFAULT 0")
                .replace("INTEGER DEFAULT 1", "BIT DEFAULT 1");
    }

    @Override
    public String translateInsertOrIgnore(String sql) {
        // MSSQL uses WHERE NOT EXISTS pattern
        return sql.replace("INSERT OR IGNORE", "INSERT");
    }

    @Override
    public String translatePragma(String pragma) {
        return "SELECT 1";
    }

    @Override
    public String translateDatetime(String expr) {
        return expr.replace("datetime(CURRENT_TIMESTAMP, '+30 days')",
                "DATEADD(DAY, 30, GETDATE())");
    }
}
