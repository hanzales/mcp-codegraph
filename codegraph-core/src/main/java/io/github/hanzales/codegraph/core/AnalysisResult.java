package io.github.hanzales.codegraph.core;

import java.util.List;

public record AnalysisResult(List<GraphNode> nodes, List<GraphRelationship> relationships, List<AnalysisWarning> warnings) {
    public AnalysisResult { nodes = List.copyOf(nodes); relationships = List.copyOf(relationships); warnings = List.copyOf(warnings); }
}
