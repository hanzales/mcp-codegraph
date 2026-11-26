package io.github.hanzales.codegraph.indexer;

import io.github.hanzales.codegraph.core.*;
import io.github.hanzales.codegraph.graph.InMemoryGraphRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/** Registry and lifecycle boundary for isolated repositories grouped into workspaces. */
public final class WorkspaceIndexService {
    private final RepositoryPathPolicy pathPolicy;
    private final LanguageAnalyzerSelector selector;
    private final BiFunction<String, RepositoryDefinition, GraphRepository> graphFactory;
    private final Map<String, WorkspaceState> workspaces = new ConcurrentHashMap<>();
    public WorkspaceIndexService(RepositoryPathPolicy pathPolicy, LanguageAnalyzerSelector selector) { this(pathPolicy, selector, (ignoredWorkspace, ignoredRepository) -> new InMemoryGraphRepository()); }
    public WorkspaceIndexService(RepositoryPathPolicy pathPolicy, LanguageAnalyzerSelector selector, BiFunction<String, RepositoryDefinition, GraphRepository> graphFactory) { this.pathPolicy = Objects.requireNonNull(pathPolicy); this.selector = Objects.requireNonNull(selector); this.graphFactory = Objects.requireNonNull(graphFactory); }
    public WorkspaceDefinition registerWorkspace(WorkspaceDefinition definition) { workspaces.putIfAbsent(definition.workspaceId(), new WorkspaceState(definition)); return definition; }
    public boolean removeWorkspace(String workspaceId) { return workspaces.remove(workspaceId) != null; }
    public RepositoryDefinition registerRepository(String workspaceId, RepositoryDefinition definition) throws IOException {
        var workspace = requireWorkspace(workspaceId); var canonical = pathPolicy.validate(definition.localPath());
        var normalized = new RepositoryDefinition(definition.repositoryId(), definition.displayName(), canonical.toString(), definition.language(), definition.branch(), definition.serviceIdentity(), definition.includePatterns(), definition.excludePatterns(), definition.analyzerConfiguration());
        workspace.repositories.putIfAbsent(normalized.repositoryId(), new RepositoryState(workspaceId, normalized, canonical, graphFactory.apply(workspaceId, normalized))); return normalized;
    }
    public boolean removeRepository(String workspaceId, String repositoryId) { return requireWorkspace(workspaceId).repositories.remove(repositoryId) != null; }
    public RepositoryIndexStatus indexRepository(String workspaceId, String repositoryId) {
        var workspace = requireWorkspace(workspaceId); var repository = workspace.repositories.get(repositoryId); if (repository == null) throw new IllegalArgumentException("Repository is not registered in workspace");
        repository.status = new RepositoryIndexStatus(workspaceId, repository.definition.repositoryId(), repository.definition.displayName(), repository.root.toString(), repository.definition.language(), RepositoryIndexState.INDEXING, 0, 0, 0, 0, null, List.of(), "");
        try {
            var language = selector instanceof DefaultLanguageAnalyzerSelector defaultSelector ? defaultSelector.detect(repository.definition.language(), repository.root) : repository.definition.language();
            var analyzer = selector.select(repository.definition.language(), repository.root);
            if (analyzer.isEmpty()) { repository.status = failed(workspaceId, repository, language, "No analyzer is registered for language: " + language, RepositoryIndexState.PARTIAL); return repository.status; }
            var result = new ProjectIndexer(analyzer.get(), repository.graph).index(repository.root, repository.definition.includePatterns(), repository.definition.excludePatterns());
            var state = result.warnings().isEmpty() ? RepositoryIndexState.INDEXED : RepositoryIndexState.PARTIAL;
            repository.status = new RepositoryIndexStatus(workspaceId, repository.definition.repositoryId(), repository.definition.displayName(), repository.root.toString(), language, state, result.fileCount(), result.symbolCount(), result.relationshipCount(), result.durationMs(), result.indexedAt(), result.warnings(), "");
        } catch (Exception exception) { repository.status = failed(workspaceId, repository, repository.definition.language(), exception.getMessage(), RepositoryIndexState.FAILED); }
        return repository.status;
    }
    public List<RepositoryIndexStatus> indexWorkspace(String workspaceId) { var workspace = requireWorkspace(workspaceId); return workspace.repositories.keySet().stream().sorted().map(id -> indexRepository(workspaceId, id)).toList(); }
    public WorkspaceIndexStatus status(String workspaceId) { var workspace = requireWorkspace(workspaceId); var statuses = workspace.repositories.values().stream().map(state -> state.status).toList(); return new WorkspaceIndexStatus(workspace.definition.workspaceId(), workspace.definition.displayName(), statuses.size(), (int) statuses.stream().filter(s -> s.state() == RepositoryIndexState.INDEXED).count(), (int) statuses.stream().filter(s -> s.state() == RepositoryIndexState.PARTIAL).count(), (int) statuses.stream().filter(s -> s.state() == RepositoryIndexState.FAILED).count(), Instant.now(), statuses); }
    public List<RepositoryDefinition> repositories(String workspaceId) { return requireWorkspace(workspaceId).repositories.values().stream().map(state -> state.definition).sorted(Comparator.comparing(RepositoryDefinition::repositoryId)).toList(); }
    public List<GraphNode> searchWorkspace(String workspaceId, String query, int limit) { var output = new ArrayList<GraphNode>(); var workspace = requireWorkspace(workspaceId); for (var repository : workspace.repositories.values()) { if (output.size() >= limit) break; for (var node : repository.graph.searchSymbols(query, limit - output.size())) output.add(scope(workspaceId, repository.definition.repositoryId(), node)); } return List.copyOf(output); }
    public List<GraphNode> searchRepository(String workspaceId, String repositoryId, String query, int limit) { var repository = requireRepository(workspaceId, repositoryId); return repository.graph.searchSymbols(query, limit); }
    private GraphNode scope(String workspaceId, String repositoryId, GraphNode node) { var attrs = new HashMap<>(node.attributes()); attrs.put("workspaceId", workspaceId); attrs.put("repositoryId", repositoryId); return new GraphNode(workspaceId + "/" + repositoryId + "/" + node.id(), node.kind(), node.name(), node.qualifiedName(), node.file(), node.startLine(), node.endLine(), attrs); }
    private RepositoryIndexStatus failed(String workspaceId, RepositoryState repository, String language, String error, RepositoryIndexState state) { return new RepositoryIndexStatus(workspaceId, repository.definition.repositoryId(), repository.definition.displayName(), repository.root.toString(), language, state, 0, 0, 0, 0, Instant.now(), List.of(new AnalysisWarning("INDEX_FAILURE", error == null ? "Indexing failed" : error, new SourceLocation("", 0, 0))), error == null ? "Indexing failed" : error); }
    private WorkspaceState requireWorkspace(String id) { var state = workspaces.get(id); if (state == null) throw new IllegalArgumentException("Workspace is not registered"); return state; }
    private RepositoryState requireRepository(String workspaceId, String repositoryId) { var repository = requireWorkspace(workspaceId).repositories.get(repositoryId); if (repository == null) throw new IllegalArgumentException("Repository is not registered in workspace"); return repository; }
    private static final class WorkspaceState { final WorkspaceDefinition definition; final Map<String, RepositoryState> repositories = new ConcurrentHashMap<>(); WorkspaceState(WorkspaceDefinition definition) { this.definition = definition; } }
    private static final class RepositoryState { final RepositoryDefinition definition; final Path root; final GraphRepository graph; volatile RepositoryIndexStatus status; RepositoryState(String workspaceId, RepositoryDefinition definition, Path root, GraphRepository graph) { this.definition = definition; this.root = root; this.graph = graph; this.status = new RepositoryIndexStatus(workspaceId, definition.repositoryId(), definition.displayName(), root.toString(), definition.language(), RepositoryIndexState.REGISTERED, 0, 0, 0, 0, null, List.of(), ""); } }
}
