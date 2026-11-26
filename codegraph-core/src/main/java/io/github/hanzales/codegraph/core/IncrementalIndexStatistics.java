package io.github.hanzales.codegraph.core;

/** Bounded, observable counters for one incremental attempt. */
public record IncrementalIndexStatistics(IndexingMode mode, int filesDiscovered,
                                         int filesReused, int filesAnalyzed,
                                         int filesDeleted, int filesRenamed,
                                         int invalidatedFiles, boolean fullFallback,
                                         long durationMs, String diagnostic) { }
