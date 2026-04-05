package com.qsdpdp.ledger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Merkle Tree implementation for Consent Ledger audit proofs.
 * Enables O(log n) verification of consent records without downloading entire chain.
 * ISO/TC 307 compliant tamper-evidence structure.
 *
 * @version 1.0.0
 * @since Universal Trust OS v3.0
 */
public class MerkleTree {

    private final List<String> leaves;
    private final List<List<String>> tree;
    private final String root;

    /**
     * Construct Merkle tree from a list of data hashes (leaf nodes).
     */
    public MerkleTree(List<String> dataHashes) {
        if (dataHashes == null || dataHashes.isEmpty()) {
            this.leaves = Collections.emptyList();
            this.tree = Collections.emptyList();
            this.root = "0".repeat(64);
            return;
        }

        this.leaves = new ArrayList<>(dataHashes);
        this.tree = new ArrayList<>();

        // Level 0 = leaves
        List<String> currentLevel = new ArrayList<>(dataHashes);
        tree.add(currentLevel);

        // Build tree bottom-up
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                nextLevel.add(hashPair(left, right));
            }
            tree.add(nextLevel);
            currentLevel = nextLevel;
        }

        this.root = currentLevel.get(0);
    }

    /**
     * Generate audit proof (Merkle path) for a given leaf index.
     * The proof contains sibling hashes needed to recompute the root.
     */
    public MerkleProof generateProof(int leafIndex) {
        if (leafIndex < 0 || leafIndex >= leaves.size()) {
            return null;
        }

        List<ProofNode> path = new ArrayList<>();
        int index = leafIndex;

        for (int level = 0; level < tree.size() - 1; level++) {
            List<String> currentLevel = tree.get(level);
            boolean isRight = (index % 2 == 1);
            int siblingIndex = isRight ? index - 1 : index + 1;

            if (siblingIndex < currentLevel.size()) {
                path.add(new ProofNode(
                        currentLevel.get(siblingIndex),
                        isRight ? "LEFT" : "RIGHT"
                ));
            } else {
                // Duplicate (odd node)
                path.add(new ProofNode(currentLevel.get(index), "LEFT"));
            }
            index = index / 2;
        }

        return new MerkleProof(leaves.get(leafIndex), root, path, leafIndex);
    }

    /**
     * Verify a Merkle proof against the tree root.
     * Can be performed offline / air-gapped — only needs the proof and root hash.
     */
    public static boolean verifyProof(MerkleProof proof) {
        if (proof == null) return false;

        String computedHash = proof.getLeafHash();
        for (ProofNode node : proof.getPath()) {
            if ("LEFT".equals(node.getPosition())) {
                computedHash = hashPair(node.getHash(), computedHash);
            } else {
                computedHash = hashPair(computedHash, node.getHash());
            }
        }
        return computedHash.equals(proof.getExpectedRoot());
    }

    /**
     * Hash two nodes together: SHA3-256(left || right).
     */
    private static String hashPair(String left, String right) {
        try {
            String combined = left + right;
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA3-256");
            } catch (Exception e) {
                digest = MessageDigest.getInstance("SHA-256");
            }
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Merkle hash failed", e);
        }
    }

    public String getRoot() { return root; }
    public int getLeafCount() { return leaves.size(); }
    public int getTreeDepth() { return tree.size(); }
    public List<String> getLeaves() { return Collections.unmodifiableList(leaves); }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("root", root);
        map.put("leafCount", leaves.size());
        map.put("treeDepth", tree.size());
        map.put("algorithm", "SHA3-256");
        return map;
    }

    // ── Data Classes ──

    public static class MerkleProof {
        private final String leafHash;
        private final String expectedRoot;
        private final List<ProofNode> path;
        private final int leafIndex;

        public MerkleProof(String leafHash, String expectedRoot, List<ProofNode> path, int leafIndex) {
            this.leafHash = leafHash;
            this.expectedRoot = expectedRoot;
            this.path = Collections.unmodifiableList(path);
            this.leafIndex = leafIndex;
        }

        public String getLeafHash() { return leafHash; }
        public String getExpectedRoot() { return expectedRoot; }
        public List<ProofNode> getPath() { return path; }
        public int getLeafIndex() { return leafIndex; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("leafHash", leafHash);
            map.put("leafIndex", leafIndex);
            map.put("expectedRoot", expectedRoot);
            map.put("pathLength", path.size());
            List<Map<String, String>> pathMaps = new ArrayList<>();
            for (ProofNode n : path) {
                pathMaps.add(Map.of("hash", n.getHash(), "position", n.getPosition()));
            }
            map.put("path", pathMaps);
            map.put("verificationComplexity", "O(log n) — " + path.size() + " hash operations");
            return map;
        }
    }

    public static class ProofNode {
        private final String hash;
        private final String position; // LEFT or RIGHT

        public ProofNode(String hash, String position) {
            this.hash = hash;
            this.position = position;
        }

        public String getHash() { return hash; }
        public String getPosition() { return position; }
    }
}
