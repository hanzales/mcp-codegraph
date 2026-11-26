package io.github.hanzales.codegraph.core;

import java.time.Instant;
import java.util.List;

public record WorkspaceIndexStatus(String workspaceId, String displayName, int repositoryCount, int indexedCount, int partialCount, int failedCount, Instant updatedAt, List<RepositoryIndexStatus> repositories) {
    public WorkspaceIndexStatus { repositories = repositories == null ? List.of() : List.copyOf(repositories); }
}
