# Known limitations

The first release has Java-only AST analysis, unresolved method calls for many
external symbols. JGit diff analysis does not infer runtime behavior, generated
code, or deleted declarations from the current source tree. A Neo4j provider is available but
requires an explicitly configured Neo4j instance. There is no guarantee that a
statically detected relationship runs at runtime. Generated code, reflection,
dynamic SQL, and runtime configuration can be missed.

The analyzer includes conservative Spring REST and dependency-injection,
JPA entity/table, and HTTP/messaging call detection. URLs, Kafka topics,
proxy-generated beans, reflection, and runtime configuration are not resolved
without additional build or runtime metadata.

Streamable-HTTP mode is intentionally opt-in and token-protected. The filter
is a small deployment boundary, not a full identity provider: use TLS,
short-lived credentials, rate limits, and a reverse proxy for remote access.
