package io.github.hanzales.codegraph.core;

import java.time.Instant;
import java.util.List;

public record RepositoryIndexStatus(
        String workspaceId,
        String repositoryId,
        String displayName,
        String canonicalPath,
        String detectedLanguage,
        RepositoryIndexState state,
        int fileCount,
        int symbolCount,
        int relationshipCount,
        long durationMs,
        Instant indexedAt,
        List<AnalysisWarning> warnings,
        String error,
        String activeSnapshotId,
        boolean stale) {
    public RepositoryIndexStatus(String workspaceId, String repositoryId, String displayName, String canonicalPath,
                                 String detectedLanguage, RepositoryIndexState state, int fileCount, int symbolCount,
                                 int relationshipCount, long durationMs, Instant indexedAt, List<AnalysisWarning> warnings,
                                 String error) {
        this(workspaceId, repositoryId, displayName, canonicalPath, detectedLanguage, state, fileCount, symbolCount,
                relationshipCount, durationMs, indexedAt, warnings, error, "", false);
    }
    public RepositoryIndexStatus {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        error = error == null ? "" : error;
    }
}
