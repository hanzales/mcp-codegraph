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
}
