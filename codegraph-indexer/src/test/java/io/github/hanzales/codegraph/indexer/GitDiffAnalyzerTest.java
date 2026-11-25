package io.github.hanzales.codegraph.indexer;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class GitDiffAnalyzerTest {
    @Test void reportsWorkingTreeJavaChangesAndSymbols() throws Exception {
        var root = Files.createTempDirectory("codegraph-git");
        try (var git = Git.init().setDirectory(root.toFile()).call()) {
            var file = root.resolve("OrderService.java");
            Files.writeString(file, "class OrderService { String create(){ return \"ok\"; } }\n");
            git.add().addFilepattern("OrderService.java").call();
            git.commit().setMessage("initial").setAuthor("test", "test@example.com").call();
            Files.writeString(file, "class OrderService { String create(){ return \"changed\"; } }\n");
            var report = new GitDiffAnalyzer().workingTree(root, false);
            assertEquals(1, report.files().size());
            assertEquals("MODIFY", report.files().getFirst().changeType());
            assertTrue(report.files().getFirst().changedSymbols().stream().anyMatch(symbol -> symbol.contains("create")));
        }
    }

    @Test void comparesTwoCommitsWithoutChangingRepository() throws Exception {
        var root = Files.createTempDirectory("codegraph-git-commits");
        try (var git = Git.init().setDirectory(root.toFile()).call()) {
            var file = root.resolve("OrderService.java");
            Files.writeString(file, "class OrderService { String create(){ return \"ok\"; } }\n");
            git.add().addFilepattern("OrderService.java").call();
            var first = git.commit().setMessage("initial").setAuthor("test", "test@example.com").call();
            Files.writeString(file, "class OrderService { String create(){ return \"changed\"; } }\n");
            git.add().addFilepattern("OrderService.java").call();
            var second = git.commit().setMessage("change").setAuthor("test", "test@example.com").call();

            var report = new GitDiffAnalyzer().commits(root, first.getName(), second.getName());
            assertEquals(1, report.files().size());
            assertEquals("MODIFY", report.files().getFirst().changeType());
            assertTrue(report.files().getFirst().changedSymbols().stream().anyMatch(symbol -> symbol.contains("create")));
        }
    }
}
