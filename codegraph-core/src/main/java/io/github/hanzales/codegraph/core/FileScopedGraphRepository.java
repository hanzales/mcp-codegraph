package io.github.hanzales.codegraph.core;

import java.util.Set;

/** Optional provider capability for removing stale facts by source file. */
public interface FileScopedGraphRepository extends GraphRepository {
    void removeSourceFiles(Set<String> relativePaths);
}
