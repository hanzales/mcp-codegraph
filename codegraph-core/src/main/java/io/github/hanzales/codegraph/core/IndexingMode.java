package io.github.hanzales.codegraph.core;

/** Requested indexing strategy. AUTO falls back to FULL when safety cannot be proven. */
public enum IndexingMode { FULL, INCREMENTAL, AUTO }
