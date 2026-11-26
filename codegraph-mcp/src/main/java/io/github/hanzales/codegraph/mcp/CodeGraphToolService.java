package io.github.hanzales.codegraph.mcp;

import io.github.hanzales.codegraph.core.*;
import io.github.hanzales.codegraph.graph.GraphImpactAnalyzer;
import io.github.hanzales.codegraph.indexer.ProjectIndexer;
import io.github.hanzales.codegraph.indexer.WorkspaceIndexService;
import java.nio.file.Path;
import java.util.*;

/** Application service used by MCP adapters. It never executes repository code. */
public final class CodeGraphToolService {
    private final GraphRepository graph; private final ProjectIndexer indexer; private IndexStatus status;
    private final io.github.hanzales.codegraph.indexer.GitDiffAnalyzer diffAnalyzer = new io.github.hanzales.codegraph.indexer.GitDiffAnalyzer();
    private final WorkspaceIndexService workspaceIndexService;
    public CodeGraphToolService(GraphRepository graph, ProjectIndexer indexer) { this(graph, indexer, null); }
    public CodeGraphToolService(GraphRepository graph, ProjectIndexer indexer, WorkspaceIndexService workspaceIndexService) { this.graph = graph; this.indexer = indexer; this.workspaceIndexService = workspaceIndexService; }
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
    public WorkspaceDefinition registerWorkspace(String workspaceId, String displayName) { return requireWorkspaceService().registerWorkspace(new WorkspaceDefinition(workspaceId, displayName, Map.of())); }
    public boolean removeWorkspace(String workspaceId) { return requireWorkspaceService().removeWorkspace(workspaceId); }
    public RepositoryDefinition registerRepository(String workspaceId, RepositoryDefinition definition) throws Exception { return requireWorkspaceService().registerRepository(workspaceId, definition); }
    public boolean removeRepository(String workspaceId, String repositoryId) { return requireWorkspaceService().removeRepository(workspaceId, repositoryId); }
    public RepositoryIndexStatus indexRepository(String workspaceId, String repositoryId) { return requireWorkspaceService().indexRepository(workspaceId, repositoryId); }
    public List<RepositoryIndexStatus> indexWorkspace(String workspaceId) { return requireWorkspaceService().indexWorkspace(workspaceId); }
    public WorkspaceIndexStatus getWorkspaceIndexStatus(String workspaceId) { return requireWorkspaceService().status(workspaceId); }
    public List<RepositoryDefinition> getWorkspaceRepositories(String workspaceId) { return requireWorkspaceService().repositories(workspaceId); }
    public List<GraphNode> searchWorkspaceSymbols(String workspaceId, String query, int limit) { return requireWorkspaceService().searchWorkspace(workspaceId, query, Math.min(Math.max(limit, 1), 100)); }
    public List<GraphNode> searchRepositorySymbols(String workspaceId, String repositoryId, String query, int limit) { return requireWorkspaceService().searchRepository(workspaceId, repositoryId, query, Math.min(Math.max(limit, 1), 100)); }
    public List<RepositorySnapshotMetadata> getRepositorySnapshots(String workspaceId, String repositoryId) { return requireWorkspaceService().snapshots(workspaceId, repositoryId); }
    public Optional<RepositorySnapshotMetadata> getActiveRepositorySnapshot(String workspaceId, String repositoryId) { return requireWorkspaceService().activeSnapshot(workspaceId, repositoryId); }
    public RepositorySnapshotMetadata selectRepositorySnapshot(String workspaceId, String repositoryId, String snapshotId) { return requireWorkspaceService().selectActiveSnapshot(workspaceId, repositoryId, snapshotId); }
    public SnapshotComparison compareRepositorySnapshots(String workspaceId, String repositoryId, String leftSnapshotId, String rightSnapshotId) { return requireWorkspaceService().compareSnapshots(workspaceId, repositoryId, leftSnapshotId, rightSnapshotId); }
    private WorkspaceIndexService requireWorkspaceService() { if (workspaceIndexService == null) throw new IllegalStateException("Workspace support is not configured"); return workspaceIndexService; }
}
