# Current-state architecture audit

## Scope inspected

The audit covers the root Maven reactor, all six modules, `AGENTS.md`, the
architecture, security, impact, transport, graph-model, getting-started,
limitations, and roadmap documents, all production Java sources, the sample
commerce fixture, and the current JUnit tests.

## Existing capabilities

- Java 21 Maven multi-module build with JavaParser Symbol Solver.
- Java classes, interfaces, fields, methods, constructors, annotations, basic
  calls, Spring REST mappings, validation annotations, DI candidates, JPA
  entities/tables/relationships, configuration-property nodes, and conservative
  HTTP/messaging candidates.
- Stable string IDs for current node categories and source line evidence on
  relationships.
- In-memory graph provider and a Neo4j provider with parameterized values and an
  allow-listed relationship type normalization.
- Bounded incoming impact traversal with a transparent heuristic risk label.
- JGit working-tree, staged, and revision-to-revision diff reports.
- MCP tools for scanning, status, search, details, direct dependencies,
  dependents, impact, endpoint listing, summaries, and Git diff analysis.
- STDIO by default and opt-in Streamable HTTP with localhost binding, bearer
  token checking, and an Origin allowlist.
- A small Spring/JPA sample and CI running `mvn verify`.

## Main data flow

```text
local path
  -> ProjectIndexer.discover()
  -> LanguageAnalyzer.analyze(root, files)
  -> AnalysisResult(nodes, relationships, warnings)
  -> GraphRepository
  -> CodeGraphToolService
  -> Spring AI MCP tool callbacks
```

The current index operation calls `graph.clear()` and rebuilds one in-memory or
Neo4j graph. `IndexStatus` records counts and timing, but not an index version,
content hashes, branch identity, module identity, or a durable repository
namespace.

## Core abstractions and extension points

### `LanguageAnalyzer`

The contract is intentionally small: `language()` plus `analyze(root,
sourceFiles)`. It is a useful language plug-in seam, but it currently has no
analysis context, cancellation token, configuration, parser diagnostics,
incremental input, or language capability metadata. A future compatible
extension should add an optional context-oriented contract rather than making
Java-specific assumptions in `core`.

### `GraphRepository`

The contract supports CRUD-like node insertion, relationship insertion,
search, and one-hop adjacency queries. It keeps storage independent from
analysis, but it lacks repository/branch namespaces, batch operations,
transactions, pagination cursors, query budgets, indexes, and a way to express
graph consistency or index versions.

### Evidence model

`GraphRelationship` carries file, line range, confidence, and free-text
evidence. `GraphNode` carries one source location and string attributes. This
is adequate for the MVP, but insufficient for multiple evidence locations,
deleted symbols, generated-source provenance, extraction version, source hash,
or structured uncertainty reasons.

### Identifiers

IDs are currently strings such as `class:package.Type` and
`method:package.Type#method(Parameter)`. They are deterministic within the
current analyzer, but not globally namespaced by repository, revision, language,
module, or analyzer version. Simple type names in some unresolved edges can
collide across packages.

### Repository isolation

The server owns one mutable `GraphRepository` and one mutable `IndexStatus`.
Calling `scan_project` clears the graph. There is no concurrent multi-repository
model, tenant boundary, per-request repository selection after indexing, or
authorization check tied to a repository identity.

## Architectural limitations

- Discovery is Java-only, follows all regular files under the root, and has no
  canonical allowlist or symlink escape enforcement at the indexer boundary.
- Full parsing is performed for every discovered file; no content-hash or
  dependency-aware incremental index exists.
- `JavaLanguageAnalyzer` uses list scans for node de-duplication, which becomes
  expensive as symbol counts grow.
- `InMemoryGraphRepository` uses a concurrent node map but linear relationship
  scans and an unbounded relationship list.
- Neo4j writes are one session/transaction per operation; indexing can create a
  very large number of network round trips.
- `GraphImpactAnalyzer` traverses only incoming edges, does not weight edge
  confidence, and uses a heuristic risk label without a formal policy model.
- JGit symbol attribution reads the current work tree; deleted and renamed
  declarations cannot be reliably recovered from the current source alone.
- HTTP authentication is a single static bearer token, not identity,
  authorization, replay protection, or rate limiting.
- MCP input limits and repository allowlisting are described but not enforced
  as a shared policy component.
- The tool catalog is smaller than the long-term product vision and has no
  structured pagination envelope or correlation metadata.

## Reusable components without modification

- `GraphNode`, `GraphRelationship`, `Confidence`, `SourceLocation`, and the
  immutable result records are suitable as the first compatibility surface.
- `LanguageAnalyzer` can remain the language plug-in seam for Phase 1.
- `GraphRepository` can remain the storage boundary while a versioned,
  repository-scoped extension is introduced alongside it.
- `GraphImpactAnalyzer` is reusable as a bounded traversal kernel after its
  risk policy and edge-direction semantics are made explicit.
- `GitDiffAnalyzer` is reusable as a read-only change source, subject to
  revision-aware symbol attribution improvements.
- `CodeGraphToolService` is reusable as an application-service layer, but should
  stop owning global mutable index state as multi-repository support arrives.

## Security and compatibility findings

- Source content is untrusted and must never become executable input. Existing
  documentation states this rule, but path allowlisting and symlink checks need
  a single enforced implementation.
- Neo4j credentials are configuration-driven; operational guidance must prevent
  accidental logging and public exposure.
- Free-form Cypher is not exposed by the current API, which is a positive
  compatibility and security constraint to preserve.
- Changing ID formats, `NodeKind`, relationship names, or record fields can
  break persisted Neo4j data and MCP consumers. Introduce schema/index versions
  and migration rules before changing them.
- The graph is currently a single mutable snapshot. Concurrent scans and reads
  have no explicit consistency semantics.

## Immediate architectural priorities

1. Establish repository/revision/index identity and canonical path policy.
2. Add bounded, incremental indexing contracts without breaking current
   `LanguageAnalyzer` and `GraphRepository` consumers.
3. Replace free-form evidence strings with an additive structured evidence
   model while retaining legacy fields.
4. Add batch graph writes and indexed adjacency queries.
5. Expand MCP responses and tools only after the underlying contracts are
   versioned and repository-scoped.
