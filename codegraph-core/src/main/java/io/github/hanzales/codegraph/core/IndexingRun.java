package io.github.hanzales.codegraph.core;

import java.time.Instant;

/** Correlates one indexing attempt independently from its resulting snapshot. */
public record IndexingRun(String runId, String workspaceId, String repositoryId,
                          Instant startedAt, Instant finishedAt, SnapshotStatus status,
                          String error) {
    public IndexingRun { error = error == null ? "" : error; }
}
