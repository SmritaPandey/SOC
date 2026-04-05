package com.qsdpdp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * SQLite Database Adapter — Zero-configuration default
 *
 * @version 1.0.0
 * @since Phase B
 */
public class SQLiteAdapter implements DatabaseAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteAdapter.class);

    private String jdbcUrl;
    private String dbPath;
    private boolean ready = false;

    @Override
    public DatabaseType getType() {
        return DatabaseType.SQLITE;
    }

    @Override
    public String getDisplayName() {
        return "SQLite 3.44";
    }

    @Override
    public void initialize(DatabaseConfig config) throws Exception {
        logger.info("Initializing SQLite adapter...");
        Class.forName("org.sqlite.JDBC");

        // Determine db path
        String dataDir = config.getDataDir();
        if (dataDir == null || dataDir.isEmpty()) {
            dataDir = getDefaultDataDir();
        }

        dbPath = dataDir + File.separator + config.getName() + ".db";
        jdbcUrl = "jdbc:sqlite:" + dbPath;

        // Create file if needed
        File dbFile = new File(dbPath);
        if (dbFile.getParentFile() != null) {
            dbFile.getParentFile().mkdirs();
        }
        if (!dbFile.exists()) {
            dbFile.createNewFile();
        }

        ready = true;
        logger.info("SQLite adapter ready: {}", dbPath);
    }

    private String getDefaultDataDir() {
        String env = System.getenv("QSDPDP_DATA_DIR");
        if (env != null && !env.isEmpty())
            return env;

        String userHome = System.getProperty("user.home");
        Path p = Paths.get(userHome, ".qsdpdp-enterprise", "data");
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            return ".";
        }
        return p.toString();
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!ready)
            throw new SQLException("SQLite adapter not initialized");
        Connection conn = DriverManager.getConnection(jdbcUrl);
        conn.setAutoCommit(true);
        return conn;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void shutdown() {
        ready = false;
        logger.info("SQLite adapter shut down");
    }

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getDbPath() {
        return dbPath;
    }
}
