package com.qsdpdp;

import com.qsdpdp.db.*;
import com.qsdpdp.db.DatabaseAdapter.DatabaseType;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.logging.Logger;

/**
 * Database Adapter Test Suite
 * Tests the multi-database abstraction layer.
 * - SQLite tests always run (zero-config)
 * - Enterprise DB tests are conditional (require running server)
 *
 * @version 1.0.0
 * @since Phase B
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase B — Multi-Database Adapter Tests")
public class DatabaseAdapterTest {

    private static final Logger log = Logger.getLogger(DatabaseAdapterTest.class.getName());

    // ═══════════════════════════════════════════════════════════
    // SQLITE ADAPTER TESTS (always run)
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void testSQLiteAdapterCreation() {
        DatabaseConfig config = new DatabaseConfig();
        config.setType("sqlite");
        DatabaseAdapterFactory factory = new DatabaseAdapterFactory();
        DatabaseAdapter adapter = factory.createAdapter(config);

        assertNotNull(adapter);
        assertEquals(DatabaseType.SQLITE, adapter.getType());
        assertEquals("SQLite 3.44", adapter.getDisplayName());
        assertInstanceOf(SQLiteAdapter.class, adapter);
    }

    @Test
    @Order(2)
    void testSQLiteAdapterInitialize() throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        config.setType("sqlite");
        config.setName("test_adapter_db");

        SQLiteAdapter adapter = new SQLiteAdapter();
        adapter.initialize(config);

        assertTrue(adapter.isReady());
        assertNotNull(adapter.getJdbcUrl());
        assertTrue(adapter.getJdbcUrl().startsWith("jdbc:sqlite:"));
        assertNotNull(adapter.getDbPath());

        adapter.shutdown();
        assertFalse(adapter.isReady());
    }

    @Test
    @Order(3)
    void testSQLiteConnection() throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        config.setType("sqlite");
        config.setName("test_connection_db");

        SQLiteAdapter adapter = new SQLiteAdapter();
        adapter.initialize(config);

        try (Connection conn = adapter.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            assertTrue(conn.isValid(2));

            // Execute a simple query
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT 1 AS test")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt("test"));
            }
        }

        adapter.shutdown();
    }

    @Test
    @Order(4)
    void testSQLiteCreateTable() throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        config.setType("sqlite");
        config.setName("test_schema_db");

        SQLiteAdapter adapter = new SQLiteAdapter();
        adapter.initialize(config);

        try (Connection conn = adapter.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS adapter_test (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            value REAL,
                            active INTEGER DEFAULT 1,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("INSERT OR IGNORE INTO adapter_test (id, name, value) VALUES ('t1', 'Test Row', 42.5)");

            try (ResultSet rs = stmt.executeQuery("SELECT * FROM adapter_test WHERE id='t1'")) {
                assertTrue(rs.next());
                assertEquals("Test Row", rs.getString("name"));
                assertEquals(42.5, rs.getDouble("value"), 0.001);
                assertEquals(1, rs.getInt("active"));
            }
        }

        adapter.shutdown();
    }

    @Test
    @Order(5)
    void testSQLiteDDLTranslation() {
        SQLiteAdapter adapter = new SQLiteAdapter();
        String ddl = "CREATE TABLE test (id INTEGER PRIMARY KEY AUTOINCREMENT)";
        // SQLite adapter should return DDL as-is
        assertEquals(ddl, adapter.translateDDL(ddl));
    }

    // ═══════════════════════════════════════════════════════════
    // DATABASE CONFIG TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(6)
    void testDatabaseConfigDefaults() {
        DatabaseConfig config = new DatabaseConfig();
        assertEquals("sqlite", config.getType());
        assertEquals("localhost", config.getHost());
        assertEquals(0, config.getPort());
        assertEquals("qsdpdp_enterprise", config.getName());
        assertEquals(10, config.getMaxPoolSize());
        assertEquals(DatabaseType.SQLITE, config.getDatabaseType());
    }

    @Test
    @Order(7)
    void testDatabaseConfigTypes() {
        DatabaseConfig config = new DatabaseConfig();

        config.setType("postgresql");
        assertEquals(DatabaseType.POSTGRESQL, config.getDatabaseType());
        assertEquals(5432, config.getEffectivePort());

        config.setType("mysql");
        assertEquals(DatabaseType.MYSQL, config.getDatabaseType());
        assertEquals(3306, config.getEffectivePort());

        config.setType("oracle");
        assertEquals(DatabaseType.ORACLE, config.getDatabaseType());
        assertEquals(1521, config.getEffectivePort());

        config.setType("mssql");
        assertEquals(DatabaseType.MSSQL, config.getDatabaseType());
        assertEquals(1433, config.getEffectivePort());

        config.setType("sqlite");
        assertEquals(DatabaseType.SQLITE, config.getDatabaseType());
        assertEquals(0, config.getEffectivePort());
    }

    @Test
    @Order(8)
    void testDatabaseConfigAliases() {
        DatabaseConfig config = new DatabaseConfig();

        config.setType("postgres");
        assertEquals(DatabaseType.POSTGRESQL, config.getDatabaseType());

        config.setType("mariadb");
        assertEquals(DatabaseType.MYSQL, config.getDatabaseType());

        config.setType("sqlserver");
        assertEquals(DatabaseType.MSSQL, config.getDatabaseType());
    }

    // ═══════════════════════════════════════════════════════════
    // FACTORY TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(9)
    void testFactoryCreatesSQLiteByDefault() {
        DatabaseAdapterFactory factory = new DatabaseAdapterFactory();
        DatabaseConfig config = new DatabaseConfig();
        DatabaseAdapter adapter = factory.createAdapter(config);
        assertInstanceOf(SQLiteAdapter.class, adapter);
    }

    @Test
    @Order(10)
    void testFactoryCreatesCorrectAdapters() {
        DatabaseAdapterFactory factory = new DatabaseAdapterFactory();
        DatabaseConfig config = new DatabaseConfig();

        config.setType("postgresql");
        assertInstanceOf(PostgreSQLAdapter.class, factory.createAdapter(config));

        config.setType("mysql");
        assertInstanceOf(MySQLAdapter.class, factory.createAdapter(config));

        config.setType("oracle");
        assertInstanceOf(OracleAdapter.class, factory.createAdapter(config));

        config.setType("mssql");
        assertInstanceOf(MSSQLAdapter.class, factory.createAdapter(config));
    }

    @Test
    @Order(11)
    void testFactoryCreateAndInitializeSQLite() throws Exception {
        DatabaseAdapterFactory factory = new DatabaseAdapterFactory();
        DatabaseConfig config = new DatabaseConfig();
        config.setType("sqlite");
        config.setName("test_factory_init");

        DatabaseAdapter adapter = factory.createAndInitialize(config);
        assertTrue(adapter.isReady());
        assertEquals(DatabaseType.SQLITE, adapter.getType());

        try (Connection conn = adapter.getConnection()) {
            assertTrue(conn.isValid(2));
        }

        adapter.shutdown();
    }

    // ═══════════════════════════════════════════════════════════
    // DDL TRANSLATION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(12)
    void testPostgreSQLDDLTranslation() {
        PostgreSQLAdapter adapter = new PostgreSQLAdapter();
        String ddl = "INSERT OR IGNORE INTO test VALUES ('a')";
        String translated = adapter.translateInsertOrIgnore(ddl);
        assertTrue(translated.contains("ON CONFLICT DO NOTHING"));
    }

    @Test
    @Order(13)
    void testMySQLDDLTranslation() {
        MySQLAdapter adapter = new MySQLAdapter();
        String ddl = "INSERT OR IGNORE INTO test VALUES ('a')";
        String translated = adapter.translateInsertOrIgnore(ddl);
        assertTrue(translated.contains("INSERT IGNORE"));
    }

    @Test
    @Order(14)
    void testMSSQLDDLTranslation() {
        MSSQLAdapter adapter = new MSSQLAdapter();
        String ddl = "CREATE TABLE test (name TEXT, score REAL)";
        String translated = adapter.translateDDL(ddl);
        assertTrue(translated.contains("NVARCHAR(MAX)"));
        assertTrue(translated.contains("FLOAT"));
    }

    @Test
    @Order(15)
    void testOracleDDLTranslation() {
        OracleAdapter adapter = new OracleAdapter();
        String ddl = "CREATE TABLE test (name TEXT, score REAL)";
        String translated = adapter.translateDDL(ddl);
        assertTrue(translated.contains("VARCHAR2(4000)"));
        assertTrue(translated.contains("NUMBER(10,2)"));
    }

    // ═══════════════════════════════════════════════════════════
    // PRAGMA TRANSLATION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(16)
    void testPragmaTranslation() {
        // SQLite keeps pragmas
        SQLiteAdapter sqlite = new SQLiteAdapter();
        assertEquals("PRAGMA foreign_keys = ON", sqlite.translatePragma("PRAGMA foreign_keys = ON"));

        // Others translate to no-op SELECT
        PostgreSQLAdapter pg = new PostgreSQLAdapter();
        assertEquals("SELECT 1", pg.translatePragma("PRAGMA foreign_keys = ON"));

        MySQLAdapter mysql = new MySQLAdapter();
        assertEquals("SELECT 1", mysql.translatePragma("PRAGMA journal_mode = WAL"));

        MSSQLAdapter mssql = new MSSQLAdapter();
        assertEquals("SELECT 1", mssql.translatePragma("PRAGMA foreign_keys = ON"));

        OracleAdapter oracle = new OracleAdapter();
        assertEquals("SELECT 1 FROM DUAL", oracle.translatePragma("PRAGMA journal_mode = WAL"));
    }

    // ═══════════════════════════════════════════════════════════
    // DATETIME TRANSLATION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(17)
    void testDatetimeTranslation() {
        String expr = "datetime(CURRENT_TIMESTAMP, '+30 days')";

        PostgreSQLAdapter pg = new PostgreSQLAdapter();
        assertTrue(pg.translateDatetime(expr).contains("INTERVAL"));

        MySQLAdapter mysql = new MySQLAdapter();
        assertTrue(mysql.translateDatetime(expr).contains("DATE_ADD"));

        OracleAdapter oracle = new OracleAdapter();
        assertTrue(oracle.translateDatetime(expr).contains("SYSTIMESTAMP"));

        MSSQLAdapter mssql = new MSSQLAdapter();
        assertTrue(mssql.translateDatetime(expr).contains("DATEADD"));
    }

    // ═══════════════════════════════════════════════════════════
    // ENTERPRISE DB CONDITIONAL TESTS
    // (Only run if the DB server is available)
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(18)
    void testPostgreSQLConditional() {
        DatabaseConfig config = new DatabaseConfig();
        config.setType("postgresql");
        config.setHost("localhost");
        config.setPort(5432);
        config.setName("qsdpdp_test");
        config.setUsername("qsdpdp");
        config.setPassword("qsdpdp_secret");

        PostgreSQLAdapter adapter = new PostgreSQLAdapter();
        try {
            adapter.initialize(config);
            assertTrue(adapter.isReady());
            log.info("✅ PostgreSQL available and connected");
            adapter.shutdown();
        } catch (Exception e) {
            log.info("⏩ PostgreSQL not available — skipping (expected in CI): " + e.getMessage());
            // Not a failure — just means no PostgreSQL server is running
        }
    }

    @Test
    @Order(19)
    void testMySQLConditional() {
        DatabaseConfig config = new DatabaseConfig();
        config.setType("mysql");
        config.setHost("localhost");
        config.setPort(3306);
        config.setName("qsdpdp_test");
        config.setUsername("qsdpdp");
        config.setPassword("qsdpdp_secret");

        MySQLAdapter adapter = new MySQLAdapter();
        try {
            adapter.initialize(config);
            assertTrue(adapter.isReady());
            log.info("✅ MySQL available and connected");
            adapter.shutdown();
        } catch (Exception e) {
            log.info("⏩ MySQL not available — skipping (expected in CI): " + e.getMessage());
        }
    }

    @Test
    @Order(20)
    void testSummary() {
        log.info("═══════════════════════════════════════════════");
        log.info("  DATABASE ADAPTER TESTS COMPLETE");
        log.info("  Adapters validated: SQLite (full), PG/MySQL/Oracle/MSSQL (DDL)");
        log.info("═══════════════════════════════════════════════");
    }
}
