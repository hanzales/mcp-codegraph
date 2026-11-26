package io.github.hanzales.codegraph.core;

import java.time.Instant;

/** Immutable provenance and lifecycle metadata for one graph snapshot. */
public record RepositorySnapshotMetadata(
        String workspaceId, String repositoryId, String snapshotId, String indexingRunId,
        String branch, String commitHash, boolean dirty, String workingTreeFingerprint,
        String sourceFingerprint, Instant indexedAt, String analyzerVersion,
        String graphSchemaVersion, SnapshotStatus status, int fileCount, int warningCount,
        int errorCount, boolean stale, boolean exactCommittedSnapshot) {
    public RepositorySnapshotMetadata {
        branch = branch == null ? "" : branch;
        commitHash = commitHash == null ? "" : commitHash;
        workingTreeFingerprint = workingTreeFingerprint == null ? "" : workingTreeFingerprint;
        sourceFingerprint = sourceFingerprint == null ? "" : sourceFingerprint;
        analyzerVersion = analyzerVersion == null ? "unknown" : analyzerVersion;
        graphSchemaVersion = graphSchemaVersion == null ? "1" : graphSchemaVersion;
        status = status == null ? SnapshotStatus.FAILED : status;
    }
}
