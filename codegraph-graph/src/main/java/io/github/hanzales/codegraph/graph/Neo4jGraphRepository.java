package io.github.hanzales.codegraph.graph;

import io.github.hanzales.codegraph.core.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;
import java.util.*;

/** Neo4j-backed graph provider. All values are parameterized; relationship types are allow-listed. */
public final class Neo4jGraphRepository implements GraphRepository, AutoCloseable {
    private final Driver driver;
    private final String database;
    public Neo4jGraphRepository(Driver driver, String database) { this.driver = Objects.requireNonNull(driver); this.database = database == null || database.isBlank() ? "neo4j" : database; }
    public void clear() { execute("MATCH (n:CodeNode) DETACH DELETE n", Map.of()); }
    public void upsertNode(GraphNode node) {
        execute("MERGE (n:CodeNode {id:$id}) SET n.kind=$kind, n.name=$name, n.qualifiedName=$qualifiedName, n.file=$file, n.startLine=$startLine, n.endLine=$endLine, n.attributes=$attributes", Map.of("id", node.id(), "kind", node.kind().name(), "name", node.name(), "qualifiedName", node.qualifiedName(), "file", node.file(), "startLine", node.startLine(), "endLine", node.endLine(), "attributes", node.attributes()));
    }
    public void addRelationship(GraphRelationship relationship) {
        var type = relationship.type().replaceAll("[^A-Z0-9_]", "_");
        if (type.isBlank()) throw new IllegalArgumentException("Relationship type cannot be empty");
        execute("MATCH (a:CodeNode {id:$source}), (b:CodeNode {id:$target}) MERGE (a)-[r:" + type + "]->(b) SET r.file=$file, r.startLine=$startLine, r.endLine=$endLine, r.confidence=$confidence, r.evidence=$evidence", Map.of("source", relationship.sourceId(), "target", relationship.targetId(), "file", relationship.file(), "startLine", relationship.startLine(), "endLine", relationship.endLine(), "confidence", relationship.confidence().name(), "evidence", relationship.evidence()));
    }
    public Optional<GraphNode> findNode(String id) { return query("MATCH (n:CodeNode {id:$id}) RETURN n", Map.of("id", id)).stream().findFirst().map(this::node); }
    public List<GraphNode> searchSymbols(String query, int limit) { return query("MATCH (n:CodeNode) WHERE toLower(n.name) CONTAINS toLower($query) OR toLower(n.qualifiedName) CONTAINS toLower($query) RETURN n LIMIT $limit", Map.of("query", query, "limit", Math.max(0, limit))).stream().map(this::node).toList(); }
    public List<GraphNode> nodes() { return query("MATCH (n:CodeNode) RETURN n", Map.of()).stream().map(this::node).toList(); }
    public List<GraphRelationship> relationships() { return relationships("MATCH (a:CodeNode)-[r]->(b:CodeNode) RETURN a.id AS source, b.id AS target, type(r) AS type, r"); }
    public List<GraphRelationship> relationshipsFrom(String id) { return relationships("MATCH (a:CodeNode {id:$id})-[r]->(b:CodeNode) RETURN a.id AS source, b.id AS target, type(r) AS type, r", Map.of("id", id)); }
    public List<GraphRelationship> relationshipsTo(String id) { return relationships("MATCH (a:CodeNode)-[r]->(b:CodeNode {id:$id}) RETURN a.id AS source, b.id AS target, type(r) AS type, r", Map.of("id", id)); }
    private List<GraphRelationship> relationships(String cypher) { return relationships(cypher, Map.of()); }
    private List<GraphRelationship> relationships(String cypher, Map<String, Object> params) { try (var session = driver.session(SessionConfig.forDatabase(database))) { return session.run(cypher, params).list(this::relationship); } }
    private List<Record> query(String cypher, Map<String, Object> params) { try (var session = driver.session(SessionConfig.forDatabase(database))) { return session.run(cypher, params).list(); } }
    private void execute(String cypher, Map<String, Object> params) { try (var session = driver.session(SessionConfig.forDatabase(database))) { session.executeWrite(tx -> { tx.run(cypher, params).consume(); return null; }); } }
    private GraphNode node(Record record) { var value = record.get("n").asNode(); var attrs = new HashMap<String, String>(); value.get("attributes").asMap().forEach((k, v) -> attrs.put(k, String.valueOf(v))); return new GraphNode(value.get("id").asString(), NodeKind.valueOf(value.get("kind").asString()), value.get("name").asString(), value.get("qualifiedName").asString(), value.get("file").asString(), value.get("startLine").asInt(), value.get("endLine").asInt(), attrs); }
    private GraphRelationship relationship(Record record) { var r = record.get("r").asRelationship(); return new GraphRelationship(record.get("source").asString(), record.get("target").asString(), record.get("type").asString(), r.get("file").asString(), r.get("startLine").asInt(), r.get("endLine").asInt(), Confidence.valueOf(r.get("confidence").asString()), r.get("evidence").asString()); }
    public void close() { driver.close(); }
}
