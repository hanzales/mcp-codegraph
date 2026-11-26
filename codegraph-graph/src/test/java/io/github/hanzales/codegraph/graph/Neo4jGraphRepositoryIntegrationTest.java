package io.github.hanzales.codegraph.graph;

import io.github.hanzales.codegraph.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against a real Neo4j instance when NEO4J_URI and NEO4J_PASSWORD are supplied. */
@EnabledIfEnvironmentVariable(named = "NEO4J_URI", matches = ".+")
class Neo4jGraphRepositoryIntegrationTest {
    @Test void dataSurvivesRepositoryRecreation() {
        var id = "integration-" + System.nanoTime();
        try (var first = new Neo4jGraphRepository(driver(), database(), "workspace:test", id, "snapshot:test")) {
            first.initializeSchema(); first.clear(); first.upsertNode(new GraphNode("class:Persisted", NodeKind.CLASS, "Persisted", "demo.Persisted", "Persisted.java", 1, 1, Map.of()));
            assertTrue(first.findNode("class:Persisted").isPresent());
        }
        try (var second = new Neo4jGraphRepository(driver(), database(), "workspace:test", id, "snapshot:test")) { assertTrue(second.findNode("class:Persisted").isPresent()); second.clear(); }
    }
    private static org.neo4j.driver.Driver driver() { return GraphDatabase.driver(System.getenv("NEO4J_URI"), AuthTokens.basic(System.getenv().getOrDefault("NEO4J_USER", "neo4j"), System.getenv("NEO4J_PASSWORD"))); }
    private static String database() { return System.getenv().getOrDefault("NEO4J_DATABASE", "neo4j"); }
}
