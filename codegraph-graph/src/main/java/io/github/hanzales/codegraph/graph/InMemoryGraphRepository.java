package io.github.hanzales.codegraph.graph;

import io.github.hanzales.codegraph.core.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryGraphRepository implements FileScopedGraphRepository {
    private final Map<String, GraphNode> nodes = new ConcurrentHashMap<>();
    private final List<GraphRelationship> relationships = new CopyOnWriteArrayList<>();
    public void clear() { nodes.clear(); relationships.clear(); }
    public void removeSourceFiles(java.util.Set<String> relativePaths) {
        nodes.values().removeIf(node -> relativePaths.contains(node.file()));
        relationships.removeIf(edge -> relativePaths.contains(edge.file()) || !nodes.containsKey(edge.sourceId()) || !nodes.containsKey(edge.targetId()));
    }
    public void upsertNode(GraphNode node) { nodes.put(node.id(), node); }
    public void addRelationship(GraphRelationship relationship) { relationships.add(relationship); }
    public Optional<GraphNode> findNode(String id) { return Optional.ofNullable(nodes.get(id)); }
    public List<GraphNode> searchSymbols(String query, int limit) { var q = query.toLowerCase(); return nodes.values().stream().filter(n -> n.name().toLowerCase().contains(q) || n.qualifiedName().toLowerCase().contains(q)).limit(Math.max(0, limit)).toList(); }
    public List<GraphNode> nodes() { return List.copyOf(nodes.values()); }
    public List<GraphRelationship> relationships() { return List.copyOf(relationships); }
    public List<GraphRelationship> relationshipsFrom(String nodeId) { return relationships.stream().filter(r -> r.sourceId().equals(nodeId)).toList(); }
    public List<GraphRelationship> relationshipsTo(String nodeId) { return relationships.stream().filter(r -> r.targetId().equals(nodeId)).toList(); }
}
