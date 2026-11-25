package io.github.hanzales.codegraph.core;

import java.time.Instant;
import java.util.List;

public record IndexStatus(String repositoryId, String root, String commit, int fileCount, int symbolCount, int relationshipCount, long durationMs, Instant indexedAt, List<AnalysisWarning> warnings) {
    public IndexStatus { warnings = warnings == null ? List.of() : List.copyOf(warnings); }
}
