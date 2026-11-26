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
        String error) {
    public RepositoryIndexStatus {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        error = error == null ? "" : error;
    }
}
