package com.qsdpdp;

import com.qsdpdp.core.ComplianceEngine;
import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.db.DataSeeder;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.rag.RAGEvaluator;
import com.qsdpdp.security.SecurityManager;
import com.qsdpdp.sync.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * QS-DPDP Enterprise Application
 * Main entry point for the Compliance Operating System
 * Supports both Web Dashboard and Desktop (JavaFX) modes
 * 
 * @version 1.0.0
 * @since Phase 1
 */
@SpringBootApplication
public class QSDPDPApplication {

    private static final Logger logger = LoggerFactory.getLogger(QSDPDPApplication.class);
    private static final String VERSION = "1.0.0";
    private static final String PRODUCT_NAME = "QS-DPDP Enterprise";

    private static ConfigurableApplicationContext context;
    private static boolean servicesInitialized = false;

    public static void main(String[] args) {
        logger.info("╔════════════════════════════════════════════════════════════╗");
        logger.info("║          {} v{}                 ║", PRODUCT_NAME, VERSION);
        logger.info("║     Enterprise Compliance Operating System                 ║");
        logger.info("║            DPDP Act 2023 Compliant                         ║");
        logger.info("╚════════════════════════════════════════════════════════════╝");

        try {
            // Start Spring Boot (includes embedded Tomcat for web dashboard)
            context = SpringApplication.run(QSDPDPApplication.class, args);
            initializeCoreServices();
            servicesInitialized = true;

            // Seed test data if enabled
            seedDataIfEnabled();

            String port = context.getEnvironment().getProperty("server.port", "8080");
            logger.info("═══════════════════════════════════════════════════════════");
            logger.info("  QS-DPDP Enterprise v{} STARTED SUCCESSFULLY", VERSION);
            logger.info("  Web Dashboard: http://localhost:{}", port);
            logger.info("  REST API:      http://localhost:{}/api/dashboard", port);
            logger.info("  Status:        ALL SYSTEMS OPERATIONAL");
            logger.info("═══════════════════════════════════════════════════════════");

            logger.info("Enterprise dashboard ready at: http://localhost:{}", port);

            // Keep server running
            keepAlive();

        } catch (Exception e) {
            logger.error("Failed to start QS-DPDP Enterprise", e);
            System.exit(1);
        }
    }

    private static void initializeCoreServices() {
        logger.info("Initializing core services...");

        // Initialize Database
        DatabaseManager dbManager = context.getBean(DatabaseManager.class);
        dbManager.initialize();
        logger.info("✓ Database Manager initialized");

        // Initialize Security Manager
        SecurityManager securityManager = context.getBean(SecurityManager.class);
        securityManager.initialize();
        logger.info("✓ Security Manager initialized");

        // Initialize Event Bus
        EventBus eventBus = context.getBean(EventBus.class);
        eventBus.initialize();
        logger.info("✓ Event Bus initialized");

        // Initialize Sync Service (Phase 1 — WebSocket bridge)
        try {
            SyncService syncService = context.getBean(SyncService.class);
            syncService.initialize();
            logger.info("✓ Sync Service initialized (WebSocket bridge active)");
        } catch (Exception e) {
            logger.warn("Sync Service init skipped (non-fatal): {}", e.getMessage());
        }

        // Initialize Audit Service
        AuditService auditService = context.getBean(AuditService.class);
        auditService.initialize();
        logger.info("✓ Audit Service initialized");

        // Initialize RAG Evaluator
        RAGEvaluator ragEvaluator = context.getBean(RAGEvaluator.class);
        ragEvaluator.initialize();
        logger.info("✓ RAG Evaluator initialized");

        // Initialize Compliance Engine
        ComplianceEngine complianceEngine = context.getBean(ComplianceEngine.class);
        complianceEngine.initialize();
        logger.info("✓ Compliance Engine initialized");

        logger.info("All core services initialized successfully");
    }

    private static void seedDataIfEnabled() {
        try {
            String autoSeed = context.getEnvironment().getProperty("qsdpdp.features.auto-seed-data", "false");
            if ("true".equalsIgnoreCase(autoSeed)) {
                logger.info("Auto-seeding test data...");
                DataSeeder seeder = context.getBean(DataSeeder.class);
                seeder.seedAll();
                logger.info("✓ Test data seeded successfully");
            }
        } catch (Exception e) {
            logger.warn("Data seeding encountered an issue (non-fatal): {}", e.getMessage());
        }
    }

    private static void keepAlive() {
        // Keep the JVM alive while the Spring context is active.
        // Thread must NOT be daemon, otherwise JVM exits when main() returns.
        Thread keepAlive = new Thread(() -> {
            try {
                while (context != null && context.isActive()) {
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        keepAlive.setDaemon(false);
        keepAlive.setName("qsdpdp-keepalive");
        keepAlive.start();
        try {
            keepAlive.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static ConfigurableApplicationContext getContext() {
        return context;
    }

    public static boolean isServicesInitialized() {
        return servicesInitialized;
    }

    public static void shutdown() {
        logger.info("Shutting down QS-DPDP Enterprise...");
        if (context != null) {
            context.close();
        }
        logger.info("QS-DPDP Enterprise shutdown complete");
    }
}
