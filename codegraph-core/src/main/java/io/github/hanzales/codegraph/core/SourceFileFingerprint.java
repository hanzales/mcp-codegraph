package io.github.hanzales.codegraph.core;

/** Content identity for one repository-relative source file. */
public record SourceFileFingerprint(String relativePath, String sha256, long size) { }
