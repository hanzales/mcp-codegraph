package io.github.hanzales.codegraph.core;

import java.util.List;

public record ImpactResult(String subject, String risk, List<GraphNode> impacted, List<List<String>> paths, List<String> limitations) {
    public ImpactResult { impacted = List.copyOf(impacted); paths = List.copyOf(paths); limitations = List.copyOf(limitations); }
}
