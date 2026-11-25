# Security model

The server is read-only. It should run with a repository allowlist and a
dedicated OS identity. Use canonical paths to prevent traversal and symlink
escapes, cap input/output sizes, and keep HTTP bound to localhost until TLS,
Origin validation, authentication, and authorization are configured.
