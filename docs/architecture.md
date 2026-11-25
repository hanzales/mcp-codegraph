# Architecture

The core contracts do not depend on Spring, Neo4j, or MCP. `ProjectIndexer`
discovers Java files, delegates AST extraction to `LanguageAnalyzer`, and writes
nodes/edges to `GraphRepository`. `GraphImpactAnalyzer` traverses incoming
relationships with explicit depth and result bounds. Spring AI adapts the
application service into MCP tools.
