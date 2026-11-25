package io.github.hanzales.codegraph.core;

public record AnalysisWarning(String code, String message, SourceLocation location) { }
