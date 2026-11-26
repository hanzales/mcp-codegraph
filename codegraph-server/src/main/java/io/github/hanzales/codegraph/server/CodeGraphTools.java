package io.github.hanzales.codegraph.server;

import io.github.hanzales.codegraph.core.*;
import io.github.hanzales.codegraph.mcp.CodeGraphToolService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public final class CodeGraphTools {
    private final CodeGraphToolService service;
    public CodeGraphTools(CodeGraphToolService service) { this.service = service; }
    @Tool(description = "Index a local Java repository using read-only AST analysis. The path must be allowlisted by deployment policy.")
    public IndexStatus scanProject(String repositoryPath) throws Exception { return service.scanProject(repositoryPath); }
    @Tool(description = "Return the current index status, symbol count, relationship count, warnings, and timing.")
    public IndexStatus getIndexStatus() { return service.getIndexStatus(); }
    @Tool(description = "Search indexed Java classes, interfaces, methods, fields, endpoints, and validations by name.")
    public List<GraphNode> searchSymbols(String query, int limit) { return service.searchSymbols(query, limit); }
    @Tool(description = "Get a symbol's stable identifier, source location, kind, and attributes.")
    public Optional<GraphNode> getSymbolDetails(String symbolId) { return service.getSymbolDetails(symbolId); }
    @Tool(description = "Find direct outgoing dependencies from a symbol.")
    public List<GraphNode> findDependencies(String symbolId, int limit) { return service.findDependencies(symbolId, limit); }
    @Tool(description = "Find direct incoming dependents of a symbol.")
    public List<GraphNode> findDependents(String symbolId, int limit) { return service.findDependents(symbolId, limit); }
    @Tool(description = "Analyze deterministic change impact through incoming graph relationships. Results include limitations and evidence-backed paths.")
    public ImpactResult analyzeImpact(String symbolId, int maxDepth, int maxResults) { return service.analyzeImpact(symbolId, maxDepth, maxResults); }
    @Tool(description = "Return a compact graph summary for the indexed repository.")
    public Map<String, Object> getProjectSummary() { return service.summary(); }
    @Tool(description = "List statically detected Spring HTTP API endpoints.")
    public List<GraphNode> getApiEndpoints(int limit) { return service.getApiEndpoints(limit); }
    @Tool(description = "Analyze working tree, staged, or two Git revisions without modifying the repository. Reports changed Java methods/classes and source ranges.")
    public GitDiffReport analyzeGitDiff(String repositoryPath, boolean staged, String baseRevision, String targetRevision) throws Exception {
        return service.analyzeGitDiff(repositoryPath, staged, baseRevision, targetRevision);
    }
    @Tool(description = "Register an isolated workspace that can contain multiple local repositories.")
    public WorkspaceDefinition registerWorkspace(String workspaceId, String displayName) { return service.registerWorkspace(workspaceId, displayName); }
    @Tool(description = "Register a local repository in a workspace. The path must be inside configured allowed roots; no repository is cloned or modified.")
    public RepositoryDefinition registerRepository(String workspaceId, RepositoryDefinition repository) throws Exception { return service.registerRepository(workspaceId, repository); }
    @Tool(description = "Remove a repository from a workspace without affecting other workspaces or repositories.")
    public boolean removeRepository(String workspaceId, String repositoryId) { return service.removeRepository(workspaceId, repositoryId); }
    @Tool(description = "Index one registered repository using its selected language analyzer.")
    public RepositoryIndexStatus indexRepository(String workspaceId, String repositoryId) { return service.indexRepository(workspaceId, repositoryId); }
    @Tool(description = "Index all registered repositories in a workspace and return independent statuses, including partial failures.")
    public List<RepositoryIndexStatus> indexWorkspace(String workspaceId) { return service.indexWorkspace(workspaceId); }
    @Tool(description = "Return workspace and per-repository indexing status.")
    public WorkspaceIndexStatus getWorkspaceIndexStatus(String workspaceId) { return service.getWorkspaceIndexStatus(workspaceId); }
    @Tool(description = "List repositories registered in a workspace.")
    public List<RepositoryDefinition> getWorkspaceRepositories(String workspaceId) { return service.getWorkspaceRepositories(workspaceId); }
    @Tool(description = "Search symbols across isolated repositories in a workspace. Returned IDs are workspace-namespaced.")
    public List<GraphNode> searchWorkspaceSymbols(String workspaceId, String query, int limit) { return service.searchWorkspaceSymbols(workspaceId, query, limit); }
    @Tool(description = "Search symbols within one repository only, without leaking results from other repositories or workspaces.")
    public List<GraphNode> searchRepositorySymbols(String workspaceId, String repositoryId, String query, int limit) { return service.searchRepositorySymbols(workspaceId, repositoryId, query, limit); }
}
