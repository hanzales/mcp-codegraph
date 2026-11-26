package io.github.hanzales.codegraph.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Deterministic identifiers that separate repositories, languages, logical symbols, and snapshots. */
public final class GraphIdentifiers {
    private GraphIdentifiers() { }
    public static String repository(String canonicalRoot) { return "repo:" + digest(canonicalRoot); }
    public static String snapshot(String repositoryId, String revision) { return "snapshot:" + digest(repositoryId + "\u0000" + (revision == null ? "working-tree" : revision)); }
    public static String logical(String language, String qualifiedSymbol) { return language.toLowerCase() + ":" + qualifiedSymbol; }
    public static String node(String repositoryId, String snapshotId, String logicalId) { return "node:" + digest(repositoryId + "\u0000" + snapshotId + "\u0000" + logicalId); }
    public static String relationship(String sourceId, String targetId, String type) { return "rel:" + digest(sourceId + "\u0000" + targetId + "\u0000" + type); }
    private static String digest(String value) { try { var bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); var out = new StringBuilder(64); for (var b : bytes) out.append(String.format("%02x", b)); return out.toString(); } catch (Exception e) { throw new IllegalStateException("SHA-256 is unavailable", e); } }
}
