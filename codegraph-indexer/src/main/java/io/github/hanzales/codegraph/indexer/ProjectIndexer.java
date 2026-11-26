package io.github.hanzales.codegraph.indexer;

import io.github.hanzales.codegraph.core.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

public final class ProjectIndexer {
    private final LanguageAnalyzer analyzer; private final GraphRepository graph;
    private final IncrementalIndexPlanner incrementalPlanner = new IncrementalIndexPlanner();
    public ProjectIndexer(LanguageAnalyzer analyzer, GraphRepository graph) { this.analyzer = analyzer; this.graph = graph; }
    public IndexStatus index(Path root) throws IOException {
        return index(root, List.of(), List.of());
    }
    public IndexStatus index(Path root, List<String> includePatterns, List<String> excludePatterns) throws IOException {
        var started = System.nanoTime(); var canonical = root.toRealPath(); var files = discover(canonical, includePatterns, excludePatterns); graph.clear();
        var result = analyzer.analyze(canonical, files);
        if (graph instanceof io.github.hanzales.codegraph.graph.PersistentGraphStore persistent) { persistent.initializeSchema(); persistent.upsertNodes(result.nodes()); persistent.addRelationships(result.relationships()); }
        else { result.nodes().forEach(graph::upsertNode); result.relationships().forEach(graph::addRelationship); }
        var repository = new GraphNode("repository:" + sha256(canonical.toString()), NodeKind.REPOSITORY, canonical.getFileName().toString(), canonical.toString(), "", 0, 0, Map.of("language", analyzer.language())); graph.upsertNode(repository);
        result.nodes().stream().filter(n -> n.kind() == NodeKind.CLASS || n.kind() == NodeKind.INTERFACE).forEach(n -> graph.addRelationship(new GraphRelationship(repository.id(), n.id(), "CONTAINS", n.file(), n.startLine(), n.endLine(), Confidence.HIGH, "indexed source file")));
        return new IndexStatus(repository.id(), canonical.toString(), "working-tree", files.size(), result.nodes().size(), result.relationships().size(), (System.nanoTime() - started) / 1_000_000, Instant.now(), result.warnings());
    }
    /** Performs a safe incremental run, falling back to a full rebuild when contracts are incompatible. */
    public IncrementalIndexResult indexIncremental(Path root, List<String> includePatterns, List<String> excludePatterns,
                                                    IndexManifest previousManifest, GraphRepository previousGraph,
                                                    IndexingMode mode, String analyzerVersion, String schemaVersion,
                                                    String configurationFingerprint, IndexingCancellation cancellation) throws IOException {
        var started = System.nanoTime(); var canonical = root.toRealPath(); var files = discover(canonical, includePatterns, excludePatterns);
        var currentManifest = incrementalPlanner.manifest(canonical, files, analyzerVersion, schemaVersion, configurationFingerprint);
        var plan = incrementalPlanner.plan(mode, previousManifest, currentManifest, previousGraph);
        if (cancellation == null) cancellation = IndexingCancellation.none();
        if (cancellation.isCancellationRequested()) throw new java.util.concurrent.CancellationException("Indexing cancelled");
        if (plan.fullFallback()) {
            var full = index(canonical, includePatterns, excludePatterns);
            return new IncrementalIndexResult(full, currentManifest, plan, new IncrementalIndexStatistics(IndexingMode.FULL, files.size(), 0, files.size(), plan.deletedFiles().size(), plan.renamedFiles().size(), plan.invalidatedFiles().size(), true, (System.nanoTime() - started) / 1_000_000, plan.fallbackReason()));
        }
        graph.clear(); var invalidated = plan.invalidatedFiles(); var reused = 0;
        if (previousGraph != null) {
            for (var node : previousGraph.nodes()) if (node.file().isBlank() || !invalidated.contains(node.file())) { graph.upsertNode(node); reused++; }
            var nodeIds = graph.nodes().stream().map(GraphNode::id).collect(java.util.stream.Collectors.toSet());
            for (var edge : previousGraph.relationships()) if (!invalidated.contains(edge.file()) && nodeIds.contains(edge.sourceId()) && nodeIds.contains(edge.targetId())) graph.addRelationship(edge);
        }
        var changedFiles = files.stream().filter(file -> invalidated.contains(canonical.relativize(file).toString().replace(FileSystems.getDefault().getSeparator(), "/"))).toList();
        if (cancellation.isCancellationRequested()) throw new java.util.concurrent.CancellationException("Indexing cancelled");
        var result = analyzer.analyze(canonical, changedFiles);
        if (cancellation.isCancellationRequested()) throw new java.util.concurrent.CancellationException("Indexing cancelled");
        result.nodes().forEach(graph::upsertNode); result.relationships().forEach(graph::addRelationship);
        var repository = new GraphNode("repository:" + sha256(canonical.toString()), NodeKind.REPOSITORY, canonical.getFileName().toString(), canonical.toString(), "", 0, 0, Map.of("language", analyzer.language())); graph.upsertNode(repository);
        var existing = graph.relationships().stream().map(this::relationshipKey).collect(java.util.stream.Collectors.toSet());
        for (var node : graph.nodes()) if ((node.kind() == NodeKind.CLASS || node.kind() == NodeKind.INTERFACE) && !node.file().isBlank()) { var edge = new GraphRelationship(repository.id(), node.id(), "CONTAINS", node.file(), node.startLine(), node.endLine(), Confidence.HIGH, "indexed source file"); if (existing.add(relationshipKey(edge))) graph.addRelationship(edge); }
        var status = new IndexStatus(repository.id(), canonical.toString(), "working-tree", files.size(), graph.nodes().size(), graph.relationships().size(), (System.nanoTime() - started) / 1_000_000, Instant.now(), result.warnings());
        return new IncrementalIndexResult(status, currentManifest, plan, new IncrementalIndexStatistics(IndexingMode.INCREMENTAL, files.size(), reused, changedFiles.size(), plan.deletedFiles().size(), plan.renamedFiles().size(), invalidated.size(), false, (System.nanoTime() - started) / 1_000_000, "Incremental graph rebuilt from reusable facts"));
    }
    public IncrementalIndexResult indexIncremental(Path root, List<String> includePatterns, List<String> excludePatterns,
                                                    IndexManifest previousManifest, GraphRepository previousGraph,
                                                    IndexingMode mode, String analyzerVersion, String schemaVersion,
                                                    String configurationFingerprint, java.time.Duration timeout) throws IOException {
        var deadline = System.nanoTime() + Math.max(0, timeout.toNanos());
        return indexIncremental(root, includePatterns, excludePatterns, previousManifest, previousGraph, mode,
                analyzerVersion, schemaVersion, configurationFingerprint, () -> System.nanoTime() > deadline);
    }
    public List<Path> discover(Path root) throws IOException {
        return discover(root, List.of(), List.of());
    }
    public List<Path> discover(Path root, List<String> includePatterns, List<String> excludePatterns) throws IOException {
        var includes = includePatterns == null ? List.<PathMatcher>of() : includePatterns.stream().map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern)).toList();
        var excludes = excludePatterns == null ? List.<PathMatcher>of() : excludePatterns.stream().map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern)).toList();
        try (Stream<Path> stream = Files.walk(root)) { return stream.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).filter(p -> !excluded(root.relativize(p))).filter(p -> includes.isEmpty() || includes.stream().anyMatch(matcher -> matcher.matches(root.relativize(p)))).filter(p -> excludes.stream().noneMatch(matcher -> matcher.matches(root.relativize(p)))).toList(); }
    }
    private static boolean excluded(Path relative) { for (var part : relative) if (Set.of(".git", "target", "build", "out", "node_modules").contains(part.toString())) return true; return false; }
    private static String sha256(String text) { try { var bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes()); var result = new StringBuilder(); for (byte b : bytes) result.append(String.format("%02x", b)); return result.toString(); } catch (Exception e) { throw new IllegalStateException(e); } }
    private String relationshipKey(GraphRelationship edge) { return edge.sourceId() + "|" + edge.targetId() + "|" + edge.type() + "|" + edge.file(); }
}
