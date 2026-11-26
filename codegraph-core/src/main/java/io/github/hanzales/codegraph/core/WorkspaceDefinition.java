package io.github.hanzales.codegraph.core;

import java.util.Map;

public record WorkspaceDefinition(String workspaceId, String displayName, Map<String, String> metadata) {
    public WorkspaceDefinition {
        if (workspaceId == null || workspaceId.isBlank() || displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Workspace identity and display name are required");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
