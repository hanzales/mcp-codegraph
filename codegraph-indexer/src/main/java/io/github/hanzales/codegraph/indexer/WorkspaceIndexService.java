package io.github.hanzales.codegraph.indexer;

import io.github.hanzales.codegraph.core.*;
import io.github.hanzales.codegraph.graph.InMemoryGraphRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/** Workspace registry and immutable, provenance-aware repository snapshot lifecycle. */
public final class WorkspaceIndexService {
    @FunctionalInterface public interface SnapshotGraphFactory { GraphRepository create(String workspaceId, RepositoryDefinition repository, String snapshotId); }
    private static final String ANALYZER_VERSION = "1";
    private static final String SCHEMA_VERSION = "1";
    private final RepositoryPathPolicy pathPolicy; private final LanguageAnalyzerSelector selector;
    private final SnapshotGraphFactory graphFactory; private final int retentionLimit;
    private final GitRepositoryMetadataReader metadataReader = new GitRepositoryMetadataReader();
    private final Map<String, WorkspaceState> workspaces = new ConcurrentHashMap<>();
    public WorkspaceIndexService(RepositoryPathPolicy p, LanguageAnalyzerSelector s) { this(p, s, (w, r, x) -> new InMemoryGraphRepository(), 3); }
    public WorkspaceIndexService(RepositoryPathPolicy p, LanguageAnalyzerSelector s, BiFunction<String, RepositoryDefinition, GraphRepository> f) { this(p, s, (w, r, x) -> f.apply(w, r), 3); }
    public WorkspaceIndexService(RepositoryPathPolicy p, LanguageAnalyzerSelector s, SnapshotGraphFactory f, int retention) { pathPolicy = Objects.requireNonNull(p); selector = Objects.requireNonNull(s); graphFactory = Objects.requireNonNull(f); retentionLimit = Math.max(1, retention); }
    public WorkspaceDefinition registerWorkspace(WorkspaceDefinition d) { workspaces.putIfAbsent(d.workspaceId(), new WorkspaceState(d)); return d; }
    public boolean removeWorkspace(String id) { return workspaces.remove(id) != null; }
    public RepositoryDefinition registerRepository(String w, RepositoryDefinition d) throws IOException { var ws = requireWorkspace(w); var root = pathPolicy.validate(d.localPath()); var n = new RepositoryDefinition(d.repositoryId(), d.displayName(), root.toString(), d.language(), d.branch(), d.serviceIdentity(), d.includePatterns(), d.excludePatterns(), d.analyzerConfiguration()); ws.repositories.putIfAbsent(n.repositoryId(), new RepositoryState(w, n, root)); return n; }
    public boolean removeRepository(String w, String r) { return requireWorkspace(w).repositories.remove(r) != null; }

