package io.github.hanzales.codegraph.core;

import java.util.Map;

/** Versioned file manifest used to make incremental decisions reproducible. */
public record IndexManifest(String analyzerVersion, String graphSchemaVersion,
                            String configurationFingerprint, Map<String, SourceFileFingerprint> files) {
    public IndexManifest { files = Map.copyOf(files == null ? Map.of() : files); }
}
