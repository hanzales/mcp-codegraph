package io.github.hanzales.codegraph.graph;

import io.github.hanzales.codegraph.core.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class GraphImpactAnalyzerTest {
    @Test void walksIncomingDependenciesWithBounds() {
        var graph = new InMemoryGraphRepository();
        graph.upsertNode(new GraphNode("method:controller", NodeKind.METHOD, "controller", "demo.Controller#run()", "Controller.java", 1, 2, Map.of()));
        graph.upsertNode(new GraphNode("method:service", NodeKind.METHOD, "service", "demo.Service#run()", "Service.java", 1, 2, Map.of()));
        graph.upsertNode(new GraphNode("endpoint:orders", NodeKind.API_ENDPOINT, "GET", "GET /orders", "Controller.java", 1, 1, Map.of()));
        graph.addRelationship(new GraphRelationship("method:controller", "method:service", "CALLS", "Controller.java", 2, 2, Confidence.HIGH, "test"));
        graph.addRelationship(new GraphRelationship("endpoint:orders", "method:controller", "EXPOSES", "Controller.java", 1, 1, Confidence.HIGH, "test"));
        var result = new GraphImpactAnalyzer(graph).analyze("method:service", 5, 10);
        assertEquals("HIGH", result.risk());
        assertEquals(2, result.impacted().size());
        assertFalse(result.paths().isEmpty());
    }
}
