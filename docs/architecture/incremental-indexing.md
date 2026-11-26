# Incremental indexing

`ProjectIndexer.indexIncremental` builds a versioned `IndexManifest` containing
SHA-256 fingerprints for repository-relative source files. The planner reports
added, modified, deleted, and content-identical renamed files. It never executes
Maven, Gradle, application code, or shell commands.

`AUTO` and `INCREMENTAL` use the previous graph as reusable input only when the
analyzer version, graph schema version, and canonical analyzer configuration
fingerprint match. Otherwise the operation produces a full fallback plan and
rebuilds the graph. `FULL` always rebuilds. A new graph is constructed for the
run, so a failure cannot partially mutate the active snapshot.

Changed files and graph relationships that touch changed symbols are
invalidated. Unchanged nodes and safe relationships are copied into the new
graph; deleted and renamed source paths are therefore not retained. Because
static semantic dependencies can be unresolved, the plan records a limitation
and callers should prefer full mode after broad API, build configuration, or
dependency changes.

The operation is idempotent for the same manifest and bounded by the discovered
file set, graph result limits, cooperative cancellation, and optional timeout.
Repository-level workspace indexing remains serialized to prevent overlapping
runs for one registered repository. Incremental statistics expose discovered,
reused, analyzed, deleted, renamed, and invalidated counts.
