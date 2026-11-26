package io.github.hanzales.codegraph.graph;

import io.github.hanzales.codegraph.core.*;
import java.util.Collection;

/** Optional capabilities implemented by durable graph providers without burdening the core GraphRepository contract. */
public interface PersistentGraphStore {
    void initializeSchema();
    void upsertNodes(Collection<GraphNode> nodes);
    void addRelationships(Collection<GraphRelationship> relationships);
    void clearRepository(String workspaceId, String repositoryId);
    void clearWorkspace(String workspaceId);
    GraphStorageStats statistics(String workspaceId, String repositoryId);
    GraphHealth health();
}
