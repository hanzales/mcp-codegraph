package io.github.hanzales.codegraph.core;

import java.util.List;
import java.util.Map;

public record RepositoryDefinition(
        String repositoryId,
        String displayName,
        String localPath,
        String language,
        String branch,
        String serviceIdentity,
        List<String> includePatterns,
        List<String> excludePatterns,
        Map<String, String> analyzerConfiguration) {
    public RepositoryDefinition {
        if (repositoryId == null || repositoryId.isBlank() || displayName == null || displayName.isBlank() || localPath == null || localPath.isBlank()) throw new IllegalArgumentException("Repository identity, path, and display name are required");
        language = language == null || language.isBlank() ? "auto" : language.toLowerCase();
        branch = branch == null ? "" : branch;
        serviceIdentity = serviceIdentity == null ? "" : serviceIdentity;
        includePatterns = includePatterns == null ? List.of() : List.copyOf(includePatterns);
        excludePatterns = excludePatterns == null ? List.of() : List.copyOf(excludePatterns);
        analyzerConfiguration = analyzerConfiguration == null ? Map.of() : Map.copyOf(analyzerConfiguration);
    }
}
