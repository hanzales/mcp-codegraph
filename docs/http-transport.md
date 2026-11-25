# Streamable HTTP transport

The server defaults to STDIO. Streamable HTTP is enabled only with the
`http` Spring profile and `codegraph.transport=streamable-http`.

```bash
export CODEGRAPH_MCP_TOKEN="a-long-random-secret"
export CODEGRAPH_MCP_ALLOWED_ORIGINS="http://localhost:8080"
mvn -pl codegraph-server -am spring-boot:run -Dspring-boot.run.profiles=http
```

The MCP endpoint is `POST http://127.0.0.1:8080/mcp`. Every request must carry
`Authorization: Bearer <token>`. If an `Origin` header is present, it must be
in the comma-separated allowlist. Empty tokens are rejected; credentials are
never logged.

This protection is deliberately small and local-first. For remote deployment,
put the service behind TLS, an authenticated reverse proxy or identity-aware
gateway, network restrictions, rate limits, and operational monitoring. Do not
bind the HTTP profile to a public interface without those controls.
