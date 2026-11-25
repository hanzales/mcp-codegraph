# MCP tools

The MVP exposes bounded tools for indexing, summaries, symbol search/details,
direct dependencies/dependents, Spring endpoint discovery, and impact analysis.
All inputs are typed and all outputs include source evidence or limitations.

`analyze_git_diff` supports read-only working-tree/staged analysis and explicit
base/target revision comparison. It reports changed files, edit ranges, and
Java declarations overlapping changed lines.
