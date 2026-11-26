package io.github.hanzales.codegraph.core;

public record GraphSourceRange(int startLine, int startColumn, int endLine, int endColumn) {
    public GraphSourceRange {
        if (startLine < 0 || startColumn < 0 || endLine < 0 || endColumn < 0) throw new IllegalArgumentException("Source positions cannot be negative");
    }
    public GraphSourceRange(int startLine, int endLine) { this(startLine, 0, endLine, 0); }
}
