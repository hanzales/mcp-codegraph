# Git-aware snapshots and graph versioning

Every indexed graph is immutable and scoped to `(workspaceId, repositoryId, snapshotId)`. The snapshot digest includes source state, branch/commit, analyzer version, and graph schema version, preventing relationships from incompatible source states from being mixed.

For a clean Git repository, the commit hash is exact. A dirty tree is never labelled as that commit: it includes a SHA-256 working-tree fingerprint and `exactCommittedSnapshot` is false. Non-Git directories use a deterministic content fingerprint and branch `non-git`.

Inspection is read-only. The indexer never checks out, fetches, pulls, pushes, or changes a branch. Source evidence remains attached to the graph belonging to the snapshot that produced it.

Each indexing run has its own `runId`. Only a fully successful run becomes active. Partial and failed runs remain visible as metadata but cannot replace the last successful snapshot. Workspace queries use each repository's active snapshot. `stale=true` means current source no longer matches the active snapshot.

The in-process default retains the active snapshot plus up to three historical snapshots (configurable through the lifecycle constructor). Comparison is a deterministic node and relationship set diff, not a runtime behavior guarantee. Neo4j receives the same snapshot scope and never silently falls back to memory.
