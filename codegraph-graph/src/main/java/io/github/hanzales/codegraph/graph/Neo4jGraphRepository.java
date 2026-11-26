package io.github.hanzales.codegraph.graph;

import io.github.hanzales.codegraph.core.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;

import java.util.*;

/**
 * Durable Neo4j provider. All values are parameters; the only interpolated
 * Cypher fragment is a validated relationship type from the known vocabulary.
 */
public final class Neo4jGraphRepository implements GraphRepository, PersistentGraphStore, AutoCloseable {
    private final Driver driver;
    private final String database;
    private final String workspaceId;
    private final String repositoryId;
    private final String snapshotId;

    public Neo4jGraphRepository(Driver driver, String database) { this(driver, database, "legacy", "legacy", "working-tree"); }
    public Neo4jGraphRepository(Driver driver, String database, String workspaceId, String repositoryId, String snapshotId) {
        this.driver = Objects.requireNonNull(driver); this.database = database == null || database.isBlank() ? "neo4j" : database;
        this.workspaceId = scope(workspaceId, "legacy"); this.repositoryId = scope(repositoryId, "legacy"); this.snapshotId = scope(snapshotId, "working-tree");
    }
    public void initializeSchema() {
        runWrite(tx -> { tx.run("CREATE CONSTRAINT codegraph_node_scope_key IF NOT EXISTS FOR (n:CodeNode) REQUIRE n.scopeKey IS UNIQUE").consume(); tx.run("CREATE INDEX codegraph_node_repository IF NOT EXISTS FOR (n:CodeNode) ON (n.workspaceId, n.repositoryId)").consume(); tx.run("CREATE INDEX codegraph_node_snapshot IF NOT EXISTS FOR (n:CodeNode) ON (n.snapshotId)").consume(); return null; });
    }
    public void clear() { clearRepository(workspaceId, repositoryId); }
    public void clearRepository(String workspace, String repository) { runWrite(tx -> { tx.run("MATCH (n:CodeNode {workspaceId:$workspaceId, repositoryId:$repositoryId}) DETACH DELETE n", Map.of("workspaceId", workspace, "repositoryId", repository)).consume(); return null; }); }
    public void clearWorkspace(String workspace) { runWrite(tx -> { tx.run("MATCH (n:CodeNode {workspaceId:$workspaceId}) DETACH DELETE n", Map.of("workspaceId", workspace)).consume(); return null; }); }
    public void upsertNode(GraphNode node) { upsertNodes(List.of(node)); }
    public void upsertNodes(Collection<GraphNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return;
        var values = nodes.stream().map(node -> { var value = new HashMap<String, Object>(); value.put("scopeKey", key(node.id())); value.put("id", node.id()); value.put("kind", node.kind().name()); value.put("name", node.name()); value.put("qualifiedName", node.qualifiedName()); value.put("file", node.file()); value.put("startLine", node.startLine()); value.put("endLine", node.endLine()); value.put("attributes", node.attributes()); value.put("workspaceId", workspaceId); value.put("repositoryId", repositoryId); value.put("snapshotId", snapshotId); return value; }).toList();
        runWrite(tx -> { tx.run("UNWIND $nodes AS node MERGE (n:CodeNode {scopeKey:node.scopeKey}) SET n.id=node.id, n.kind=node.kind, n.name=node.name, n.qualifiedName=node.qualifiedName, n.file=node.file, n.startLine=node.startLine, n.endLine=node.endLine, n.attributes=node.attributes, n.workspaceId=node.workspaceId, n.repositoryId=node.repositoryId, n.snapshotId=node.snapshotId", Map.of("nodes", values)).consume(); return null; });
    }
    public void addRelationship(GraphRelationship relationship) { addRelationships(List.of(relationship)); }
    public void addRelationships(Collection<GraphRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) return;
        runWrite(tx -> { for (var relationship : relationships) { var type = relationshipType(relationship.type()); var cypher = "MATCH (a:CodeNode {scopeKey:$source}), (b:CodeNode {scopeKey:$target}) MERGE (a)-[r:" + type + " {relationshipKey:$relationshipKey}]->(b) SET r.file=$file, r.startLine=$startLine, r.endLine=$endLine, r.confidence=$confidence, r.evidence=$evidence, r.workspaceId=$workspaceId, r.repositoryId=$repositoryId, r.snapshotId=$snapshotId"; var params = new HashMap<String, Object>(); params.put("source", key(relationship.sourceId())); params.put("target", key(relationship.targetId())); params.put("relationshipKey", relationship.sourceId() + "|" + relationship.targetId() + "|" + relationship.type()); params.put("file", relationship.file()); params.put("startLine", relationship.startLine()); params.put("endLine", relationship.endLine()); params.put("confidence", relationship.confidence().name()); params.put("evidence", relationship.evidence()); params.put("workspaceId", workspaceId); params.put("repositoryId", repositoryId); params.put("snapshotId", snapshotId); tx.run(cypher, params).consume(); } return null; });
    }
    public Optional<GraphNode> findNode(String id) { return query("MATCH (n:CodeNode {scopeKey:$scopeKey}) RETURN n", Map.of("scopeKey", key(id))).stream().findFirst().map(this::node); }
    public List<GraphNode> searchSymbols(String query, int limit) { return query("MATCH (n:CodeNode {workspaceId:$workspaceId, repositoryId:$repositoryId, snapshotId:$snapshotId}) WHERE toLower(n.name) CONTAINS toLower($query) OR toLower(n.qualifiedName) CONTAINS toLower($query) RETURN n ORDER BY n.id LIMIT $limit", Map.of("workspaceId", workspaceId, "repositoryId", repositoryId, "snapshotId", snapshotId, "query", query == null ? "" : query, "limit", Math.max(0, limit))).stream().map(this::node).toList(); }
    public List<GraphNode> nodes() { return query("MATCH (n:CodeNode {workspaceId:$workspaceId, repositoryId:$repositoryId, snapshotId:$snapshotId}) RETURN n ORDER BY n.id", scopeParams()).stream().map(this::node).toList(); }
    public List<GraphRelationship> relationships() { return relationships("MATCH (a:CodeNode {workspaceId:$workspaceId, repositoryId:$repositoryId, snapshotId:$snapshotId})-[r]->(b:CodeNode {workspaceId:$workspaceId, repositoryId:$repositoryId, snapshotId:$snapshotId}) RETURN a.id AS source, b.id AS target, type(r) AS type, r", scopeParams()); }
    public List<GraphRelationship> relationshipsFrom(String id) { return relationships("MATCH (a:CodeNode {scopeKey:$scopeKey})-[r]->(b:CodeNode {workspaceId:$workspaceId, repositoryId:$repositoryId, snapshotId:$snapshotId}) RETURN a.id AS source, b.id AS target, type(r) AS type, r", Map.of("scopeKey", key(id), "workspaceId", workspaceId, "repositoryId", repositoryId, "snapshotId", snapshotId)); }
    public List<GraphRelationship> relationshipsTo(String id) { return relationships("MATCH (a:CodeNode {workspaceId:$workspaceId, repositoryId:$repositoryId, snapshotId:$snapshotId})-[r]->(b:CodeNode {scopeKey:$scopeKey}) RETURN a.id AS source, b.id AS target, type(r) AS type, r", Map.of("scopeKey", key(id), "workspaceId", workspaceId, "repositoryId", repositoryId, "snapshotId", snapshotId)); }
    public GraphStorageStats statistics(String workspace, String repository) { var values = query("MATCH (n:CodeNode {workspaceId:$workspaceId, repositoryId:$repositoryId}) OPTIONAL MATCH (n)-[r]->() RETURN count(DISTINCT n) AS nodes, count(r) AS relationships", Map.of("workspaceId", workspace, "repositoryId", repository)); if (values.isEmpty()) return new GraphStorageStats(0, 0); var record = values.getFirst(); return new GraphStorageStats(record.get("nodes").asLong(), record.get("relationships").asLong()); }
    public GraphHealth health() { try { query("RETURN 1 AS ok", Map.of()); return new GraphHealth(true, "neo4j", "Connection is healthy"); } catch (RuntimeException exception) { return new GraphHealth(false, "neo4j", safeMessage(exception)); } }
    private Map<String, Object> scopeParams() { return Map.of("workspaceId", workspaceId, "repositoryId", repositoryId, "snapshotId", snapshotId); }
    private String key(String id) { return workspaceId + "|" + repositoryId + "|" + snapshotId + "|" + id; }
    private static String scope(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static String relationshipType(String value) { var type = value == null ? "" : value.replaceAll("[^A-Z0-9_]", "_"); if (type.isBlank() || !type.matches("[A-Z][A-Z0-9_]*")) throw new IllegalArgumentException("Invalid relationship type"); return type; }
    private <T> T runWrite(java.util.function.Function<org.neo4j.driver.TransactionContext, T> work) { try (var session = driver.session(SessionConfig.forDatabase(database))) { return session.executeWrite(work::apply); } catch (RuntimeException exception) { throw new GraphStorageException("Neo4j write failed", exception); } }
    private List<Record> query(String cypher, Map<String, Object> params) { try (var session = driver.session(SessionConfig.forDatabase(database))) { return session.run(cypher, params).list(); } catch (RuntimeException exception) { throw new GraphStorageException("Neo4j query failed", exception); } }
    private List<GraphRelationship> relationships(String cypher, Map<String, Object> params) { return query(cypher, params).stream().map(this::relationship).toList(); }
    private GraphNode node(Record record) { var value = record.get("n").asNode(); var attrs = new HashMap<String, String>(); value.get("attributes").asMap().forEach((k, v) -> attrs.put(k, String.valueOf(v))); return new GraphNode(value.get("id").asString(), NodeKind.valueOf(value.get("kind").asString()), value.get("name").asString(), value.get("qualifiedName").asString(), value.get("file").asString(), value.get("startLine").asInt(), value.get("endLine").asInt(), attrs); }
    private GraphRelationship relationship(Record record) { var relationship = record.get("r").asRelationship(); return new GraphRelationship(record.get("source").asString(), record.get("target").asString(), record.get("type").asString(), relationship.get("file").asString(), relationship.get("startLine").asInt(), relationship.get("endLine").asInt(), Confidence.valueOf(relationship.get("confidence").asString()), relationship.get("evidence").asString()); }
    private static String safeMessage(Exception exception) { return exception.getClass().getSimpleName() + ": connection unavailable"; }
    public void close() { driver.close(); }
}
