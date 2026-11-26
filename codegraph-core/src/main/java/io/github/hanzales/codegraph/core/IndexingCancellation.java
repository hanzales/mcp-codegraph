package io.github.hanzales.codegraph.core;

/** Small cooperative cancellation contract used by bounded indexing operations. */
@FunctionalInterface
public interface IndexingCancellation {
    boolean isCancellationRequested();
    static IndexingCancellation none() { return () -> false; }
}
