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
    public ProjectIndexer(LanguageAnalyzer analyzer, GraphRepository graph) { this.analyzer = analyzer; this.graph = graph; }
    public IndexStatus index(Path root) throws IOException {
        return index(root, List.of(), List.of());
    }
    public IndexStatus index(Path root, List<String> includePatterns, List<String> excludePatterns) throws IOException {
        var started = System.nanoTime(); var canonical = root.toRealPath(); var files = discover(canonical, includePatterns, excludePatterns); graph.clear();
        var result = analyzer.analyze(canonical, files); result.nodes().forEach(graph::upsertNode); result.relationships().forEach(graph::addRelationship);
        var repository = new GraphNode("repository:" + sha256(canonical.toString()), NodeKind.REPOSITORY, canonical.getFileName().toString(), canonical.toString(), "", 0, 0, Map.of("language", analyzer.language())); graph.upsertNode(repository);
        result.nodes().stream().filter(n -> n.kind() == NodeKind.CLASS || n.kind() == NodeKind.INTERFACE).forEach(n -> graph.addRelationship(new GraphRelationship(repository.id(), n.id(), "CONTAINS", n.file(), n.startLine(), n.endLine(), Confidence.HIGH, "indexed source file")));
        return new IndexStatus(repository.id(), canonical.toString(), "working-tree", files.size(), result.nodes().size(), result.relationships().size(), (System.nanoTime() - started) / 1_000_000, Instant.now(), result.warnings());
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
}
