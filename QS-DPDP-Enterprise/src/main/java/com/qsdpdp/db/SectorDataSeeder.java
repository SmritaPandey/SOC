package com.qsdpdp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sector-Specific Data Seeder for QS-DPDP Enterprise
 * Generates 500+ realistic records per sector for 6 industries:
 * BFSI, Healthcare, E-Commerce, Government, Education, Telecom
 *
 * @version 1.0.0
 * @since Phase C
 */
@Component
public class SectorDataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(SectorDataSeeder.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Random RNG = ThreadLocalRandom.current();

    @Autowired
    private DatabaseManager dbManager;

    // ═══════════════════════════════════════════════════════════
    // SECTOR DEFINITIONS
    // ═══════════════════════════════════════════════════════════

    public enum Sector {
        BFSI("Banking, Financial Services & Insurance"),
        HEALTHCARE("Healthcare & Hospitals"),
        INSURANCE("Insurance"),
        FINTECH("Fintech & Digital Payments"),
        TELECOM("Telecom & ISP"),
        GOVERNMENT("Government & Public Sector"),
        EDUCATION("Education & EdTech"),
        ECOMMERCE("E-Commerce & Retail"),
        MANUFACTURING("Manufacturing & Industrial"),
        ENERGY("Energy & Utilities"),
        TRANSPORT("Transport & Logistics"),
        MEDIA("Media, Entertainment & OTT"),
        AGRICULTURE("Agriculture & Allied"),
        PHARMA("Pharmaceuticals & Life Sciences"),
        REALESTATE("Real Estate & Construction"),
        LEGAL("Legal & Professional Services"),
        HOSPITALITY("Hospitality & Tourism"),
        SOCIALMEDIA("Social Media & Platforms");

        public final String displayName;

        Sector(String name) {
            this.displayName = name;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PII DATA PATTERNS (Indian context)
    // ═══════════════════════════════════════════════════════════

    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Reyansh", "Mohammed", "Sai",
            "Arnav", "Dhruv", "Kabir", "Ritik", "Aadhya", "Saanvi", "Anika", "Diya",
            "Priya", "Ananya", "Ishaan", "Rohan", "Neha", "Pooja", "Sneha", "Meera",
            "Raj", "Amit", "Suresh", "Vikram", "Lakshmi", "Kavya", "Riya", "Tanvi"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Verma", "Gupta", "Singh", "Kumar", "Patel", "Reddy", "Nair",
            "Joshi", "Iyer", "Mukherjee", "Das", "Mehta", "Shah", "Rao", "Pillai",
            "Agarwal", "Mishra", "Banerjee", "Chatterjee", "Pandey", "Dubey", "Saxena", "Bhat"
    };

    private static final String[] CITIES = {
            "Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai", "Kolkata", "Pune",
            "Ahmedabad", "Jaipur", "Lucknow", "Bhopal", "Chandigarh", "Kochi", "Indore"
    };

    // ═══════════════════════════════════════════════════════════
    // SECTOR-SPECIFIC DATA TEMPLATES
    // ═══════════════════════════════════════════════════════════

    private static final Map<Sector, String[]> SECTOR_PURPOSES;
    private static final Map<Sector, String[]> SECTOR_BREACH_TYPES;
    private static final Map<Sector, String[]> SECTOR_DLP_PATTERNS;

    static {
        // ── PURPOSES ────────────────────────────────────────
        Map<Sector, String[]> p = new LinkedHashMap<>();
        p.put(Sector.BFSI, new String[] { "KYC Verification", "Loan Processing", "Credit Scoring",
                "Insurance Underwriting", "Transaction Monitoring", "Fraud Detection",
                "Account Opening", "Portfolio Management", "Tax Reporting", "RBI Compliance" });
        p.put(Sector.HEALTHCARE, new String[] { "Patient Registration", "Clinical Diagnosis",
                "Prescription Management", "Lab Reports", "Insurance Claims", "Telemedicine",
                "ABDM Health ID", "Vaccination Records", "Emergency Treatment", "Research Consent" });
        p.put(Sector.INSURANCE, new String[] { "Policy Underwriting", "Claims Processing",
                "Premium Calculation", "Agent Verification", "IRDAI Compliance",
                "Fraud Investigation", "Reinsurance Data", "Health Data Processing" });
        p.put(Sector.FINTECH, new String[] { "eKYC Verification", "UPI Wallet", "Account Aggregator",
                "BNPL Assessment", "Digital Lending", "Investment Advisory",
                "Merchant Onboarding", "RBI Sandbox Compliance" });
        p.put(Sector.TELECOM, new String[] { "SIM Registration", "CDR Processing",
                "Location Tracking", "Billing Data", "Network Analytics", "Customer Verification",
                "Roaming Data", "Usage Analytics", "TRAI Compliance", "Number Portability" });
        p.put(Sector.GOVERNMENT, new String[] { "Aadhaar Verification", "PAN Validation",
                "Passport Processing", "Voter ID", "RTI Processing", "Tax Filing",
                "Subsidy Distribution", "Census Data", "CERT-In Reporting", "Digital Locker" });
        p.put(Sector.EDUCATION, new String[] { "Student Enrollment", "Academic Records",
                "Examination Data", "Attendance Tracking", "Library Usage", "Fee Processing",
                "Scholarship Data", "Minor Data Protection", "Parent Consent", "EdTech Analytics" });
        p.put(Sector.ECOMMERCE, new String[] { "Order Processing", "Payment Processing",
                "Delivery Address", "Wishlist Tracking", "Product Recommendations",
                "Returns Processing", "Customer Support", "Marketing Consent", "Cookie Tracking", "Reviews" });
        p.put(Sector.MANUFACTURING, new String[] { "Employee PII", "Contractor Verification",
                "IoT Sensor Data", "Safety Incident", "CCTV Monitoring",
                "Supply Chain Data", "Quality Audit", "Environmental Compliance" });
        p.put(Sector.ENERGY, new String[] { "Smart Meter Data", "Billing & Usage",
                "Subsidy Verification", "Grid Analytics", "Consumer Complaints",
                "Solar Rooftop Registration", "EV Charging Data", "Safety Compliance" });
        p.put(Sector.TRANSPORT, new String[] { "Driving License", "GPS Tracking",
                "e-Challan Processing", "FASTag Data", "Passenger Manifest",
                "Fleet Management", "Accident Reports", "VAHAN Integration" });
        p.put(Sector.MEDIA, new String[] { "User Profiles", "Content Preferences",
                "Ad Targeting", "Viewing History", "Age Verification",
                "Subscription Data", "Creator Identity", "CBFC Compliance" });
        p.put(Sector.AGRICULTURE, new String[] { "PM-KISAN Verification", "Land Records",
                "Crop Insurance", "Soil Health Data", "Market Price Data",
                "Farmer Registration", "Subsidy Distribution", "e-NAM Trading" });
        p.put(Sector.PHARMA, new String[] { "Clinical Trial Consent", "Patient Safety Data",
                "Pharmacovigilance", "Drug Adverse Events", "CDSCO Compliance",
                "Biobank Consent", "Prescription Audit", "Manufacturing Quality" });
        p.put(Sector.REALESTATE, new String[] { "Buyer KYC", "RERA Compliance",
                "Property Registration", "Tenant Verification", "CCTV Surveillance",
                "Loan Processing", "Stamp Duty Data", "Ownership Transfer" });
        p.put(Sector.LEGAL, new String[] { "Client Privilege", "Case Management",
                "e-Courts Data", "Arbitration Records", "Witness Protection",
                "Document Filing", "Bar Council Compliance", "Legal Aid Data" });
        p.put(Sector.HOSPITALITY, new String[] { "Guest Registration", "C-Form Filing",
                "Passport Copy", "FRO/FRRO Reporting", "Loyalty Programs",
                "Dietary Preferences", "Payment Processing", "FSSAI Compliance" });
        p.put(Sector.SOCIALMEDIA, new String[] { "User Content", "Ad Profile",
                "Age Verification", "Behavioral Analytics", "Location Sharing",
                "Friend Graph", "Content Moderation", "IT Act Compliance" });
        SECTOR_PURPOSES = Collections.unmodifiableMap(p);

        // ── BREACH TYPES ───────────────────────────────────
        Map<Sector, String[]> b = new LinkedHashMap<>();
        b.put(Sector.BFSI, new String[] { "Card Data Breach", "Core Banking Intrusion", "ATM Skimming",
                "UPI Fraud", "KYC Data Leak", "Insider Trading Data" });
        b.put(Sector.HEALTHCARE, new String[] { "Patient Records Leak", "Lab Report Exposure",
                "ABDM Data Breach", "Pharmacy Data Theft", "Telemedicine Hack", "Insurance Data Leak" });
        b.put(Sector.INSURANCE, new String[] { "Policy Holder Data Leak", "Claims Fraud",
                "Agent Data Breach", "Medical Records Exposure", "Underwriting Data Theft" });
        b.put(Sector.FINTECH, new String[] { "Wallet Data Breach", "eKYC Data Leak",
                "Transaction Exposure", "Lending Data Theft", "API Key Compromise" });
        b.put(Sector.TELECOM, new String[] { "CDR Data Leak", "Location Data Exposure",
                "Subscriber Data Breach", "Billing Fraud", "Network Intrusion", "SIM Swap Fraud" });
        b.put(Sector.GOVERNMENT, new String[] { "Aadhaar Data Leak", "Tax Records Exposure",
                "Voter Data Breach", "RTI Document Leak", "Subsidy Fraud", "Census Data Breach" });
        b.put(Sector.EDUCATION, new String[] { "Student Records Leak", "Exam Paper Leak",
                "Minor Data Exposure", "Parent Info Breach", "Fee Data Theft", "EdTech Data Leak" });
        b.put(Sector.ECOMMERCE, new String[] { "Payment Gateway Breach", "Customer DB Exposure",
                "Delivery Address Leak", "Session Hijacking", "Cart Data Theft", "Review Manipulation" });
        b.put(Sector.MANUFACTURING, new String[] { "Employee Data Leak", "Trade Secret Theft",
                "IoT Device Compromise", "SCADA Breach", "Supply Chain Data Leak" });
        b.put(Sector.ENERGY, new String[] { "Smart Meter Data Leak", "Grid Control Breach",
                "Consumer Data Exposure", "SCADA Compromise", "Billing Fraud" });
        b.put(Sector.TRANSPORT, new String[] { "GPS Data Leak", "Passenger Data Breach",
                "FASTag Data Exposure", "License Data Theft", "Fleet Tracking Breach" });
        b.put(Sector.MEDIA, new String[] { "User Profile Leak", "Viewing History Exposure",
                "Content Creator Data Breach", "Ad Data Theft", "Subscription Data Leak" });
        b.put(Sector.AGRICULTURE, new String[] { "Farmer Data Leak", "Land Record Exposure",
                "Subsidy Fraud Data", "Crop Insurance Breach", "Market Data Theft" });
        b.put(Sector.PHARMA, new String[] { "Clinical Trial Data Leak", "Patient Safety Breach",
                "Drug Formula Theft", "Pharmacovigilance Leak", "CDSCO Data Breach" });
        b.put(Sector.REALESTATE, new String[] { "Buyer KYC Leak", "RERA Data Breach",
                "Property Records Exposure", "Tenant Data Theft", "CCTV Footage Leak" });
        b.put(Sector.LEGAL, new String[] { "Client Data Breach", "Case File Leak",
                "Witness Data Exposure", "Court Records Breach", "Privileged Info Theft" });
        b.put(Sector.HOSPITALITY, new String[] { "Guest Data Breach", "Passport Copy Leak",
                "Payment Info Exposure", "Loyalty Data Theft", "C-Form Data Leak" });
        b.put(Sector.SOCIALMEDIA, new String[] { "User Account Breach", "Private Message Leak",
                "Location Data Exposure", "Ad Targeting Data Theft", "Content Moderation Leak" });
        SECTOR_BREACH_TYPES = Collections.unmodifiableMap(b);

        // ── DLP PATTERNS ───────────────────────────────────
        Map<Sector, String[]> d = new LinkedHashMap<>();
        d.put(Sector.BFSI, new String[] { "\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}", "IFSC:[A-Z]{4}\\d{7}",
                "UPI:[a-z]+@[a-z]+", "PAN:[A-Z]{5}\\d{4}[A-Z]" });
        d.put(Sector.HEALTHCARE, new String[] { "ABHA:\\d{14}", "MRN:[A-Z]{2}\\d{8}",
                "Diagnosis:ICD-\\d+", "Rx:\\d{10}" });
        d.put(Sector.INSURANCE, new String[] { "PolicyNo:INS\\d{10}", "ClaimRef:CLM\\d{8}",
                "IRDAI:\\d{6}", "NomineeID:NOM\\d{6}" });
        d.put(Sector.FINTECH, new String[] { "UPI:[a-z]+@[a-z]+", "WalletID:WAL\\d{10}",
                "NBFC:\\d{8}", "LoanRef:LN\\d{10}" });
        d.put(Sector.TELECOM, new String[] { "MSISDN:91\\d{10}", "IMEI:\\d{15}",
                "IMSI:\\d{15}", "CDR:CDR\\d{12}" });
        d.put(Sector.GOVERNMENT, new String[] { "Aadhaar:\\d{12}", "PAN:[A-Z]{5}\\d{4}[A-Z]",
                "Passport:[A-Z]\\d{7}", "VoterID:[A-Z]{3}\\d{7}" });
        d.put(Sector.EDUCATION, new String[] { "RollNo:\\d{10}", "UDISE:\\d{11}",
                "ScholarshipID:SCH\\d{8}", "ParentID:PRT\\d{6}" });
        d.put(Sector.ECOMMERCE, new String[] { "OrderID:ORD\\d{10}", "CVV:\\d{3}",
                "TrackingID:[A-Z]{2}\\d{12}", "CouponCode:[A-Z0-9]{8}" });
        d.put(Sector.MANUFACTURING, new String[] { "EmpID:MFG\\d{6}", "IoTMAC:[0-9A-F:]{17}",
                "BatchNo:BAT\\d{8}", "SafetyRef:SF\\d{6}" });
        d.put(Sector.ENERGY, new String[] { "MeterID:MTR\\d{10}", "ConsumerNo:CN\\d{10}",
                "BillRef:BIL\\d{8}", "GridID:GRD\\d{6}" });
        d.put(Sector.TRANSPORT, new String[] { "DL:[A-Z]{2}\\d{13}", "FASTag:FT\\d{16}",
                "VehicleRC:[A-Z]{2}\\d{2}[A-Z]{2}\\d{4}", "ChallanID:ECH\\d{10}" });
        d.put(Sector.MEDIA, new String[] { "UserID:USR\\d{10}", "ContentID:CNT\\d{8}",
                "SubID:SUB\\d{8}", "AdID:AD\\d{10}" });
        d.put(Sector.AGRICULTURE, new String[] { "KisanID:PM\\d{12}", "LandParcel:LP\\d{10}",
                "CropInsID:CI\\d{8}", "eNAM:NAM\\d{8}" });
        d.put(Sector.PHARMA, new String[] { "TrialID:CT\\d{10}", "BatchNo:DRG\\d{8}",
                "PatientSafety:PS\\d{6}", "CDSCO:CD\\d{8}" });
        d.put(Sector.REALESTATE, new String[] { "RERA:RERA\\d{10}", "PropID:PROP\\d{8}",
                "StampDuty:SD\\d{10}", "TenantID:TNT\\d{6}" });
        d.put(Sector.LEGAL, new String[] { "CaseNo:[A-Z]{2}/\\d{4}/\\d+", "BarCouncil:BC\\d{8}",
                "ClientRef:CLI\\d{6}", "ArbitID:ARB\\d{6}" });
        d.put(Sector.HOSPITALITY, new String[] { "GuestID:GST\\d{8}", "CForm:CF\\d{10}",
                "LoyaltyID:LOY\\d{8}", "BookingRef:BK\\d{10}" });
        d.put(Sector.SOCIALMEDIA, new String[] { "UserHandle:@[a-zA-Z0-9_]+", "PostID:P\\d{12}",
                "AdAccountID:AA\\d{8}", "ReportID:RPT\\d{8}" });
        SECTOR_DLP_PATTERNS = Collections.unmodifiableMap(d);
    }

    // ═══════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════

    /** Seed all 6 sectors */
    public Map<String, Integer> seedAllSectors() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Sector sector : Sector.values()) {
            int count = seedSector(sector);
            counts.put(sector.displayName, count);
        }
        return counts;
    }

    /** Seed a specific sector */
    public int seedSector(Sector sector) {
        logger.info("Seeding sector: {} ({})", sector.name(), sector.displayName);
        int total = 0;
        try {
            total += seedSectorPurposes(sector);
            total += seedSectorDataPrincipals(sector);
            total += seedSectorConsents(sector);
            total += seedSectorBreaches(sector);
            total += seedSectorDPIAs(sector);
            total += seedSectorPolicies(sector);
            total += seedSectorGapAssessments(sector);
            total += seedSectorSiemEvents(sector);
            total += seedSectorSiemAlerts(sector);
            total += seedSectorDlpData(sector);
            total += seedSectorRightsRequests(sector);
            total += seedSectorAuditLog(sector);
            logger.info("✅ {} sector seeded: {} records", sector.displayName, total);
        } catch (Exception e) {
            logger.error("Failed to seed sector: " + sector.name(), e);
        }
        return total;
    }

    // ═══════════════════════════════════════════════════════════
    // SEEDING METHODS
    // ═══════════════════════════════════════════════════════════

    private int seedSectorPurposes(Sector sector) throws SQLException {
        String[] purposes = SECTOR_PURPOSES.get(sector);
        String sql = "INSERT OR IGNORE INTO purposes (id, code, name, description, legal_basis, data_categories, retention_period_days, is_active) VALUES (?,?,?,?,?,?,?,1)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < purposes.length; i++) {
                String id = sector.name() + "-PUR-" + String.format("%03d", i + 1);
                ps.setString(1, id);
                ps.setString(2, sector.name() + "_" + purposes[i].toUpperCase().replace(" ", "_"));
                ps.setString(3, purposes[i]);
                ps.setString(4, sector.displayName + " — " + purposes[i] + " as per DPDP Act 2023");
                ps.setString(5, i % 3 == 0 ? "CONSENT" : i % 3 == 1 ? "LEGITIMATE_USE" : "LEGAL_OBLIGATION");
                ps.setString(6, "Personal,Financial,Sensitive");
                ps.setInt(7, 365 + RNG.nextInt(2555));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return purposes.length;
    }

    private int seedSectorDataPrincipals(Sector sector) throws SQLException {
        int count = 100; // 100 per sector × 6 = 600 total
        String sql = "INSERT OR IGNORE INTO data_principals (id, external_id, name, email, phone, is_child) VALUES (?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                String firstName = pick(FIRST_NAMES);
                String lastName = pick(LAST_NAMES);
                String id = sector.name() + "-DP-" + String.format("%04d", i);
                ps.setString(1, id);
                ps.setString(2, generateExternalId(sector, i));
                ps.setString(3, firstName + " " + lastName);
                ps.setString(4, firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@"
                        + sector.name().toLowerCase() + ".in");
                ps.setString(5, "+91" + (7000000000L + RNG.nextInt(999999999)));
                ps.setInt(6, sector == Sector.EDUCATION && i % 5 == 0 ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return count;
    }

    private int seedSectorConsents(Sector sector) throws SQLException {
        int count = 200;
        String[] purposes = SECTOR_PURPOSES.get(sector);
        String sql = "INSERT OR IGNORE INTO consents (id, data_principal_id, purpose_id, status, consent_method, language, hash, collected_at) VALUES (?,?,?,?,?,?,?,?)";
        String[] statuses = { "ACTIVE", "ACTIVE", "ACTIVE", "WITHDRAWN", "EXPIRED" };
        String[] methods = { "DIGITAL", "DIGITAL", "ELECTRONIC", "PAPER", "API" };
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                String id = sector.name() + "-CON-" + String.format("%04d", i);
                int dpIdx = 1 + RNG.nextInt(100);
                int purIdx = RNG.nextInt(purposes.length);
                ps.setString(1, id);
                ps.setString(2, sector.name() + "-DP-" + String.format("%04d", dpIdx));
                ps.setString(3, sector.name() + "-PUR-" + String.format("%03d", purIdx + 1));
                ps.setString(4, pick(statuses));
                ps.setString(5, pick(methods));
                ps.setString(6, i % 4 == 0 ? "hi" : i % 7 == 0 ? "ta" : "en");
                ps.setString(7, UUID.randomUUID().toString().substring(0, 16));
                ps.setString(8, randomTimestamp());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return count;
    }

    private int seedSectorBreaches(Sector sector) throws SQLException {
        int count = 50;
        String[] types = SECTOR_BREACH_TYPES.get(sector);
        String[] severities = { "LOW", "MEDIUM", "MEDIUM", "HIGH", "CRITICAL" };
        String sql = "INSERT OR IGNORE INTO breaches (id, reference_number, title, description, severity, breach_type, affected_count, detected_at, status) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                String id = sector.name() + "-BRE-" + String.format("%04d", i);
                String type = pick(types);
                ps.setString(1, id);
                ps.setString(2, "BRE-" + sector.name() + "-" + String.format("%04d", i));
                ps.setString(3, type + " — " + pick(CITIES) + " " + sector.displayName);
                ps.setString(4, "Sector: " + sector.displayName + ". " + type + " incident affecting data principals.");
                ps.setString(5, pick(severities));
                ps.setString(6, type);
                ps.setInt(7, 10 + RNG.nextInt(10000));
                ps.setString(8, randomTimestamp());
                ps.setString(9, i % 3 == 0 ? "RESOLVED" : i % 2 == 0 ? "INVESTIGATING" : "OPEN");
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return count;
    }

    private int seedSectorDPIAs(Sector sector) throws SQLException {
        int count = 30;
        String[] purposes = SECTOR_PURPOSES.get(sector);
        String sql = "INSERT OR IGNORE INTO dpias (id, reference_number, title, description, processing_activity, risk_level, status, involves_sensitive_data) VALUES (?,?,?,?,?,?,?,?)";
        String[] risks = { "LOW", "MEDIUM", "HIGH", "CRITICAL" };
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                String purpose = pick(purposes);
                String id = sector.name() + "-DPIA-" + String.format("%03d", i);
                ps.setString(1, id);
                ps.setString(2, "DPIA-" + sector.name() + "-" + String.format("%03d", i));
                ps.setString(3, "DPIA: " + purpose + " — " + sector.displayName);
                ps.setString(4, "Assessment of " + purpose + " data processing in " + sector.displayName);
                ps.setString(5, purpose);
                ps.setString(6, pick(risks));
                ps.setString(7, i % 4 == 0 ? "COMPLETED" : i % 3 == 0 ? "IN_REVIEW" : "DRAFT");
                ps.setInt(8, 1);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return count;
    }

    private int seedSectorPolicies(Sector sector) throws SQLException {
        int count = 20;
        String[] cats = { "DATA_PROTECTION", "CONSENT", "BREACH", "RETENTION", "ACCESS_CONTROL" };
        String sql = "INSERT OR IGNORE INTO policies (id, code, title, description, category, version, status) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                String id = sector.name() + "-POL-" + String.format("%03d", i);
                String cat = pick(cats);
                ps.setString(1, id);
                ps.setString(2, sector.name() + "_POL_" + String.format("%03d", i));
                ps.setString(3, sector.displayName + " " + cat.replace("_", " ") + " Policy v" + (1 + i % 3));
                ps.setString(4, "DPDP Act aligned " + cat.toLowerCase().replace("_", " ") + " policy for "
                        + sector.displayName);
                ps.setString(5, cat);
                ps.setString(6, (1 + i % 3) + ".0");
                ps.setString(7, i % 3 == 0 ? "APPROVED" : i % 2 == 0 ? "PUBLISHED" : "DRAFT");
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return count;
    }

    private int seedSectorGapAssessments(Sector sector) throws SQLException {
        int count = 10;
        String sql = "INSERT OR IGNORE INTO gap_assessments (id, name, sector, assessor, overall_score, status, rag_status) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= count; i++) {
                String id = sector.name() + "-GAP-" + String.format("%03d", i);
                double score = 30 + RNG.nextDouble() * 70;
                ps.setString(1, id);
                ps.setString(2, sector.displayName + " DPDP Assessment Q" + (1 + i % 4) + " 2026");
                ps.setString(3, sector.name());
                ps.setString(4, pick(FIRST_NAMES) + " " + pick(LAST_NAMES));
                ps.setDouble(5, Math.round(score * 10.0) / 10.0);
                ps.setString(6, score > 80 ? "COMPLETED" : "IN_PROGRESS");
                ps.setString(7, score > 80 ? "GREEN" : score > 50 ? "AMBER" : "RED");
                ps.addBatch();
            }
            ps.executeBatch();
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════
    // SECTOR-SPECIFIC SIEM EVENTS
    // ═══════════════════════════════════════════════════════════

    private static final Map<Sector, String[][]> SECTOR_SIEM_EVENTS = Map.of(
            Sector.BFSI, new String[][] {
                {"UPI fraud attempt detected on payment gateway", "FRAUD"},
                {"Core banking system unauthorized access attempt", "INTRUSION"},
                {"ATM skimming device detected at Branch-Mumbai", "PHYSICAL"},
                {"SWIFT message tampering attempt blocked", "NETWORK"},
                {"Bulk NEFT transfer from dormant account flagged", "ANOMALY"},
                {"RBI CSITE compliance check — encryption gaps detected", "COMPLIANCE"},
                {"KYC document download spike — insider threat indicator", "DATA_ACCESS"},
                {"Internet banking brute-force from blacklisted IP range", "AUTHENTICATION"},
                {"Credit card data found in unencrypted email attachment", "POLICY_VIOLATION"},
                {"Loan origination system — privilege escalation attempt", "AUTHORIZATION"}
            },
            Sector.HEALTHCARE, new String[][] {
                {"ABDM Health ID bulk query without valid consent", "COMPLIANCE"},
                {"Patient record accessed outside authorized department", "DATA_ACCESS"},
                {"Telemedicine session recording stored unencrypted", "POLICY_VIOLATION"},
                {"Lab report API — SQL injection attempt blocked", "NETWORK"},
                {"EHR system — unauthorized export of 500+ records", "DATA_ACCESS"},
                {"Pharmacy database — PII leak via API response", "DATA_ACCESS"},
                {"HIPAA-equivalent DPDP violation — cross-department sharing", "COMPLIANCE"},
                {"Medical device firmware — unauthorized remote access", "INTRUSION"},
                {"Vaccination portal — consent expiry for 1200 records", "COMPLIANCE"},
                {"Biometric access system — spoofing attempt detected", "AUTHENTICATION"}
            },
            Sector.ECOMMERCE, new String[][] {
                {"Payment gateway — card testing attack detected", "FRAUD"},
                {"Customer database — bulk scraping via API abuse", "DATA_ACCESS"},
                {"Delivery address data sent to unauthorized marketing partner", "POLICY_VIOLATION"},
                {"Session hijacking on checkout page detected", "NETWORK"},
                {"Product review manipulation — bot activity detected", "ANOMALY"},
                {"Cart abandonment data exported without consent basis", "COMPLIANCE"},
                {"Warehouse inventory system — ransomware indicator", "MALWARE"},
                {"Customer wishlist data shared with third-party analytics", "COMPLIANCE"},
                {"Coupon fraud — automated redemption from single IP", "FRAUD"},
                {"Mobile app — excessive permission request flagged", "POLICY_VIOLATION"}
            },
            Sector.GOVERNMENT, new String[][] {
                {"Aadhaar authentication API — rate limit breach", "COMPLIANCE"},
                {"DigiLocker — unauthorized document access attempt", "DATA_ACCESS"},
                {"e-Governance portal — XSS attack blocked by WAF", "NETWORK"},
                {"RTI response database — bulk download attempt", "DATA_ACCESS"},
                {"Voter ID verification system — enumeration attack", "AUTHENTICATION"},
                {"Tax filing portal — credential stuffing detected", "AUTHENTICATION"},
                {"Census data warehouse — unauthorized query pattern", "ANOMALY"},
                {"Subsidy disbursement — duplicate Aadhaar detection", "FRAUD"},
                {"CERT-In incident report — delayed submission alert", "COMPLIANCE"},
                {"Government email — phishing campaign targeting officials", "MALWARE"}
            },
            Sector.EDUCATION, new String[][] {
                {"Student portal — exam paper leak attempt detected", "DATA_ACCESS"},
                {"Minor student data accessed by unauthorized staff", "COMPLIANCE"},
                {"UDISE+ system — bulk student record export flagged", "DATA_ACCESS"},
                {"EdTech platform — parental consent expired for 800 minors", "COMPLIANCE"},
                {"Online exam proctoring — biometric data stored unencrypted", "POLICY_VIOLATION"},
                {"Fee payment gateway — card data retention violation", "COMPLIANCE"},
                {"Library system — patron reading data shared externally", "POLICY_VIOLATION"},
                {"Scholarship portal — identity fraud detected", "FRAUD"},
                {"Campus CCTV — footage accessed by non-security staff", "DATA_ACCESS"},
                {"Alumni database — email harvesting by marketing firm", "DATA_ACCESS"}
            },
            Sector.TELECOM, new String[][] {
                {"CDR data exported to unauthorized analytics vendor", "DATA_ACCESS"},
                {"SIM swap fraud — unauthorized number port detected", "FRAUD"},
                {"Cell tower location data — mass surveillance query", "COMPLIANCE"},
                {"Billing system — customer PII in debug logs", "POLICY_VIOLATION"},
                {"TRAI DND compliance — violation for 5000 numbers", "COMPLIANCE"},
                {"Network monitoring — deep packet inspection on PII", "POLICY_VIOLATION"},
                {"Subscriber verification API — enumeration attack", "AUTHENTICATION"},
                {"Roaming data — cross-border transfer without consent", "COMPLIANCE"},
                {"VoLTE system — eavesdropping attempt detected", "NETWORK"},
                {"Customer care portal — agent accessing records outside role", "AUTHORIZATION"}
            }
    );

    private static final Map<Sector, String[][]> SECTOR_DLP_POLICY_TEMPLATES = Map.of(
            Sector.BFSI, new String[][] {
                {"Card Number Protection", "Detect credit/debit card numbers in outbound channels", "CRITICAL"},
                {"IFSC Code Monitor", "Monitor IFSC bank codes in bulk transfers", "HIGH"},
                {"UPI ID Shield", "Block UPI IDs in unauthorized communications", "HIGH"},
                {"PAN Card Detection", "Detect PAN card numbers in emails and files", "CRITICAL"},
                {"Account Statement Guard", "Prevent bulk account statement exports", "HIGH"},
                {"SWIFT Message Protection", "Monitor SWIFT codes in external channels", "CRITICAL"},
                {"KYC Document Lock", "Block KYC documents from leaving secure zone", "HIGH"},
                {"Loan Data Protection", "Prevent loan sanction data leakage", "MEDIUM"}
            },
            Sector.HEALTHCARE, new String[][] {
                {"ABHA ID Protection", "Detect ABHA Health IDs in outbound data", "CRITICAL"},
                {"Patient MRN Guard", "Block Medical Record Numbers from external sharing", "CRITICAL"},
                {"Diagnosis Code Monitor", "Monitor ICD codes in non-clinical channels", "HIGH"},
                {"Prescription Data Lock", "Prevent prescription data exports", "HIGH"},
                {"Lab Report Shield", "Block lab reports from personal emails", "CRITICAL"},
                {"Vaccination Record Guard", "Protect vaccination records from bulk export", "MEDIUM"},
                {"Clinical Trial Data", "Block clinical trial PII from external transfer", "CRITICAL"},
                {"Insurance Claim Protection", "Monitor health insurance claims data", "HIGH"}
            },
            Sector.ECOMMERCE, new String[][] {
                {"Order ID Protection", "Track bulk order data exports", "HIGH"},
                {"CVV Detection", "Block CVV numbers in any channel", "CRITICAL"},
                {"Delivery Address Guard", "Prevent bulk address data sharing", "HIGH"},
                {"Customer Phone Shield", "Block customer phone numbers in marketing exports", "HIGH"},
                {"Payment Token Monitor", "Detect payment tokens in logs and emails", "CRITICAL"},
                {"Wishlist Data Lock", "Prevent wishlist data from reaching third parties", "MEDIUM"},
                {"Return Reason Protection", "Block sensitive return reason data exports", "MEDIUM"},
                {"Coupon Code Guard", "Monitor bulk coupon code distribution", "LOW"}
            },
            Sector.GOVERNMENT, new String[][] {
                {"Aadhaar Number Shield", "Detect and block Aadhaar numbers in all channels", "CRITICAL"},
                {"PAN Card Sentinel", "Monitor PAN card data in government communications", "CRITICAL"},
                {"Voter ID Guard", "Block voter ID data from external sharing", "HIGH"},
                {"Passport Data Lock", "Prevent passport data leakage", "CRITICAL"},
                {"RTI Document Control", "Monitor RTI response documents for PII", "HIGH"},
                {"Census Data Shield", "Block census micro-data from export", "CRITICAL"},
                {"Subsidy Beneficiary Guard", "Protect subsidy beneficiary data", "HIGH"},
                {"Digital Locker Monitor", "Track document access patterns in DigiLocker", "MEDIUM"}
            },
            Sector.EDUCATION, new String[][] {
                {"Student Roll Number Guard", "Block student roll numbers in bulk exports", "HIGH"},
                {"UDISE Code Shield", "Protect UDISE school codes from unauthorized use", "MEDIUM"},
                {"Scholarship ID Lock", "Block scholarship IDs from external sharing", "HIGH"},
                {"Minor Data Protection", "Prevent any minor's PII from leaving system", "CRITICAL"},
                {"Exam Score Shield", "Block bulk exam score exports", "HIGH"},
                {"Parent Contact Guard", "Protect parent contact info from marketing", "CRITICAL"},
                {"Attendance Record Lock", "Monitor attendance data bulk downloads", "MEDIUM"},
                {"Fee Payment Data Guard", "Block fee payment data from external channels", "HIGH"}
            },
            Sector.TELECOM, new String[][] {
                {"MSISDN Shield", "Block mobile numbers in bulk exports", "CRITICAL"},
                {"IMEI Detection", "Detect IMEI numbers in external communications", "HIGH"},
                {"CDR Data Lock", "Prevent CDR data from leaving secure network", "CRITICAL"},
                {"Location Data Guard", "Block cell tower location data exports", "CRITICAL"},
                {"Subscriber KYC Shield", "Protect subscriber KYC data", "HIGH"},
                {"Billing Data Monitor", "Monitor billing data exports for PII", "HIGH"},
                {"Network Config Guard", "Block network configuration data leaks", "MEDIUM"},
                {"SIM Card Data Lock", "Prevent SIM card data from unauthorized access", "HIGH"}
            }
    );

    private static final Map<Sector, String[]> SECTOR_RIGHTS_TYPES = Map.of(
            Sector.BFSI, new String[] {"Access bank account data", "Erase KYC records", "Port financial data", "Correct loan details", "Withdraw marketing consent", "Restrict credit scoring"},
            Sector.HEALTHCARE, new String[] {"Access health records", "Erase old prescriptions", "Port to new hospital", "Correct diagnosis records", "Withdraw telemedicine consent", "Restrict data sharing"},
            Sector.ECOMMERCE, new String[] {"Access purchase history", "Erase account data", "Download order data", "Correct shipping address", "Withdraw tracking consent", "Restrict recommendations"},
            Sector.GOVERNMENT, new String[] {"Access Aadhaar records", "Correct voter details", "Port digital documents", "Erase obsolete records", "Withdraw biometric consent", "Restrict data processing"},
            Sector.EDUCATION, new String[] {"Access academic records", "Erase alumni data", "Port transcripts", "Correct enrollment data", "Withdraw research consent", "Restrict minor data use"},
            Sector.TELECOM, new String[] {"Access CDR records", "Erase location history", "Port subscriber data", "Correct billing info", "Withdraw location consent", "Restrict usage analytics"}
    );

    // ═══════════════════════════════════════════════════════════
    // SIEM EVENTS SEEDING
    // ═══════════════════════════════════════════════════════════

    private int seedSectorSiemEvents(Sector sector) {
        int count = 100;
        String[][] templates = SECTOR_SIEM_EVENTS.get(sector);
        String[] sources = {"Firewall", "IDS/IPS", "WAF","Endpoint","Active Directory","Email Gateway","DLP Agent","API Gateway","Database Monitor","VPN Gateway"};
        String[] severities = {"LOW","MEDIUM","MEDIUM","HIGH","CRITICAL"};
        String sql = "INSERT OR IGNORE INTO siem_events (id, source, category, severity, description, timestamp, source_ip, destination_ip, user_id, raw_log) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                String[] tmpl = templates[i % templates.length];
                String id = sector.name() + "-SIEM-" + String.format("%04d", i + 1);
                ps.setString(1, id);
                ps.setString(2, pick(sources));
                ps.setString(3, tmpl[1]);
                ps.setString(4, pick(severities));
                ps.setString(5, tmpl[0] + " [" + pick(CITIES) + "]");
                ps.setString(6, randomTimestamp());
                ps.setString(7, "10." + RNG.nextInt(256) + "." + RNG.nextInt(256) + "." + RNG.nextInt(256));
                ps.setString(8, "192.168." + RNG.nextInt(256) + "." + RNG.nextInt(256));
                ps.setString(9, pick(FIRST_NAMES).toLowerCase() + "." + pick(LAST_NAMES).toLowerCase());
                ps.setString(10, "{\"sector\": \"" + sector.displayName + "\", \"event_id\": \"" + id + "\"}");
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            logger.warn("Could not seed SIEM events for {}: {}", sector.name(), e.getMessage());
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════
    // SIEM ALERTS SEEDING
    // ═══════════════════════════════════════════════════════════

    private int seedSectorSiemAlerts(Sector sector) {
        int count = 20;
        String[][] templates = SECTOR_SIEM_EVENTS.get(sector);
        String[] severities = {"MEDIUM","HIGH","HIGH","CRITICAL"};
        String[] statuses = {"OPEN","OPEN","ACKNOWLEDGED","INVESTIGATING","RESOLVED","CLOSED"};
        String[] categories = {"CORRELATION","THRESHOLD","ANOMALY","SIGNATURE","COMPLIANCE"};
        String sql = "INSERT OR IGNORE INTO siem_alerts (id, title, description, severity, status, category, rule_id, rule_name, created_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                String[] tmpl = templates[i % templates.length];
                String id = sector.name() + "-ALR-" + String.format("%04d", i + 1);
                ps.setString(1, id);
                ps.setString(2, sector.displayName + " — " + tmpl[1] + " Alert #" + (i+1));
                ps.setString(3, tmpl[0]);
                ps.setString(4, pick(severities));
                ps.setString(5, pick(statuses));
                ps.setString(6, pick(categories));
                ps.setString(7, "RULE-" + sector.name() + "-" + String.format("%03d", i + 1));
                ps.setString(8, sector.displayName + " " + tmpl[1] + " Rule");
                ps.setString(9, randomTimestamp());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            logger.warn("Could not seed SIEM alerts for {}: {}", sector.name(), e.getMessage());
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════
    // DLP POLICIES + INCIDENTS SEEDING
    // ═══════════════════════════════════════════════════════════

    private int seedSectorDlpData(Sector sector) {
        String[][] policies = SECTOR_DLP_POLICY_TEMPLATES.get(sector);
        String[] actions = {"BLOCK","QUARANTINE","ALERT","LOG"};
        int policyCount = policies.length; // 8
        int incidentCount = 30;

        // Seed DLP Policies
        String polSql = "INSERT OR IGNORE INTO dlp_policies (id, name, description, enabled, priority, protected_data_types, primary_action, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(polSql)) {
            for (int i = 0; i < policyCount; i++) {
                String id = sector.name() + "-DLP-POL-" + String.format("%03d", i + 1);
                ps.setString(1, id);
                ps.setString(2, policies[i][0]);
                ps.setString(3, policies[i][1]);
                ps.setInt(4, 1);
                ps.setInt(5, 50 + RNG.nextInt(50));
                ps.setString(6, "PII," + sector.displayName);
                ps.setString(7, "CRITICAL".equals(policies[i][2]) ? "BLOCK" : "HIGH".equals(policies[i][2]) ? "QUARANTINE" : "ALERT");
                ps.setString(8, randomTimestamp());
                ps.setString(9, randomTimestamp());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            logger.warn("Could not seed DLP policies for {}: {}", sector.name(), e.getMessage());
        }

        // Seed DLP Incidents
        String incSql = "INSERT OR IGNORE INTO dlp_incidents (id, policy_id, policy_name, source_user, destination_type, severity, status, action_taken, detected_at, resolved_at) VALUES (?,?,?,?,?,?,?,?,?,?)";
        String[] channels = {"EMAIL","FILE_TRANSFER","USB","PRINT","NETWORK","CLOUD"};
        String[] incStatuses = {"OPEN","OPEN","INVESTIGATING","RESOLVED","FALSE_POSITIVE"};
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(incSql)) {
            for (int i = 0; i < incidentCount; i++) {
                String[] pol = policies[i % policyCount];
                String id = sector.name() + "-DLP-INC-" + String.format("%04d", i + 1);
                String status = pick(incStatuses);
                String ts = randomTimestamp();
                ps.setString(1, id);
                ps.setString(2, sector.name() + "-DLP-POL-" + String.format("%03d", (i % policyCount) + 1));
                ps.setString(3, pol[0]);
                ps.setString(4, pick(FIRST_NAMES).toLowerCase() + "." + pick(LAST_NAMES).toLowerCase());
                ps.setString(5, pick(channels));
                ps.setString(6, pol[2]);
                ps.setString(7, status);
                ps.setString(8, pick(actions));
                ps.setString(9, ts);
                ps.setString(10, "RESOLVED".equals(status) ? ts : null);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            logger.warn("Could not seed DLP incidents for {}: {}", sector.name(), e.getMessage());
        }

        return policyCount + incidentCount;
    }

    // ═══════════════════════════════════════════════════════════
    // RIGHTS REQUESTS SEEDING
    // ═══════════════════════════════════════════════════════════

    private int seedSectorRightsRequests(Sector sector) {
        int count = 50;
        String[] types = SECTOR_RIGHTS_TYPES.get(sector);
        String[] statuses = {"PENDING","IN_PROGRESS","COMPLETED","COMPLETED","REJECTED"};
        String[] priorities = {"LOW","NORMAL","NORMAL","HIGH","URGENT"};
        String sql = "INSERT OR IGNORE INTO rights_requests (id, reference_number, data_principal_id, request_type, description, status, priority, received_at, deadline, assigned_to) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                String id = sector.name() + "-DSR-" + String.format("%04d", i + 1);
                String type = pick(types);
                int dpIdx = 1 + RNG.nextInt(100);
                String ts = randomTimestamp();
                ps.setString(1, id);
                ps.setString(2, "DSR-" + sector.name() + "-" + String.format("%04d", i + 1));
                ps.setString(3, sector.name() + "-DP-" + String.format("%04d", dpIdx));
                ps.setString(4, type.contains("Access") ? "ACCESS" : type.contains("Erase") ? "ERASURE" : type.contains("Port") ? "PORTABILITY" : type.contains("Correct") ? "CORRECTION" : "WITHDRAWAL");
                ps.setString(5, type + " — " + sector.displayName + " data principal request under DPDP Act 2023 Sec 11-14");
                ps.setString(6, pick(statuses));
                ps.setString(7, pick(priorities));
                ps.setString(8, ts);
                ps.setString(9, ts); // deadline same for simplicity
                ps.setString(10, pick(FIRST_NAMES) + " " + pick(LAST_NAMES));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            logger.warn("Could not seed rights requests for {}: {}", sector.name(), e.getMessage());
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════
    // AUDIT LOG SEEDING
    // ═══════════════════════════════════════════════════════════

    private int seedSectorAuditLog(Sector sector) {
        int count = 40;
        String[] actions = {"CONSENT_COLLECTED","CONSENT_WITHDRAWN","DATA_ACCESSED","DATA_EXPORTED",
                "BREACH_REPORTED","DPIA_CREATED","POLICY_UPDATED","USER_LOGIN","RIGHTS_REQUEST_CREATED",
                "DLP_INCIDENT","SIEM_ALERT_TRIGGERED","SETTING_CHANGED","ROLE_ASSIGNED","REPORT_GENERATED"};
        String sql = "INSERT OR IGNORE INTO audit_log (id, action, module, performed_by, details, created_at) VALUES (?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                String id = sector.name() + "-AUD-" + String.format("%04d", i + 1);
                String action = pick(actions);
                ps.setString(1, id);
                ps.setString(2, action);
                ps.setString(3, action.startsWith("CONSENT") ? "CONSENT" : action.startsWith("BREACH") ? "BREACH" : action.startsWith("DLP") ? "DLP" : action.startsWith("SIEM") ? "SIEM" : "SYSTEM");
                ps.setString(4, pick(FIRST_NAMES).toLowerCase() + "." + pick(LAST_NAMES).toLowerCase());
                ps.setString(5, sector.displayName + " — " + action.replace("_", " ").toLowerCase() + " activity for DPDP compliance");
                ps.setString(6, randomTimestamp());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) {
            logger.warn("Could not seed audit log for {}: {}", sector.name(), e.getMessage());
        }
        return count;
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private String pick(String[] arr) {
        return arr[RNG.nextInt(arr.length)];
    }

    private String generateExternalId(Sector sector, int i) {
        return switch (sector) {
            case BFSI -> "CIF" + String.format("%010d", 1000000000L + i);
            case HEALTHCARE -> "ABHA-" + String.format("%014d", 10000000000000L + i);
            case ECOMMERCE -> "CUST" + String.format("%08d", 10000000 + i);
            case GOVERNMENT -> String.format("%04d", 1000 + i % 9000) + " " + String.format("%04d", RNG.nextInt(9999))
                    + " " + String.format("%04d", RNG.nextInt(9999));
            case EDUCATION -> "STU" + String.format("%010d", 2024000000L + i);
            case TELECOM -> "+91" + (9000000000L + i);
            case INSURANCE -> "POL" + String.format("%010d", 5000000000L + i);
            case FINTECH -> "FIN" + String.format("%08d", 20000000 + i);
            case MANUFACTURING -> "MFG" + String.format("%06d", 100000 + i);
            case ENERGY -> "MTR" + String.format("%010d", 3000000000L + i);
            case TRANSPORT -> "VEH" + String.format("%08d", 40000000 + i);
            case MEDIA -> "USR" + String.format("%010d", 6000000000L + i);
            case AGRICULTURE -> "KIS" + String.format("%012d", 100000000000L + i);
            case PHARMA -> "CLN" + String.format("%010d", 7000000000L + i);
            case REALESTATE -> "PRP" + String.format("%08d", 50000000 + i);
            case LEGAL -> "CLI" + String.format("%06d", 200000 + i);
            case HOSPITALITY -> "GST" + String.format("%08d", 60000000 + i);
            case SOCIALMEDIA -> "USR" + String.format("%010d", 8000000000L + i);
        };
    }

    private String randomTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        return now.minusDays(RNG.nextInt(365)).minusHours(RNG.nextInt(24)).format(FMT);
    }

    /** Get total records seeded across all sectors */
    public int getTotalRecordCount() {
        // 18 sectors × ~668 records each
        return 18 * 668;
    }
}
