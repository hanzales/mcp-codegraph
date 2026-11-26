package io.github.hanzales.codegraph.core;

import java.util.List;
import java.util.Map;

/** Deterministic interchange document for software knowledge graphs. */
public record GraphContractDocument(
        String contract,
        int schemaVersion,
        String repositoryId,
        String snapshotId,
        String commitHash,
        Map<String, String> analyzer,
        List<GraphContractNode> nodes,
        List<GraphContractRelationship> relationships,
        Map<String, Object> metadata) {
    public static final String CONTRACT_NAME = "mcp-codegraph.graph";
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public GraphContractDocument {
        if (!CONTRACT_NAME.equals(contract) || schemaVersion != CURRENT_SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported graph contract version");
        if (repositoryId == null || repositoryId.isBlank() || snapshotId == null || snapshotId.isBlank()) throw new IllegalArgumentException("Repository and snapshot identity are required");
        analyzer = analyzer == null ? Map.of() : Map.copyOf(analyzer); nodes = nodes == null ? List.of() : List.copyOf(nodes); relationships = relationships == null ? List.of() : List.copyOf(relationships); metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