    public synchronized RepositoryIndexStatus indexRepository(String w, String rid) {
        var r = requireRepository(w, rid); var started = Instant.now(); var run = GraphIdentifiers.indexingRun(w, rid, started.toEpochMilli());
        r.status = status(w, r, RepositoryIndexState.INDEXING, 0, 0, 0, 0, null, List.of(), "");
        GitRepositoryMetadata source; try { source = metadataReader.inspect(r.root); } catch (Exception e) { return fail(w, r, e); }
        String language; java.util.Optional<LanguageAnalyzer> analyzer;
        try { language = selector instanceof DefaultLanguageAnalyzerSelector d ? d.detect(r.definition.language(), r.root) : r.definition.language(); analyzer = selector.select(r.definition.language(), r.root); }
        catch (Exception e) { return fail(w, r, e); }
        var descriptor = source.branch() + "\u0000" + source.commitHash() + "\u0000" + source.workingTreeFingerprint() + "\u0000" + ANALYZER_VERSION + "\u0000" + SCHEMA_VERSION; var sid = GraphIdentifiers.snapshot(r.definition.repositoryId(), descriptor); GraphRepository graph; try { graph = graphFactory.create(w, r.definition, sid); } catch (Exception e) { return fail(w, r, e); }
        if (analyzer.isEmpty()) { var m = metadata(r, sid, run, source, SnapshotStatus.PARTIAL, started, 0, 1, 0); r.snapshots.put(sid, new SnapshotState(m, graph)); r.status = status(w, r, RepositoryIndexState.PARTIAL, 0, 0, 0, 0, null, List.of(), "No analyzer is registered for language: " + language); return r.status; }
        try {
            var result = new ProjectIndexer(analyzer.get(), graph).index(r.root, r.definition.includePatterns(), r.definition.excludePatterns()); var partial = !result.warnings().isEmpty(); var m = metadata(r, sid, run, source, partial ? SnapshotStatus.PARTIAL : SnapshotStatus.ACTIVE, result.indexedAt(), result.fileCount(), result.warnings().size(), 0); r.snapshots.put(sid, new SnapshotState(m, graph));
            if (!partial) { r.activeSnapshotId = sid; activateOnly(r, sid); enforceRetention(r); }
            r.status = status(w, r, partial ? RepositoryIndexState.PARTIAL : RepositoryIndexState.INDEXED, result.fileCount(), result.symbolCount(), result.relationshipCount(), result.durationMs(), result.indexedAt(), result.warnings(), "");
        } catch (Exception e) { return fail(w, r, e); }
        return r.status;
    }
    public List<RepositoryIndexStatus> indexWorkspace(String id) { var ws = requireWorkspace(id); return ws.repositories.keySet().stream().sorted().map(x -> indexRepository(id, x)).toList(); }
    public WorkspaceIndexStatus status(String id) { var ws = requireWorkspace(id); var ss = ws.repositories.values().stream().map(r -> currentStatus(ws.definition.workspaceId(), r)).toList(); return new WorkspaceIndexStatus(id, ws.definition.displayName(), ss.size(), (int) ss.stream().filter(x -> x.state() == RepositoryIndexState.INDEXED).count(), (int) ss.stream().filter(x -> x.state() == RepositoryIndexState.PARTIAL).count(), (int) ss.stream().filter(x -> x.state() == RepositoryIndexState.FAILED).count(), Instant.now(), ss); }
    public List<RepositoryDefinition> repositories(String id) { return requireWorkspace(id).repositories.values().stream().map(x -> x.definition).sorted(Comparator.comparing(RepositoryDefinition::repositoryId)).toList(); }
    public List<GraphNode> searchWorkspace(String id, String q, int limit) { var out = new ArrayList<GraphNode>(); for (var r : requireWorkspace(id).repositories.values()) { if (out.size() >= limit) break; var g = activeGraph(r); if (g.isPresent()) for (var n : g.get().searchSymbols(q, limit - out.size())) out.add(scope(id, r.definition.repositoryId(), n)); } return List.copyOf(out); }
    public List<GraphNode> searchRepository(String w, String r, String q, int limit) { return activeGraph(requireRepository(w, r)).map(g -> g.searchSymbols(q, limit)).orElse(List.of()); }
    public List<RepositorySnapshotMetadata> snapshots(String w, String r) { var state = requireRepository(w, r); return state.snapshots.values().stream().map(x -> refresh(x.metadata, state)).sorted(Comparator.comparing(RepositorySnapshotMetadata::indexedAt, Comparator.nullsLast(Comparator.reverseOrder()))).toList(); }
    public Optional<RepositorySnapshotMetadata> activeSnapshot(String w, String r) { var s = requireRepository(w, r); var x = s.snapshots.get(s.activeSnapshotId); return x == null ? Optional.empty() : Optional.of(refresh(x.metadata, s)); }
    public RepositorySnapshotMetadata selectActiveSnapshot(String w, String r, String id) { var s = requireRepository(w, r); var x = s.snapshots.get(id); if (x == null || (x.metadata.status() != SnapshotStatus.ACTIVE && x.metadata.status() != SnapshotStatus.INDEXED)) throw new IllegalArgumentException("Snapshot is not a successful snapshot"); s.activeSnapshotId = id; activateOnly(s, id); return x.metadata; }
    public SnapshotComparison compareSnapshots(String w, String r, String left, String right) { var s = requireRepository(w, r); var a = s.snapshots.get(left); var b = s.snapshots.get(right); if (a == null || b == null) throw new IllegalArgumentException("Snapshot is not registered"); var an = ids(a.graph.nodes()); var bn = ids(b.graph.nodes()); var ar = rels(a.graph.relationships()); var br = rels(b.graph.relationships()); return new SnapshotComparison(w, r, left, right, diff(bn, an), diff(an, bn), diff(br, ar), diff(ar, br), a.metadata.sourceFingerprint().equals(b.metadata.sourceFingerprint()), List.of("Static graph comparison does not prove runtime behavior")); }

