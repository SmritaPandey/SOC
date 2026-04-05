package com.qsdpdp.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database Adapter Interface — Multi-Database Abstraction Layer
 * Supports SQLite (default), PostgreSQL, MySQL/MariaDB, Oracle, MS SQL Server
 *
 * @version 1.0.0
 * @since Phase B
 */
public interface DatabaseAdapter {

    /** Database type identifier */
    enum DatabaseType {
        SQLITE, POSTGRESQL, MYSQL, ORACLE, MSSQL
    }

    /** Get the database type */
    DatabaseType getType();

    /** Get display name */
    String getDisplayName();

    /** Initialize the adapter (load driver, set up connection pool, etc.) */
    void initialize(DatabaseConfig config) throws Exception;

    /** Get a new database connection */
    Connection getConnection() throws SQLException;

    /** Check if the adapter is initialized and ready */
    boolean isReady();

    /** Shutdown the adapter and release resources */
    void shutdown();

    /** Get the JDBC URL used by this adapter */
    String getJdbcUrl();

    /**
     * Translate a SQLite-compatible DDL statement to this database's dialect.
     * This allows the existing schema to work across all databases.
     */
    default String translateDDL(String sqliteDDL) {
        return sqliteDDL; // Default: no translation needed (SQLite)
    }

    /**
     * Get the SQL for checking table existence.
     */
    default String getTableExistsSQL(String tableName) {
        return "SELECT 1 FROM " + tableName + " LIMIT 1";
    }

    /**
     * Get the INSERT OR IGNORE equivalent for this database.
     */
    default String translateInsertOrIgnore(String sql) {
        return sql; // SQLite default
    }

    /**
     * Get the PRAGMA equivalent or no-op for this database.
     */
    default String translatePragma(String pragma) {
        return pragma; // SQLite default
    }

    /**
     * Get the datetime function equivalent.
     */
    default String translateDatetime(String expr) {
        return expr; // SQLite default
    }
}
