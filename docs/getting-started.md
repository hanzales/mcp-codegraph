# Getting started

Install Java 21 and Maven, then run `mvn test`. Start the STDIO server with:

```bash
mvn -pl codegraph-server -am spring-boot:run
```

Configure the MCP client to launch the server from the repository root. Call
`scan_project` with an absolute local path. The current MVP stores the graph in
memory and must be rescanned after a process restart. To persist it in Neo4j,
start `docker compose --profile neo4j up -d neo4j` and set
`CODEGRAPH_GRAPH_PROVIDER=neo4j` plus `CODEGRAPH_GRAPH_PASSWORD` before starting
the server.

## Multi-repository workspace mode

Workspace registration is local-only. The server never clones repositories or
changes branches. Configure allowed roots before using workspace tools:

```bash
export CODEGRAPH_SECURITY_ALLOWED_ROOTS="$HOME/workspaces"
mvn -pl codegraph-server -am spring-boot:run
```

Register a workspace, then register each local repository with
`register_repository`:

```json
{
  "workspaceId": "workspace:commerce",
  "repository": {
    "repositoryId": "repo:commerce-monolith",
    "displayName": "Commerce Monolith",
    "localPath": "/Users/me/workspaces/commerce-monolith",
    "language": "auto",
    "branch": "main",
    "serviceIdentity": "commerce-monolith",
    "includePatterns": [],
    "excludePatterns": ["**/target/**", "**/build/**"],
    "analyzerConfiguration": {}
  }
}
```

Use `index_workspace`, `get_workspace_index_status`,
`search_workspace_symbols`, or `search_repository_symbols`. Unsupported
languages are reported as `PARTIAL`; they do not prevent other repositories
from indexing. Cross-repository dependency resolution is intentionally deferred.
