package io.github.hanzales.codegraph.core;

import java.util.Map;

public record GraphNode(String id, NodeKind kind, String name, String qualifiedName, String file, int startLine, int endLine, Map<String, String> attributes) {
    public GraphNode { attributes = attributes == null ? Map.of() : Map.copyOf(attributes); }
}
