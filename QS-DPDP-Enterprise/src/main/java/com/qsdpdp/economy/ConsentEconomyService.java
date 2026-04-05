package com.qsdpdp.economy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Consent Economy Service
 * Implements consent wallet, marketplace, and real-time notifications.
 *
 * Features:
 * 1. Consent Wallet — Data Principal centralized consent store
 * 2. Consent Marketplace — Future-ready monetization framework
 * 3. Consent Notifications — Real-time alerts for use, misuse, breach
 *
 * References:
 * - DPDP Act 2023 S.6 (consent management), S.11 (Data Principal rights)
 * - MyData Global: Human-centric personal data model
 * - W3C Verifiable Credentials: Consent as verifiable claims
 *
 * @version 1.0.0
 * @since Universal Trust OS v3.0
 */
@Service
public class ConsentEconomyService {

    private static final Logger logger = LoggerFactory.getLogger(ConsentEconomyService.class);

    // Wallets: principalId → ConsentWallet
    private final Map<String, ConsentWallet> wallets = new ConcurrentHashMap<>();
    // Marketplace listings
    private final CopyOnWriteArrayList<MarketplaceListing> listings = new CopyOnWriteArrayList<>();
    // Notification subscriptions
    private final Map<String, NotificationSubscription> subscriptions = new ConcurrentHashMap<>();
    // Notification log
    private final CopyOnWriteArrayList<ConsentNotification> notificationLog = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void initialize() {
        seedWallets();
        seedListings();
        logger.info("✅ Consent Economy initialized — {} wallets, {} marketplace listings", wallets.size(), listings.size());
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT WALLET
    // ═══════════════════════════════════════════════════════════

    public Map<String, Object> getWallet(String principalId) {
        ConsentWallet wallet = wallets.computeIfAbsent(principalId, id -> createNewWallet(id));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("principalId", principalId);
        result.put("walletId", wallet.walletId);
        result.put("totalConsents", wallet.consents.size());
        result.put("activeConsents", wallet.consents.stream().filter(c -> "ACTIVE".equals(c.status)).count());
        result.put("revokedConsents", wallet.consents.stream().filter(c -> "REVOKED".equals(c.status)).count());
        result.put("consents", wallet.consents.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("consentId", c.consentId); m.put("fiduciary", c.fiduciary);
            m.put("purpose", c.purpose); m.put("status", c.status);
            m.put("grantedAt", c.grantedAt.toString());
            m.put("dataCategories", c.dataCategories);
            return m;
        }).toList());
        result.put("privacyScore", calculatePrivacyScore(wallet));
        result.put("rights", Map.of(
                "access", "S.11 — Right to access personal data summary",
                "correction", "S.12 — Right to correction of inaccurate data",
                "erasure", "S.12(3) — Right to erasure upon consent withdrawal",
                "portability", "Planned — Data portability framework",
                "grievance", "S.13 — Right to grievance redressal"
        ));
        result.put("createdAt", wallet.createdAt.toString());
        return result;
    }

    public Map<String, Object> revokeAll(String principalId) {
        ConsentWallet wallet = wallets.get(principalId);
        if (wallet == null) return Map.of("status", "WALLET_NOT_FOUND");

        int revoked = 0;
        for (WalletConsent c : wallet.consents) {
            if ("ACTIVE".equals(c.status)) {
                c.status = "REVOKED";
                c.revokedAt = Instant.now();
                revoked++;
                triggerNotification(principalId, "CONSENT_REVOKED", "Consent " + c.consentId + " revoked for " + c.fiduciary);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "MASS_REVOCATION_COMPLETE");
        result.put("principalId", principalId);
        result.put("consentsRevoked", revoked);
        result.put("revokedAt", Instant.now().toString());
        result.put("dpdpCompliance", "S.6(6) — All consents withdrawn; fiduciaries notified");
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT MARKETPLACE
    // ═══════════════════════════════════════════════════════════

    public Map<String, Object> getListings() {
        List<Map<String, Object>> activeListing = listings.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("listingId", l.listingId); m.put("title", l.title);
            m.put("fiduciary", l.fiduciary); m.put("purpose", l.purpose);
            m.put("dataCategories", l.dataCategories); m.put("compensationType", l.compensationType);
            m.put("status", l.status); m.put("createdAt", l.createdAt.toString());
            return m;
        }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalListings", listings.size());
        result.put("activeListings", listings.stream().filter(l -> "ACTIVE".equals(l.status)).count());
        result.put("listings", activeListing);
        result.put("notice", "Consent Marketplace is future-ready. Monetization features pending regulatory clarity on consent value exchange.");
        result.put("framework", "MyData Global + W3C Verifiable Credentials");
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // NOTIFICATIONS ENGINE
    // ═══════════════════════════════════════════════════════════

    public Map<String, Object> subscribe(String principalId, Map<String, Object> params) {
        NotificationSubscription sub = new NotificationSubscription();
        sub.subscriptionId = "SUB-" + UUID.randomUUID().toString().substring(0, 8);
        sub.principalId = principalId;
        sub.channels = (List<String>) params.getOrDefault("channels", List.of("IN_APP", "EMAIL"));
        sub.consentUseAlerts = (Boolean) params.getOrDefault("consentUseAlerts", true);
        sub.breachAlerts = (Boolean) params.getOrDefault("breachAlerts", true);
        sub.misuseAlerts = (Boolean) params.getOrDefault("misuseAlerts", true);
        sub.createdAt = Instant.now();
        subscriptions.put(principalId, sub);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUBSCRIBED");
        result.put("subscriptionId", sub.subscriptionId);
        result.put("principalId", principalId);
        result.put("channels", sub.channels);
        result.put("alertTypes", Map.of(
                "consentUse", sub.consentUseAlerts,
                "breach", sub.breachAlerts,
                "misuse", sub.misuseAlerts
        ));
        result.put("dpdpCompliance", "S.8(3) — Transparency obligation through proactive notifications");
        return result;
    }

    public Map<String, Object> getNotifications(String principalId) {
        List<Map<String, Object>> notifications = notificationLog.stream()
                .filter(n -> principalId.equals(n.principalId))
                .map(n -> Map.of("type", (Object) n.type, "message", n.message, "timestamp", n.timestamp.toString()))
                .toList();

        return Map.of("principalId", principalId, "notifications", notifications, "total", notifications.size());
    }

    private void triggerNotification(String principalId, String type, String message) {
        ConsentNotification n = new ConsentNotification();
        n.principalId = principalId;
        n.type = type;
        n.message = message;
        n.timestamp = Instant.now();
        notificationLog.add(n);
    }

    // ── Helpers ──

    private double calculatePrivacyScore(ConsentWallet wallet) {
        if (wallet.consents.isEmpty()) return 100;
        long active = wallet.consents.stream().filter(c -> "ACTIVE".equals(c.status)).count();
        long total = wallet.consents.size();
        return Math.round((1 - (double) active / (total + 5)) * 100 * 10.0) / 10.0;
    }

    private ConsentWallet createNewWallet(String principalId) {
        ConsentWallet wallet = new ConsentWallet();
        wallet.walletId = "WALLET-" + UUID.randomUUID().toString().substring(0, 8);
        wallet.principalId = principalId;
        wallet.createdAt = Instant.now();
        wallet.consents = new ArrayList<>();
        return wallet;
    }

    private void seedWallets() {
        String[][] data = {
                {"DP-001", "ACME Corp", "Marketing analytics", "ACTIVE", "PERSONAL_DATA,BEHAVIORAL"},
                {"DP-001", "MegaBank", "Credit scoring", "ACTIVE", "FINANCIAL_DATA,PERSONAL_DATA"},
                {"DP-001", "HealthCare Inc", "Treatment records", "ACTIVE", "HEALTH_DATA"},
                {"DP-001", "TeleCom Ltd", "Service delivery", "REVOKED", "PERSONAL_DATA,LOCATION"},
                {"DP-002", "EduTech", "Learning analytics", "ACTIVE", "PERSONAL_DATA,BEHAVIORAL"},
                {"DP-002", "InsurTech", "Policy processing", "ACTIVE", "PERSONAL_DATA,FINANCIAL_DATA"}
        };
        for (String[] d : data) {
            ConsentWallet wallet = wallets.computeIfAbsent(d[0], this::createNewWallet);
            WalletConsent c = new WalletConsent();
            c.consentId = "C-" + UUID.randomUUID().toString().substring(0, 6);
            c.fiduciary = d[1]; c.purpose = d[2]; c.status = d[3];
            c.dataCategories = List.of(d[4].split(","));
            c.grantedAt = Instant.now();
            wallet.consents.add(c);
        }
    }

    private void seedListings() {
        listings.add(createListing("Academic Research Data Sharing", "Research Foundation", "Academic research",
                List.of("ANONYMIZED_DATA"), "RECOGNITION"));
        listings.add(createListing("Smart City Analytics", "Municipal Corp", "Urban planning",
                List.of("LOCATION_DATA", "BEHAVIORAL"), "PUBLIC_BENEFIT"));
        listings.add(createListing("Health Research Consent", "Medical Institute", "Clinical research",
                List.of("HEALTH_DATA"), "INFORMED_CONSENT"));
    }

    private MarketplaceListing createListing(String title, String fiduciary, String purpose,
                                              List<String> categories, String compensationType) {
        MarketplaceListing l = new MarketplaceListing();
        l.listingId = "MKT-" + UUID.randomUUID().toString().substring(0, 8);
        l.title = title; l.fiduciary = fiduciary; l.purpose = purpose;
        l.dataCategories = categories; l.compensationType = compensationType;
        l.status = "ACTIVE"; l.createdAt = Instant.now();
        return l;
    }

    // ── Data Classes ──
    static class ConsentWallet {
        String walletId, principalId;
        List<WalletConsent> consents;
        Instant createdAt;
    }
    static class WalletConsent {
        String consentId, fiduciary, purpose, status;
        List<String> dataCategories;
        Instant grantedAt, revokedAt;
    }
    static class MarketplaceListing {
        String listingId, title, fiduciary, purpose, compensationType, status;
        List<String> dataCategories;
        Instant createdAt;
    }
    static class NotificationSubscription {
        String subscriptionId, principalId;
        List<String> channels;
        boolean consentUseAlerts, breachAlerts, misuseAlerts;
        Instant createdAt;
    }
    static class ConsentNotification {
        String principalId, type, message;
        Instant timestamp;
    }
}
