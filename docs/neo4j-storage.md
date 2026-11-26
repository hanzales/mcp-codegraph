# Neo4j graph storage

Neo4j graph instances are constructed with workspace, repository, and immutable
snapshot scope. This prevents a query from traversing relationships produced by
different source states. The workspace indexer creates a new scoped provider for
each run; a failed write is surfaced as an error and never falls back to memory.

The in-memory provider remains the default. Select Neo4j explicitly with
`CODEGRAPH_GRAPH_PROVIDER=neo4j`; the application does not silently fall back
when Neo4j is unavailable.

## Local Docker setup

Credentials are required and are not committed:

```bash
export NEO4J_PASSWORD="use-a-local-secret"
docker compose --profile neo4j up -d neo4j
CODEGRAPH_GRAPH_PROVIDER=neo4j \
CODEGRAPH_GRAPH_PASSWORD="$NEO4J_PASSWORD" \
mvn -pl codegraph-server -am spring-boot:run
```

The provider creates a uniqueness constraint for scoped node keys and indexes
for repository and snapshot lookup. It stores workspace, repository, and
snapshot properties on every node and relationship. Repository `clear()` is
scoped to the provider instance; `clearRepository` and `clearWorkspace` expose
explicit cleanup boundaries.

## Durability and idempotency

Nodes are merged by a deterministic workspace/repository/snapshot/node key.
Relationships are merged by source, target, and relationship type. Batch node
and relationship writes run in Neo4j write transactions. Re-running an index is
therefore idempotent for the same scope; it does not create duplicate nodes or
edges.

## Failure behavior

Connection and query failures throw `GraphStorageException` or fail application
startup when the explicitly selected provider cannot initialize. No in-memory
fallback is attempted. The Actuator `codeGraphNeo4j` health indicator reports
the connection state at `/actuator/health`.

All values are parameters. Relationship types are validated to uppercase
identifier syntax before the required Cypher type interpolation. Client input
cannot provide arbitrary Cypher.

## Workspace repositories

Workspace indexing creates one scoped Neo4j provider per registered repository.
The default snapshot is `working-tree` until revision-aware workspace indexing
is added. Cross-repository dependency resolution is not performed.

Existing records written by the original unscoped provider are not silently
guessed into a workspace. Re-index the repository into the scoped schema or
remove legacy `CodeNode` data during a planned migration.
