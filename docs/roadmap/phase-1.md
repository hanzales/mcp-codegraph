# Phase 1: repository-scoped trustworthy index

## Objective

Turn the current single-repository Java MVP into a trustworthy, bounded,
repository-scoped indexing foundation without introducing LLMs, orchestration,
frontends, or major transport changes.

## Work packages

### 1. Repository and snapshot identity

- Add repository catalog records with stable logical IDs.
- Add revision/index/schema/analyzer version fields to `IndexStatus`.
- Namespace graph reads and writes by repository and snapshot.
- Keep legacy constructors and IDs readable during a compatibility window.

### 2. Enforced path policy

- Canonicalize roots once and reject traversal, non-allowlisted roots, and
  symlink escapes.
- Enforce maximum file size, repository file count, and total bytes.
- Return safe error codes without leaking credentials or host details.

### 3. Incremental index kernel

- Persist or inject a content-hash manifest.
- Analyze only added/changed files and remove deleted symbols.
- Produce deterministic invalidation warnings for unresolved dependencies.
- Preserve a full rebuild mode for recovery and compatibility.

### 4. Evidence and uncertainty

- Add structured evidence records alongside the current evidence string.
- Add resolution status and reason codes for unresolved relationships.
- Include snapshot identity in graph and MCP responses.

### 5. Graph access performance

- Add batch node/relationship writes as optional interfaces.
- Add indexed in-memory adjacency maps.
- Add bounded pagination and query budgets.
- Add Neo4j batch transaction implementation without changing core semantics.

### 6. Test and measurement baseline

- Add fixtures for path escape, symlink escape, deleted symbols, duplicate
  package names, multi-module repositories, and repository isolation.
- Add property tests for ID determinism and graph round trips.
- Add a reproducible benchmark fixture with documented hardware and dataset.
- Add a Neo4j Testcontainers suite when CI infrastructure permits.

## Acceptance criteria

- Two repositories can be indexed in one process without one clearing or
  returning the other repository's graph.
- A query response identifies repository, snapshot, schema, and analyzer
  versions.
- A path outside configured roots, a traversal path, and a symlink escape are
  rejected before file discovery.
- A second index of unchanged files performs no language parsing for those
  files and produces equivalent graph facts.
- Deleted files remove their symbols and relationships from the selected
  snapshot without modifying the source repository.
- In-memory and Neo4j providers pass the same contract tests for nodes,
  relationships, evidence, namespaces, and bounded queries.
- Large fixtures use bounded memory and return explicit limit warnings.
- Existing Java analyzer, JGit diff, STDIO, and opt-in HTTP behavior remains
  backward compatible or has a documented migration path.
- `mvn verify` passes, and no benchmark or completeness claim is published
  without recorded methodology and results.

## Exit deliverables

- Versioned repository/snapshot contracts.
- Enforced repository security policy.
- Incremental indexing prototype with full-rebuild fallback.
- Structured evidence compatibility layer.
- Provider contract test suite.
- Updated MCP tool schemas and documentation.
