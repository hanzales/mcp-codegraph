package io.github.hanzales.codegraph.indexer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class GitRepositoryMetadataReaderTest {
    @Test void discoversCommitAndDirtyFingerprintWithoutChangingRepository() throws Exception {
        var root = Files.createTempDirectory("codegraph-git");
        try (var git = Git.init().setDirectory(root.toFile()).call()) {
            Files.writeString(root.resolve("Example.java"), "class Example {}\n");
            git.add().addFilepattern("Example.java").call();
            git.commit().setMessage("initial").setAuthor(new PersonIdent("test", "test@example.invalid")).call();
            var reader = new GitRepositoryMetadataReader();
            var clean = reader.inspect(root);
            assertTrue(clean.git()); assertFalse(clean.dirty()); assertEquals(40, clean.commitHash().length());
            Files.writeString(root.resolve("Example.java"), "class Example { int value; }\n");
            var dirty = reader.inspect(root);
            assertTrue(dirty.dirty()); assertFalse(dirty.exactCommittedSnapshot());
            assertNotEquals(clean.sourceFingerprint(), dirty.sourceFingerprint());
            assertFalse(Files.exists(root.resolve(".git/index.lock")));
        }
    }

    @Test void fingerprintsNonGitDirectories() throws Exception {
        var root = Files.createTempDirectory("codegraph-non-git"); Files.writeString(root.resolve("README.md"), "sample");
        var metadata = new GitRepositoryMetadataReader().inspect(root);
        assertFalse(metadata.git()); assertEquals("non-git", metadata.branch()); assertEquals(metadata.sourceFingerprint(), metadata.workingTreeFingerprint());
    }
}
