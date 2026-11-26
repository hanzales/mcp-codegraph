# MCP tools

The MVP exposes bounded tools for indexing, summaries, symbol search/details,
direct dependencies/dependents, Spring endpoint discovery, and impact analysis.
All inputs are typed and all outputs include source evidence or limitations.

`analyze_git_diff` supports read-only working-tree/staged analysis and explicit
base/target revision comparison. It reports changed files, edit ranges, and
Java declarations overlapping changed lines.

Workspace tools are also available:

- `register_workspace`, `register_repository`, `remove_repository`
- `index_repository`, `index_workspace`, `get_workspace_index_status`
- `get_workspace_repositories`
- `search_workspace_symbols`, `search_repository_symbols`

Workspace search results use namespaced IDs. Repository registration is local
and requires the path to be inside `codegraph.security.allowed-roots` (or the
equivalent `CODEGRAPH_SECURITY_ALLOWED_ROOTS` environment variable). Automatic
language detection is extension-based; Java is the only analyzer currently
implemented. No cross-repository dependency edges are inferred.
