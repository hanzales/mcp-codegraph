package io.github.hanzales.codegraph.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphContractTest {
    @Test void identifiersAreRepositoryAndSnapshotScoped() {
        var logical = GraphIdentifiers.logical("java", "class:demo.Order");
        assertEquals(logical, GraphIdentifiers.logical("java", "class:demo.Order"));
        assertNotEquals(GraphIdentifiers.node("repo:a", "snapshot:1", logical), GraphIdentifiers.node("repo:b", "snapshot:1", logical));
        assertNotEquals(GraphIdentifiers.node("repo:a", "snapshot:1", logical), GraphIdentifiers.node("repo:a", "snapshot:2", logical));
        assertNotEquals(GraphIdentifiers.node("repo:a", "snapshot:1", logical), GraphIdentifiers.node("repo:a", "snapshot:1", GraphIdentifiers.logical("csharp", "class:demo.Order")));
    }

    @Test void serializesDeterministicallyAndPreservesMultipleEvidence() throws Exception {
        var repo = "repo:test"; var snapshot = "snapshot:commit";
        var node = new GraphContractNode("node:1", "java:class:demo.Order", repo, snapshot, GraphEntityKind.CLASS, "Order", "demo.Order", "java", "java-analyzer", "1.0", new GraphSourceRange(1, 1), Map.of("framework", "spring"));
        var evidence = new GraphEvidence(repo, "commit", "src/Order.java", new GraphSourceRange(1, 1), "declaration");
        var relation = new GraphContractRelationship("rel:1", node.id(), node.id(), "REFERENCES", ResolutionStatus.INFERRED, Confidence.MEDIUM, List.of(evidence, evidence), Map.of());
        var document = new GraphContractDocument(GraphContractDocument.CONTRACT_NAME, 1, repo, snapshot, "commit", Map.of("language", "java"), List.of(node), List.of(relation), Map.of());
        var serializer = new GraphContractSerializer();
        assertEquals(serializer.write(document), serializer.write(serializer.read(serializer.write(document))));
        assertEquals(2, serializer.read(serializer.write(document)).relationships().getFirst().evidence().size());
    }

    @Test void adaptsLegacyRecordsWithoutChangingTheirShape() throws Exception {
        var legacy = new GraphNode("class:demo.Order", NodeKind.CLASS, "Order", "demo.Order", "src/Order.java", 2, 5, Map.of("annotation:Entity", "true"));
        var converted = GraphContractAdapter.node(legacy, "repo:test", "snapshot:commit", "java", "java-analyzer", "1.0");
        assertEquals(GraphEntityKind.CLASS, converted.kind());
        assertEquals("java:class:demo.Order", converted.logicalId());
        var legacyRelationship = new GraphRelationship(legacy.id(), legacy.id(), "REFERENCES", legacy.file(), 3, 3, Confidence.UNRESOLVED, "unresolved reference");
        var relationship = GraphContractAdapter.relationship(legacyRelationship, "repo:test", "snapshot:commit", "java", "java-analyzer", "1.0");
        assertEquals(ResolutionStatus.UNRESOLVED, relationship.resolution());
        assertEquals(1, relationship.evidence().size());
    }
}
