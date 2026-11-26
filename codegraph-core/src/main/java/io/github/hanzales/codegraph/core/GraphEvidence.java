package io.github.hanzales.codegraph.core;

/** Source-backed evidence. Paths are repository-relative and never absolute. */
public record GraphEvidence(String repositoryId, String commitHash, String relativeFilePath, GraphSourceRange sourceRange, String rationale) {
    public GraphEvidence {
        if (repositoryId == null || repositoryId.isBlank()) throw new IllegalArgumentException("repositoryId is required");
        if (relativeFilePath == null || relativeFilePath.isBlank() || relativeFilePath.startsWith("/")) throw new IllegalArgumentException("relativeFilePath must be relative");
        rationale = rationale == null ? "" : rationale;
    }
}
