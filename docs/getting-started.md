# Getting started

Install Java 21 and Maven, then run `mvn test`. Start the STDIO server with:

```bash
mvn -pl codegraph-server -am spring-boot:run
```

Configure the MCP client to launch the server from the repository root. Call
`scan_project` with an absolute local path. The current MVP stores the graph in
memory and must be rescanned after a process restart.
