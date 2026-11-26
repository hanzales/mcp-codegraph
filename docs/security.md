# Security model

The server is read-only. It should run with a repository allowlist and a
dedicated OS identity. Use canonical paths to prevent traversal and symlink
escapes, cap input/output sizes, and keep HTTP bound to localhost until TLS,
Origin validation, authentication, and authorization are configured.

Workspace repository registration enforces canonical containment under the
configured `codegraph.security.allowed-roots`. An empty allowlist rejects
workspace registrations. Each registered repository receives an isolated graph
and status; workspace aggregation prefixes IDs rather than merging raw IDs.
This is process-local isolation, not a substitute for OS permissions or a
multi-tenant authorization system.
