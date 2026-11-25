package io.github.hanzales.codegraph.graph;

import io.github.hanzales.codegraph.core.*;
import java.util.*;

public final class GraphImpactAnalyzer implements ImpactAnalyzer {
    private final GraphRepository graph;
    public GraphImpactAnalyzer(GraphRepository graph) { this.graph = graph; }
    public ImpactResult analyze(String symbolId, int maxDepth, int maxResults) {
        var subject = graph.findNode(symbolId).map(GraphNode::qualifiedName).orElse(symbolId);
        var impacted = new LinkedHashMap<String, GraphNode>();
        var paths = new ArrayList<List<String>>();
        var queue = new ArrayDeque<List<String>>();
        queue.add(List.of(symbolId));
        while (!queue.isEmpty() && impacted.size() < maxResults) {
            var path = queue.removeFirst();
            if (path.size() - 1 >= maxDepth) continue;
            var current = path.getLast();
            for (var edge : graph.relationshipsTo(current)) {
                if (path.contains(edge.sourceId())) continue;
                var next = new ArrayList<>(path); next.add(edge.sourceId());
                graph.findNode(edge.sourceId()).ifPresent(n -> impacted.putIfAbsent(n.id(), n));
                if (paths.size() < maxResults) paths.add(List.copyOf(next));
                queue.add(next);
            }
        }
        var api = impacted.values().stream().filter(n -> n.kind() == NodeKind.API_ENDPOINT).count();
        var writes = impacted.values().stream().filter(n -> n.kind() == NodeKind.DATABASE_TABLE).count();
        var risk = api > 0 || writes > 0 || impacted.size() > 50 ? "HIGH" : impacted.size() > 10 ? "MEDIUM" : "LOW";
        return new ImpactResult(subject, risk, List.copyOf(impacted.values()), paths, List.of("Static relationships do not guarantee runtime execution.", "Unresolved external and reflection-based calls may be missing."));
    }
}
