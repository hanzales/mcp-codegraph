package io.github.hanzales.codegraph.core;

/** Result of an incremental or fallback indexing operation. */
public record IncrementalIndexResult(IndexStatus status, IndexManifest manifest,
                                     IncrementalIndexPlan plan,
                                     IncrementalIndexStatistics statistics) { }
