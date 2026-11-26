# Multi-repository workspaces

`WorkspaceIndexService` is the Phase 1 registry and lifecycle boundary. A
workspace owns a map of registered repositories, and each repository owns an
independent graph provider and index status. Removing one repository drops only
that repository state; other repositories and workspaces are not cleared.

## Registration

`WorkspaceDefinition.workspaceId` and `RepositoryDefinition.repositoryId` are
caller-visible stable identifiers. Repository paths are canonicalized at
registration. `RepositoryPathPolicy` requires every path to be an existing
directory under one of the configured allowed roots and therefore rejects
traversal and symlink escapes through `toRealPath()` containment checks.

An empty allowed-root configuration rejects all workspace repository
registrations. The legacy single-repository `scan_project` flow remains
available and is not silently changed by workspace registration.

## Analyzer selection

`DefaultLanguageAnalyzerSelector` uses an explicit language when supplied or a
deterministic extension count when `auto` is selected. Java is currently the
only implemented analyzer. C#, JavaScript, TypeScript, and SQL are detected but
reported as `PARTIAL` with an unsupported-analyzer error; no fake graph facts
are produced.

## Isolation and queries

Each repository uses a separate in-memory graph in the workspace registry.
Repository queries only read that graph. Workspace search aggregates results
and prefixes returned node IDs with `workspaceId/repositoryId/`, preventing
identifier collisions in the combined response. This is an aggregation view,
not cross-repository dependency resolution.

## Lifecycle states

- `REGISTERED`: repository metadata is valid but no index has run.
- `INDEXING`: an index operation is active.
- `INDEXED`: indexing completed without analyzer warnings.
- `PARTIAL`: unsupported language or analyzer warnings produced an incomplete
  result; other repositories remain usable.
- `FAILED`: indexing could not produce a result.
- `REMOVED`: reserved for durable registries; current removal deletes the state
  from the active registry and does not affect unrelated data.

The registry is process-local in this phase. Restarting the server removes
workspace registrations and repository graphs. Persistent catalog storage and
distributed lifecycle coordination are future work.
