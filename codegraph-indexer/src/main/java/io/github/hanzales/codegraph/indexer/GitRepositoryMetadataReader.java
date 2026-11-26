package io.github.hanzales.codegraph.indexer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

/** Inspects repository state without checkout, fetch, mutation, or build execution. */
public final class GitRepositoryMetadataReader {
    public GitRepositoryMetadata inspect(Path root) throws IOException {
        var canonical = root.toRealPath();
        var fingerprint = fingerprint(canonical);
        try (var repository = new FileRepositoryBuilder().findGitDir(canonical.toFile()).build();
             var git = new Git(repository)) {
            var head = repository.resolve(Constants.HEAD);
            var commit = head == null ? "" : head.name();
            var dirty = !git.status().call().isClean();
            return new GitRepositoryMetadata(true, repository.getBranch(), commit,
                    dirty, dirty ? fingerprint : "", dirty || commit.isBlank() ? fingerprint : commit);
        } catch (Exception notGit) {
            return new GitRepositoryMetadata(false, "non-git", "", false, fingerprint, fingerprint);
        }
    }

    private String fingerprint(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            var files = stream.filter(Files::isRegularFile)
                    .filter(path -> !excluded(root.relativize(path)))
                    .sorted().toList();
            var digest = sha();
            for (var file : files) {
                var relative = root.relativize(file).toString().replace(FileSystems.getDefault().getSeparator(), "/");
                digest.update(relative.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Files.readAllBytes(file));
                digest.update((byte) 0);
            }
            return hex(digest.digest());
        }
    }
    private static boolean excluded(Path relative) { for (var part : relative) if (Set.of(".git", "target", "build", "out", "node_modules").contains(part.toString())) return true; return false; }
    private static MessageDigest sha() { try { return MessageDigest.getInstance("SHA-256"); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static String hex(byte[] bytes) { var b = new StringBuilder(); for (byte value : bytes) b.append(String.format("%02x", value)); return b.toString(); }
}
