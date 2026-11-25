package io.github.hanzales.codegraph.core;

import java.util.List;
import java.util.Optional;

public interface GraphRepository {
    void clear();
    void upsertNode(GraphNode node);
    void addRelationship(GraphRelationship relationship);
    Optional<GraphNode> findNode(String id);
    List<GraphNode> searchSymbols(String query, int limit);
    List<GraphNode> nodes();
    List<GraphRelationship> relationships();
    List<GraphRelationship> relationshipsFrom(String nodeId);
    List<GraphRelationship> relationshipsTo(String nodeId);
}
