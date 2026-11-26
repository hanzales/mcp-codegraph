# Architecture

MCP CodeGraph is a read-only static-analysis service. `ProjectIndexer`
discovers source files, delegates language-specific extraction to a
`LanguageAnalyzer`, and writes provider-neutral nodes and evidence-backed
relationships to a `GraphRepository`. `GraphImpactAnalyzer` performs bounded
incoming traversal. `CodeGraphToolService` exposes application use cases, and
the Spring AI adapter exposes those use cases through MCP.

The detailed audit and target platform architecture are documented in
[`architecture/current-state.md`](architecture/current-state.md) and
[`architecture/software-intelligence.md`](architecture/software-intelligence.md).
