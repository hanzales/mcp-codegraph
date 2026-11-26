# Versioned language-neutral graph contract

The interchange contract is named `mcp-codegraph.graph` and is currently at
schema version `1`. Its JSON Schema is
[`docs/schema/graph-contract.schema.json`](../schema/graph-contract.schema.json).
The contract is intentionally independent of JavaParser, Spring, Neo4j, MCP,
and any future language implementation.

## Identity model

Each node has two identifiers:

- `logicalId`: stable symbol identity within a language and repository context,
  such as `java:class:demo.OrderService`.
- `id`: snapshot-specific identity derived from repository ID, snapshot ID, and
  logical ID.

`GraphIdentifiers` uses deterministic SHA-256 values for repository, snapshot,
node, and relationship IDs. A language prefix and the legacy kind prefix keep
Java, C#, JavaScript, TypeScript, SQL, and future language symbols from
colliding. A logical symbol can therefore remain stable across snapshots while
the snapshot node identity changes.

## Nodes

The contract supports workspace, repository, snapshot, module/project,
namespace, code symbols, framework findings, UI concepts, database concepts,
external integrations, events, configuration, permissions, and tests. The
`metadata` object is an extensible typed JSON object; language-specific fields
must be placed there rather than added to core records.

## Relationships and evidence

Relationships use the shared vocabulary but carry `type` as a string so a
future producer can add a vocabulary value without making old readers fail.
Each relationship has:

- `resolution`: `CONFIRMED`, `INFERRED`, or `UNRESOLVED`;
- categorical `confidence`: `HIGH`, `MEDIUM`, `LOW`, or `UNRESOLVED`;
- zero or more structured evidence records;
- optional metadata.

Evidence contains repository ID, commit hash, repository-relative file path,
source range, and rationale. It must not contain absolute paths, credentials, or
raw source by default. Multiple evidence records are supported for relationships
derived from more than one source location.

## Deterministic serialization

`GraphContractSerializer` sorts nodes and relationships by ID and configures
Jackson to sort properties and map keys. This makes fixtures, cache keys, and
review diffs stable. Consumers must still treat arrays as semantically sets and
should not infer meaning from insertion order.

## Compatibility and migration

The original `GraphNode`, `GraphRelationship`, and `NodeKind` records remain in
place for existing Java analyzers, graph providers, and MCP consumers. They are
not silently changed. `GraphContractAdapter` provides an additive conversion to
the versioned representation and maps legacy confidence to resolution status:

- `HIGH` -> `CONFIRMED`;
- `MEDIUM`/`LOW` -> `INFERRED`;
- `UNRESOLVED` -> `UNRESOLVED`.

Legacy IDs remain readable during migration, but new persisted interchange
should use repository-scoped IDs. A future schema version must provide an
explicit migration adapter before changing field meanings or removing legacy
values. Unknown relationship vocabulary values must be preserved as strings.

The contract does not claim that an inferred relationship executes at runtime;
it only describes evidence and uncertainty produced by an analyzer.
