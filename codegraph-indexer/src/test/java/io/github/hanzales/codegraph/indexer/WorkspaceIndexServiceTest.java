package io.github.hanzales.codegraph.indexer;

import io.github.hanzales.codegraph.core.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceIndexServiceTest {
    @Test void indexesMultipleRepositoriesWithIsolationAndNamespacedWorkspaceResults() throws Exception {
        var root = Files.createTempDirectory("codegraph-workspace");
        var commerce = Files.createDirectories(root.resolve("commerce"));
        var payments = Files.createDirectories(root.resolve("payments"));
        Files.writeString(commerce.resolve("Order.java"), "package commerce; class Order { String id() { return \"order\"; } }\n");
        Files.writeString(payments.resolve("Payment.java"), "package payments; class Payment { String id() { return \"payment\"; } }\n");
        var service = new WorkspaceIndexService(new RepositoryPathPolicy(List.of(root)), new DefaultLanguageAnalyzerSelector());
        service.registerWorkspace(new WorkspaceDefinition("workspace:commerce", "Commerce Platform", Map.of()));
        service.registerRepository("workspace:commerce", new RepositoryDefinition("repo:commerce", "Commerce", commerce.toString(), "auto", "main", "commerce", List.of(), List.of(), Map.of()));
        service.registerRepository("workspace:commerce", new RepositoryDefinition("repo:payments", "Payments", payments.toString(), "java", "main", "payments", List.of(), List.of(), Map.of()));

        assertEquals(RepositoryIndexState.INDEXED, service.indexRepository("workspace:commerce", "repo:commerce").state());
        assertEquals(RepositoryIndexState.INDEXED, service.indexRepository("workspace:commerce", "repo:payments").state());
        assertEquals(2, service.status("workspace:commerce").indexedCount());
        assertTrue(service.searchRepository("workspace:commerce", "repo:commerce", "Payment", 20).isEmpty());
        assertFalse(service.searchRepository("workspace:commerce", "repo:payments", "Payment", 20).isEmpty());
        var workspaceResults = service.searchWorkspace("workspace:commerce", "", 20);
        assertTrue(workspaceResults.size() >= 2);
        assertTrue(workspaceResults.stream().allMatch(node -> node.id().startsWith("workspace:commerce/")));

        assertTrue(service.removeRepository("workspace:commerce", "repo:payments"));
        assertEquals(1, service.status("workspace:commerce").repositoryCount());
        assertTrue(service.searchWorkspace("workspace:commerce", "", 20).stream().noneMatch(node -> node.id().contains("repo:payments")));
    }

    @Test void reportsUnsupportedLanguageAsPartialWithoutFailingOtherRepositories() throws Exception {
        var root = Files.createTempDirectory("codegraph-workspace-language");
        var repo = Files.createDirectories(root.resolve("frontend"));
        Files.writeString(repo.resolve("app.ts"), "export const app = true;\n");
        var service = new WorkspaceIndexService(new RepositoryPathPolicy(List.of(root)), new DefaultLanguageAnalyzerSelector());
        service.registerWorkspace(new WorkspaceDefinition("workspace:test", "Test", Map.of()));
        service.registerRepository("workspace:test", new RepositoryDefinition("repo:frontend", "Frontend", repo.toString(), "auto", "main", "frontend", List.of(), List.of(), Map.of()));
        var status = service.indexRepository("workspace:test", "repo:frontend");
        assertEquals(RepositoryIndexState.PARTIAL, status.state());
        assertTrue(status.error().contains("No analyzer"));
    }

    @Test void rejectsPathsOutsideAllowedRoots() throws Exception {
        var allowed = Files.createTempDirectory("codegraph-allowed");
        var outside = Files.createTempDirectory("codegraph-outside");
        var service = new WorkspaceIndexService(new RepositoryPathPolicy(List.of(allowed)), new DefaultLanguageAnalyzerSelector());
        service.registerWorkspace(new WorkspaceDefinition("workspace:secure", "Secure", Map.of()));
        assertThrows(SecurityException.class, () -> service.registerRepository("workspace:secure", new RepositoryDefinition("repo:outside", "Outside", outside.toString(), "auto", "", "", List.of(), List.of(), Map.of())));
    }
}
