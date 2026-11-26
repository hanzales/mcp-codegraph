# Software Intelligence Platform architecture

## Product boundary

The long-term system is a read-only software knowledge service for an AI Agent
Orchestrator. It provides source-backed facts, relationships, uncertainty, and
impact hypotheses to Product Owner, Product Management, Impact Analysis, and
Developer agents. It does not execute application code, build scripts, SQL, or
LLM reasoning inside the analysis platform.

## Target logical architecture

```text
                 MCP / future API adapters
                           |
                 Application query services
                           |
       repository catalog + policy + result envelopes
                           |
      analysis orchestration / index lifecycle manager
             |                         |
    language analyzers          Git/change provider
 (Java, Kotlin, C#, ...)                |
             |                         |
       canonical IR + evidence + unresolved findings
                           |
              graph storage abstraction
                 |                    |
          in-memory/test          Neo4j/other
```

The canonical intermediate representation (IR) is the key multi-language
boundary. Language analyzers produce the same provider-neutral symbol,
relationship, evidence, and uncertainty concepts. Framework detectors are
language-specific plug-ins that enrich the IR; they must not write directly to
Neo4j or MCP responses.

## Platform concepts

### Repository identity

Every indexed repository needs a stable logical `repositoryId`, canonical root
identity, source provider metadata, and an access policy. A repository can have
multiple branches, commits, and index versions. The graph namespace should be
scoped by `(repositoryId, revisionId, analyzerVersion, schemaVersion)` rather
than by path alone.

### Revisions and snapshots

An index is an immutable snapshot or an explicitly versioned mutable working
state. A revision should identify Git commit/branch where available and a
content digest for non-Git sources. Query responses must identify which snapshot
they describe.

### Canonical IR

The IR should include:

- Symbol identity: language, module, package, kind, qualified signature, and
  stable identity strategy.
- Relationship identity: source, target, type, direction, extraction strategy,
  confidence, resolution status, and one or more evidence records.
- Evidence: repository-relative path, revision, line/column range, source hash,
  and optional AST or detector rationale. Do not store raw source by default.
- Findings: confirmed fact, candidate, unresolved reference, warning, or
  limitation with deterministic reason codes.

### Query and impact services

Queries should be read-only, bounded, paginated, and snapshot-consistent.
Impact analysis should distinguish direct evidence from transitive hypotheses,
preserve edge confidence, explain risk factors, and return limitations. A
future orchestrator can decide how to combine these facts; the platform should
not pretend that a static path is runtime execution.

## Multi-language extension model

Keep `LanguageAnalyzer` as the compatibility seam and add a richer optional
context contract in a later phase. A language plug-in should provide:

1. file/project discovery rules;
2. parser and symbol-resolution implementation;
3. mapping from language symbols to canonical IR;
4. language-specific detectors;
5. diagnostics and unresolved-reference reporting;
6. capability/version metadata.

Java remains the reference implementation. Kotlin can initially reuse JVM
framework detectors but must not reuse Java AST assumptions. C#, TypeScript,
Python, and Go should each have isolated modules and fixtures.

## Multi-repository model

The catalog should own repository registration, canonical path policy, source
provider metadata, allowed roots, snapshot lifecycle, and per-repository quotas.
Analyzers should receive a repository-scoped analysis context. Graph providers
should enforce repository and snapshot predicates on every query. MCP tools
should require an explicit repository handle rather than relying on the last
global scan.

## Scalability strategy

For hundreds of thousands of files:

- discover and hash files with bounded parallelism;
- skip unchanged files and invalidate only affected symbols;
- parse in batches and stream analysis results rather than retaining the whole
  repository in lists;
- use hash maps for symbol indexes and adjacency indexes;
- batch graph writes in transactions;
- separate hot query indexes from archival evidence;
- enforce file, repository, node, edge, traversal, and response budgets;
- expose progress and partial warnings without claiming completeness.

The first scalability milestone is not distributed processing. It is a correct
incremental single-process index with measurable memory and latency budgets.

## Security architecture

The platform must enforce canonical allowlisted roots, reject traversal and
symlink escapes, refuse to execute repository tools, redact secrets from
responses/logs, and apply per-repository quotas. HTTP transport authentication
and authorization belong at the deployment boundary and must not be confused
with static analysis confidence. Remote deployments require TLS, identity,
Origin validation, rate limits, and audit logging.

## Explicit non-goals for Phase 1

- No LLM dependency or prompt-injection classifier.
- No agent orchestration or workflow engine.
- No source-code editing, build execution, database execution, or shell access.
- No frontend application.
- No claim of complete runtime architecture reconstruction.
