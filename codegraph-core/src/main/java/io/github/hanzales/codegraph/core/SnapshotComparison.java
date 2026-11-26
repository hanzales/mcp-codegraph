package io.github.hanzales.codegraph.core;

import java.util.List;

/** Deterministic set comparison between two immutable graph snapshots. */
public record SnapshotComparison(String workspaceId, String repositoryId,
                                 String leftSnapshotId, String rightSnapshotId,
                                 List<String> addedNodeIds, List<String> removedNodeIds,
                                 List<String> addedRelationshipKeys, List<String> removedRelationshipKeys,
                                 boolean sameSourceState, List<String> limitations) {
    public SnapshotComparison {
        addedNodeIds = List.copyOf(addedNodeIds == null ? List.of() : addedNodeIds);
        removedNodeIds = List.copyOf(removedNodeIds == null ? List.of() : removedNodeIds);
        addedRelationshipKeys = List.copyOf(addedRelationshipKeys == null ? List.of() : addedRelationshipKeys);
        removedRelationshipKeys = List.copyOf(removedRelationshipKeys == null ? List.of() : removedRelationshipKeys);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }
}
