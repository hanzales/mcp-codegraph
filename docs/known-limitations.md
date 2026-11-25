# Known limitations

The first release has Java-only AST analysis, unresolved method calls for many
external symbols, and no full JGit diff tool. A Neo4j provider is available but
requires an explicitly configured Neo4j instance. There is no guarantee that a
statically detected relationship runs at runtime. Generated code, reflection,
dynamic SQL, and runtime configuration can be missed.