    private RepositoryIndexStatus fail(String w, RepositoryState r, Exception e) { var msg = e.getMessage() == null ? "Indexing failed" : e.getMessage(); r.status = status(w, r, RepositoryIndexState.FAILED, 0, 0, 0, 0, Instant.now(), List.of(new AnalysisWarning("INDEX_FAILURE", msg, new SourceLocation("", 0, 0))), msg); return r.status; }
    private RepositoryIndexStatus currentStatus(String w, RepositoryState r) { var x = r.status; return new RepositoryIndexStatus(x.workspaceId(), x.repositoryId(), x.displayName(), x.canonicalPath(), x.detectedLanguage(), x.state(), x.fileCount(), x.symbolCount(), x.relationshipCount(), x.durationMs(), x.indexedAt(), x.warnings(), x.error(), r.activeSnapshotId, stale(r)); }
    private RepositoryIndexStatus status(String w, RepositoryState r, RepositoryIndexState st, int f, int n, int rel, long d, Instant at, List<AnalysisWarning> warnings, String error) { return new RepositoryIndexStatus(w, r.definition.repositoryId(), r.definition.displayName(), r.root.toString(), r.definition.language(), st, f, n, rel, d, at, warnings, error, r.activeSnapshotId, stale(r)); }
    private Optional<GraphRepository> activeGraph(RepositoryState r) { var s = r.snapshots.get(r.activeSnapshotId); return s == null ? Optional.empty() : Optional.of(s.graph); }
    private boolean stale(RepositoryState r) { var s = r.snapshots.get(r.activeSnapshotId); if (s == null) return false; try { return !s.metadata.sourceFingerprint().equals(metadataReader.inspect(r.root).sourceFingerprint()); } catch (Exception e) { return true; } }
    private RepositorySnapshotMetadata refresh(RepositorySnapshotMetadata m, RepositoryState r) { return new RepositorySnapshotMetadata(m.workspaceId(), m.repositoryId(), m.snapshotId(), m.indexingRunId(), m.branch(), m.commitHash(), m.dirty(), m.workingTreeFingerprint(), m.sourceFingerprint(), m.indexedAt(), m.analyzerVersion(), m.graphSchemaVersion(), m.status(), m.fileCount(), m.warningCount(), m.errorCount(), m.snapshotId().equals(r.activeSnapshotId) && stale(r), m.exactCommittedSnapshot()); }
    private RepositorySnapshotMetadata metadata(RepositoryState r, String sid, String run, GitRepositoryMetadata source, SnapshotStatus st, Instant at, int files, int warnings, int errors) { return new RepositorySnapshotMetadata(r.workspaceId, r.definition.repositoryId(), sid, run, source.branch(), source.commitHash(), source.dirty(), source.workingTreeFingerprint(), source.sourceFingerprint(), at, "1", "1", st, files, warnings, errors, false, source.exactCommittedSnapshot()); }
    private void activateOnly(RepositoryState r, String id) { r.snapshots.replaceAll((k, v) -> k.equals(id) ? new SnapshotState(new RepositorySnapshotMetadata(v.metadata.workspaceId(), v.metadata.repositoryId(), v.metadata.snapshotId(), v.metadata.indexingRunId(), v.metadata.branch(), v.metadata.commitHash(), v.metadata.dirty(), v.metadata.workingTreeFingerprint(), v.metadata.sourceFingerprint(), v.metadata.indexedAt(), v.metadata.analyzerVersion(), v.metadata.graphSchemaVersion(), SnapshotStatus.ACTIVE, v.metadata.fileCount(), v.metadata.warningCount(), v.metadata.errorCount(), false, v.metadata.exactCommittedSnapshot()), v.graph) : v); }
    private void enforceRetention(RepositoryState r) { var old = r.snapshots.keySet().stream().filter(x -> !x.equals(r.activeSnapshotId)).sorted().toList(); for (int i = 0; i < Math.max(0, old.size() - retentionLimit); i++) r.snapshots.remove(old.get(i)); }
    private Set<String> ids(List<GraphNode> ns) { var s = new TreeSet<String>(); ns.forEach(n -> s.add(n.id())); return s; }
    private Set<String> rels(List<GraphRelationship> rs) { var s = new TreeSet<String>(); rs.forEach(x -> s.add(x.sourceId() + "|" + x.targetId() + "|" + x.type())); return s; }
    private List<String> diff(Set<String> a, Set<String> b) { var x = new ArrayList<>(a); x.removeAll(b); return x; }
    private GraphNode scope(String w, String r, GraphNode n) { var a = new HashMap<>(n.attributes()); a.put("workspaceId", w); a.put("repositoryId", r); return new GraphNode(w + "/" + r + "/" + n.id(), n.kind(), n.name(), n.qualifiedName(), n.file(), n.startLine(), n.endLine(), a); }
    private WorkspaceState requireWorkspace(String id) { var x = workspaces.get(id); if (x == null) throw new IllegalArgumentException("Workspace is not registered"); return x; }
    private RepositoryState requireRepository(String w, String r) { var x = requireWorkspace(w).repositories.get(r); if (x == null) throw new IllegalArgumentException("Repository is not registered in workspace"); return x; }
    private static final class WorkspaceState { final WorkspaceDefinition definition; final Map<String, RepositoryState> repositories = new ConcurrentHashMap<>(); WorkspaceState(WorkspaceDefinition d) { definition = d; } }
    private static final class RepositoryState { final String workspaceId; final RepositoryDefinition definition; final Path root; final Map<String, SnapshotState> snapshots = new ConcurrentHashMap<>(); volatile String activeSnapshotId = ""; volatile RepositoryIndexStatus status; RepositoryState(String w, RepositoryDefinition d, Path r) { workspaceId = w; definition = d; root = r; status = new RepositoryIndexStatus(w, d.repositoryId(), d.displayName(), r.toString(), d.language(), RepositoryIndexState.REGISTERED, 0, 0, 0, 0, null, List.of(), ""); } }
    private record SnapshotState(RepositorySnapshotMetadata metadata, GraphRepository graph) { }
}
