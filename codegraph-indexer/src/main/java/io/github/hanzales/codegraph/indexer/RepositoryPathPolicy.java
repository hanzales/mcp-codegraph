package io.github.hanzales.codegraph.indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Canonical path policy for workspace repositories. Empty allowlists deny all registrations. */
public final class RepositoryPathPolicy {
    private final List<Path> allowedRoots;
    public RepositoryPathPolicy(List<Path> allowedRoots) throws IOException {
        this.allowedRoots = allowedRoots == null ? List.of() : allowedRoots.stream().map(path -> {
            try { return path.toRealPath(); } catch (IOException e) { throw new IllegalArgumentException("Allowed root does not exist: " + path, e); }
        }).toList();
    }
    public Path validate(String candidate) throws IOException {
        if (candidate == null || candidate.isBlank()) throw new SecurityException("Repository path is required");
        var path = Path.of(candidate).toRealPath();
        if (!Files.isDirectory(path)) throw new SecurityException("Repository path is not a directory");
        if (allowedRoots.stream().noneMatch(path::startsWith)) throw new SecurityException("Repository path is outside configured allowed roots");
        return path;
    }
    public List<Path> allowedRoots() { return allowedRoots; }
}
