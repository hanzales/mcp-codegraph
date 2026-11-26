package io.github.hanzales.codegraph.indexer;

import io.github.hanzales.codegraph.core.*;
import io.github.hanzales.codegraph.graph.InMemoryGraphRepository;
import io.github.hanzales.codegraph.analyzer.JavaLanguageAnalyzer;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IncrementalIndexTest {
    @Test void reusesUnchangedFactsAndDetectsModifiedFiles() throws Exception {
        var root = Files.createTempDirectory("codegraph-incremental");
        Files.writeString(root.resolve("A.java"), "class A { int value() { return 1; } }\n");
        Files.writeString(root.resolve("B.java"), "class B { }\n");
        var analyzer = new JavaLanguageAnalyzer(); var previous = new InMemoryGraphRepository(); var indexer = new ProjectIndexer(analyzer, previous);
        indexer.index(root); var planner = new IncrementalIndexPlanner(); var manifest = planner.manifest(root, indexer.discover(root), "1", "1", "config");
        Files.writeString(root.resolve("A.java"), "class A { int value() { return 2; } }\n");
        var current = new InMemoryGraphRepository(); var result = new ProjectIndexer(analyzer, current).indexIncremental(root, List.of(), List.of(), manifest, previous, IndexingMode.AUTO, "1", "1", "config", IndexingCancellation.none());
        assertFalse(result.plan().fullFallback()); assertEquals(1, result.statistics().filesAnalyzed()); assertTrue(result.statistics().filesReused() > 0); assertTrue(result.plan().modifiedFiles().contains("A.java"));
    }

    @Test void incompatibleAnalyzerFallsBackToFull() throws Exception {
        var root = Files.createTempDirectory("codegraph-incremental-fallback"); Files.writeString(root.resolve("A.java"), "class A {}\n");
        var analyzer = new JavaLanguageAnalyzer(); var graph = new InMemoryGraphRepository(); var indexer = new ProjectIndexer(analyzer, graph); indexer.index(root);
        var manifest = new IncrementalIndexPlanner().manifest(root, indexer.discover(root), "old", "1", "config");
        var result = indexer.indexIncremental(root, List.of(), List.of(), manifest, graph, IndexingMode.AUTO, "new", "1", "config", IndexingCancellation.none());
        assertTrue(result.plan().fullFallback()); assertEquals(IndexingMode.FULL, result.statistics().mode());
    }
}
