package io.github.hanzales.codegraph.mcp;

import io.github.hanzales.codegraph.core.*;
import io.github.hanzales.codegraph.graph.GraphImpactAnalyzer;
import io.github.hanzales.codegraph.indexer.ProjectIndexer;
import java.nio.file.Path;
import java.util.*;

/** Application service used by MCP adapters. It never executes repository code. */
public final class CodeGraphToolService {
    private final GraphRepository graph; private final ProjectIndexer indexer; private IndexStatus status;
    private final io.github.hanzales.codegraph.indexer.GitDiffAnalyzer diffAnalyzer = new io.github.hanzales.codegraph.indexer.GitDiffAnalyzer();
    public CodeGraphToolService(GraphRepository graph, ProjectIndexer indexer) { this.graph = graph; this.indexer = indexer; }
    public synchronized IndexStatus scanProject(String path) throws Exception { status = indexer.index(Path.of(path)); return status; }
    public IndexStatus getIndexStatus() { return status; }
    public List<GraphNode> searchSymbols(String query, int limit) { return graph.searchSymbols(query, Math.min(Math.max(limit, 1), 100)); }
    public Optional<GraphNode> getSymbolDetails(String id) { return graph.findNode(id); }
    public List<GraphNode> findDependencies(String id, int limit) { return graph.relationshipsFrom(id).stream().limit(limit).map(r -> graph.findNode(r.targetId()).orElse(null)).filter(Objects::nonNull).toList(); }
    public List<GraphNode> findDependents(String id, int limit) { return graph.relationshipsTo(id).stream().limit(limit).map(r -> graph.findNode(r.sourceId()).orElse(null)).filter(Objects::nonNull).toList(); }
    public ImpactResult analyzeImpact(String id, int maxDepth, int maxResults) { return new GraphImpactAnalyzer(graph).analyze(id, Math.min(maxDepth, 20), Math.min(maxResults, 200)); }
    public Map<String, Object> summary() { return Map.of("nodes", graph.nodes().size(), "relationships", graph.relationships().size(), "indexed", status != null); }
    public List<GraphNode> getApiEndpoints(int limit) { return graph.nodes().stream().filter(n -> n.kind() == NodeKind.API_ENDPOINT).limit(limit).toList(); }
    public GitDiffReport analyzeGitDiff(String path, boolean staged, String base, String target) throws Exception {
        if (base != null && target != null) return diffAnalyzer.commits(Path.of(path), base, target);
        return diffAnalyzer.workingTree(Path.of(path), staged);
    }
}
