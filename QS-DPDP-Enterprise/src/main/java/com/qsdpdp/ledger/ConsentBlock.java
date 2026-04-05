package com.qsdpdp.ledger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Immutable Consent Block for the Consent Ledger Network (CLN).
 * Each block contains a consent record hash-chained to the previous block,
 * forming a tamper-proof, append-only ledger.
 *
 * Structure mirrors blockchain block design per ISO/TC 307 (Blockchain standards).
 *
 * @version 1.0.0
 * @since Universal Trust OS v3.0
 */
public class ConsentBlock {

    private final long blockIndex;
    private final String blockId;
    private final Instant timestamp;
    private final String consentId;
    private final String dataPrincipalHash;  // SHA3-256 hash of Data Principal identity
    private final String fiduciaryId;
    private final String purpose;
    private final String consentAction;       // GRANT, REVOKE, MODIFY, RENEW
    private final String dataHash;            // SHA3-256 hash of full consent payload
    private final String previousBlockHash;
    private final String blockHash;
    private final String merkleRoot;
    private final byte[] signature;           // ML-DSA-87 or RSA-4096 signature
    private final String signatureAlgorithm;
    private final Map<String, String> metadata;

    private ConsentBlock(Builder builder) {
        this.blockIndex = builder.blockIndex;
        this.blockId = builder.blockId;
        this.timestamp = builder.timestamp;
        this.consentId = builder.consentId;
        this.dataPrincipalHash = builder.dataPrincipalHash;
        this.fiduciaryId = builder.fiduciaryId;
        this.purpose = builder.purpose;
        this.consentAction = builder.consentAction;
        this.dataHash = builder.dataHash;
        this.previousBlockHash = builder.previousBlockHash;
        this.merkleRoot = builder.merkleRoot;
        this.signature = builder.signature;
        this.signatureAlgorithm = builder.signatureAlgorithm;
        this.metadata = Collections.unmodifiableMap(builder.metadata);
        this.blockHash = computeBlockHash();
    }

    /**
     * Compute SHA3-256 hash of block contents for chain integrity.
     */
    private String computeBlockHash() {
        try {
            String content = blockIndex + "|" + timestamp.toEpochMilli() + "|" +
                    consentId + "|" + dataPrincipalHash + "|" + fiduciaryId + "|" +
                    purpose + "|" + consentAction + "|" + dataHash + "|" +
                    previousBlockHash + "|" + merkleRoot;
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            // Fallback to SHA-256 if SHA3 not available
            try {
                String content = blockIndex + "|" + timestamp.toEpochMilli() + "|" +
                        consentId + "|" + dataHash + "|" + previousBlockHash;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            } catch (Exception ex) {
                throw new RuntimeException("Hash computation failed", ex);
            }
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /**
     * Verify block integrity by recomputing hash.
     */
    public boolean verifyIntegrity() {
        String recomputed = computeBlockHash();
        return recomputed.equals(this.blockHash);
    }

    /**
     * Verify chain linkage — this block's previousBlockHash matches parent's blockHash.
     */
    public boolean verifyChainLink(ConsentBlock previousBlock) {
        if (previousBlock == null) return "0".repeat(64).equals(previousBlockHash);
        return previousBlock.getBlockHash().equals(this.previousBlockHash);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("blockIndex", blockIndex);
        map.put("blockId", blockId);
        map.put("timestamp", timestamp.toString());
        map.put("consentId", consentId);
        map.put("dataPrincipalHash", dataPrincipalHash);
        map.put("fiduciaryId", fiduciaryId);
        map.put("purpose", purpose);
        map.put("consentAction", consentAction);
        map.put("dataHash", dataHash);
        map.put("previousBlockHash", previousBlockHash);
        map.put("blockHash", blockHash);
        map.put("merkleRoot", merkleRoot);
        map.put("signatureAlgorithm", signatureAlgorithm);
        map.put("signaturePresent", signature != null && signature.length > 0);
        map.put("metadata", metadata);
        return map;
    }

    // Getters
    public long getBlockIndex() { return blockIndex; }
    public String getBlockId() { return blockId; }
    public Instant getTimestamp() { return timestamp; }
    public String getConsentId() { return consentId; }
    public String getDataPrincipalHash() { return dataPrincipalHash; }
    public String getFiduciaryId() { return fiduciaryId; }
    public String getPurpose() { return purpose; }
    public String getConsentAction() { return consentAction; }
    public String getDataHash() { return dataHash; }
    public String getPreviousBlockHash() { return previousBlockHash; }
    public String getBlockHash() { return blockHash; }
    public String getMerkleRoot() { return merkleRoot; }
    public byte[] getSignature() { return signature; }
    public String getSignatureAlgorithm() { return signatureAlgorithm; }
    public Map<String, String> getMetadata() { return metadata; }

    public static class Builder {
        private long blockIndex;
        private String blockId = UUID.randomUUID().toString();
        private Instant timestamp = Instant.now();
        private String consentId;
        private String dataPrincipalHash;
        private String fiduciaryId;
        private String purpose;
        private String consentAction = "GRANT";
        private String dataHash;
        private String previousBlockHash = "0".repeat(64);
        private String merkleRoot = "";
        private byte[] signature = new byte[0];
        private String signatureAlgorithm = "NONE";
        private Map<String, String> metadata = new LinkedHashMap<>();

        public Builder blockIndex(long i) { this.blockIndex = i; return this; }
        public Builder blockId(String id) { this.blockId = id; return this; }
        public Builder timestamp(Instant t) { this.timestamp = t; return this; }
        public Builder consentId(String id) { this.consentId = id; return this; }
        public Builder dataPrincipalHash(String h) { this.dataPrincipalHash = h; return this; }
        public Builder fiduciaryId(String id) { this.fiduciaryId = id; return this; }
        public Builder purpose(String p) { this.purpose = p; return this; }
        public Builder consentAction(String a) { this.consentAction = a; return this; }
        public Builder dataHash(String h) { this.dataHash = h; return this; }
        public Builder previousBlockHash(String h) { this.previousBlockHash = h; return this; }
        public Builder merkleRoot(String r) { this.merkleRoot = r; return this; }
        public Builder signature(byte[] s) { this.signature = s; return this; }
        public Builder signatureAlgorithm(String a) { this.signatureAlgorithm = a; return this; }
        public Builder metadata(String key, String value) { this.metadata.put(key, value); return this; }
        public ConsentBlock build() { return new ConsentBlock(this); }
    }
}
