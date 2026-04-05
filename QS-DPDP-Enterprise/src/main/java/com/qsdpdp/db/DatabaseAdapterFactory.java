package com.qsdpdp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Database Adapter Factory — Creates the appropriate adapter based on
 * configuration
 *
 * @version 1.0.0
 * @since Phase B
 */
@Component
public class DatabaseAdapterFactory {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseAdapterFactory.class);

    /**
     * Create a DatabaseAdapter based on the given configuration.
     */
    public DatabaseAdapter createAdapter(DatabaseConfig config) {
        DatabaseAdapter.DatabaseType type = config.getDatabaseType();
        logger.info("Creating database adapter for: {}", type);

        return switch (type) {
            case POSTGRESQL -> new PostgreSQLAdapter();
            case MYSQL -> new MySQLAdapter();
            case ORACLE -> new OracleAdapter();
            case MSSQL -> new MSSQLAdapter();
            default -> new SQLiteAdapter();
        };
    }

    /**
     * Create and initialize an adapter in one step.
     */
    public DatabaseAdapter createAndInitialize(DatabaseConfig config) throws Exception {
        DatabaseAdapter adapter = createAdapter(config);
        adapter.initialize(config);
        return adapter;
    }
}
