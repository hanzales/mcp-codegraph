package io.github.hanzales.codegraph.indexer;

import io.github.hanzales.codegraph.core.*;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class WorkspaceSnapshotTest {
    @Test void createsDistinctSnapshotsAndComparesStates() throws Exception {
        var root = Files.createTempDirectory("codegraph-snapshots");
        Files.writeString(root.resolve("Order.java"), "class Order { String id() { return \"one\"; } }\n");
        var service = new WorkspaceIndexService(new RepositoryPathPolicy(List.of(root)), new DefaultLanguageAnalyzerSelector());
        service.registerWorkspace(new WorkspaceDefinition("workspace:test", "Test", Map.of()));
        service.registerRepository("workspace:test", new RepositoryDefinition("repo:test", "Test", root.toString(), "java", "", "", List.of(), List.of(), Map.of()));
        assertEquals(RepositoryIndexState.INDEXED, service.indexRepository("workspace:test", "repo:test").state());
        var first = service.activeSnapshot("workspace:test", "repo:test").orElseThrow();
        Files.writeString(root.resolve("Order.java"), "class Order { String id() { return \"two\"; } int total() { return 1; } }\nclass NewOrder {}\n");
        assertTrue(service.status("workspace:test").repositories().get(0).stale());
        assertEquals(RepositoryIndexState.INDEXED, service.indexRepository("workspace:test", "repo:test").state());
        var snapshots = service.snapshots("workspace:test", "repo:test");
        assertTrue(snapshots.size() >= 2);
        var second = service.activeSnapshot("workspace:test", "repo:test").orElseThrow();
        var comparison = service.compareSnapshots("workspace:test", "repo:test", first.snapshotId(), second.snapshotId());
        assertFalse(comparison.sameSourceState()); assertFalse(comparison.addedNodeIds().isEmpty());
    }

    @Test void failedIndexPreservesLastSuccessfulActiveSnapshot() throws Exception {
        var root = Files.createTempDirectory("codegraph-snapshot-failure"); Files.writeString(root.resolve("Good.java"), "class Good {}\n");
        var calls = new AtomicInteger();
        var service = new WorkspaceIndexService(new RepositoryPathPolicy(List.of(root)), new DefaultLanguageAnalyzerSelector(), (w, r, s) -> { if (calls.incrementAndGet() > 1) throw new IllegalStateException("simulated storage failure"); return new io.github.hanzales.codegraph.graph.InMemoryGraphRepository(); }, 3);
        service.registerWorkspace(new WorkspaceDefinition("workspace:test", "Test", Map.of()));
        service.registerRepository("workspace:test", new RepositoryDefinition("repo:test", "Test", root.toString(), "java", "", "", List.of(), List.of(), Map.of()));
        service.indexRepository("workspace:test", "repo:test"); var active = service.activeSnapshot("workspace:test", "repo:test").orElseThrow().snapshotId();
        service.indexRepository("workspace:test", "repo:test");
        assertEquals(active, service.activeSnapshot("workspace:test", "repo:test").orElseThrow().snapshotId());
    }
}
