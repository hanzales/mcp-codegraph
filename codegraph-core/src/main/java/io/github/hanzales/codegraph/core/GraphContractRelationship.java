package io.github.hanzales.codegraph.core;

import java.util.List;
import java.util.Map;

/** Versioned relationship with multiple evidence records and explicit uncertainty. */
public record GraphContractRelationship(
        String id,
        String sourceId,
        String targetId,
        String type,
        ResolutionStatus resolution,
        Confidence confidence,
        List<GraphEvidence> evidence,
        Map<String, Object> metadata) {
    public GraphContractRelationship {
        if (id == null || id.isBlank() || sourceId == null || sourceId.isBlank() || targetId == null || targetId.isBlank()) throw new IllegalArgumentException("Relationship identifiers are required");
        if (type == null || type.isBlank() || resolution == null || confidence == null) throw new IllegalArgumentException("Relationship classification is required");
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
