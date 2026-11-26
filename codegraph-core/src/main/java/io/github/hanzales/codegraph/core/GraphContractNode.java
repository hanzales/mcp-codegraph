package io.github.hanzales.codegraph.core;

import java.util.Map;

/** Versioned, language-neutral node representation. */
public record GraphContractNode(
        String id,
        String logicalId,
        String repositoryId,
        String snapshotId,
        GraphEntityKind kind,
        String name,
        String qualifiedName,
        String language,
        String analyzer,
        String analyzerVersion,
        GraphSourceRange sourceRange,
        Map<String, Object> metadata) {
    public GraphContractNode {
        if (id == null || id.isBlank() || logicalId == null || logicalId.isBlank()) throw new IllegalArgumentException("Node identifiers are required");
        if (repositoryId == null || repositoryId.isBlank() || snapshotId == null || snapshotId.isBlank()) throw new IllegalArgumentException("Repository and snapshot identity are required");
        if (kind == null || language == null || language.isBlank() || analyzer == null || analyzer.isBlank() || analyzerVersion == null || analyzerVersion.isBlank()) throw new IllegalArgumentException("Node kind and analyzer metadata are required");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
