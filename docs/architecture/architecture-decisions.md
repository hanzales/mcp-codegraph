# Architecture decisions

## ADR-001: Keep the analysis core transport- and provider-independent

**Status:** Accepted

The core domain exposes Java-neutral records and contracts. Spring AI/MCP and
Neo4j integrations remain adapters. This permits CLI, HTTP, tests, and future
transports without coupling analyzers to a protocol or database.

## ADR-002: Use a canonical intermediate representation for languages

**Status:** Proposed for the platform evolution

All language analyzers should emit shared symbols, relationships, evidence, and
uncertainty concepts. Language-specific details remain attributes or detector
extensions. This prevents a Java-shaped graph from becoming the permanent
platform contract.

## ADR-003: Treat static relationships as evidence-backed hypotheses

**Status:** Accepted

Every important relationship carries source location and confidence. Unresolved
references are represented rather than guessed. Impact results include
limitations and must not claim runtime execution.

## ADR-004: Scope graph data by repository and snapshot

**Status:** Proposed; Phase 1 priority

Path-only IDs and one global mutable graph are insufficient for multi-repository
operation. New contracts should carry repository and snapshot identity while
retaining a compatibility reader for current IDs during migration.

## ADR-005: Prefer bounded incremental indexing before distributed indexing

**Status:** Accepted for Phase 1

Hundreds of thousands of files first require correct hashing, invalidation,
streaming, batching, and budgets. Distributed workers add operational and
consistency complexity and are deferred until single-process measurements show
the need.

## ADR-006: Read-only analysis boundary

**Status:** Accepted

The platform never executes analyzed code, build tools, SQL, shell commands, or
client-provided Cypher and never modifies the analyzed repository. This is a
security and product boundary, not an optional deployment setting.

## ADR-007: No LLM in Phase 1

**Status:** Accepted

Deterministic AST, symbol, framework, Git, and graph analysis is the foundation.
LLM enrichment, if ever added, must be an isolated optional consumer of
evidence and must not replace deterministic facts or silently raise confidence.

## ADR-008: Preserve current public contracts where practical

**Status:** Accepted

Existing records, `LanguageAnalyzer`, `GraphRepository`, MCP tool names, and
default STDIO behavior are already usable. Add versioned interfaces or default
methods where possible; document ID/schema migrations before changing stored
data or response shapes.
