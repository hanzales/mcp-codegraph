package io.github.hanzales.codegraph.core;

/** Lifecycle state of an immutable repository graph snapshot. */
public enum SnapshotStatus { INDEXING, INDEXED, ACTIVE, PARTIAL, FAILED, RETIRED }
