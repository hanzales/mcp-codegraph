package io.github.hanzales.codegraph.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codegraph.graph")
public record GraphStorageProperties(String provider, String uri, String username, String password, String database) {
    public String providerName() { return provider == null || provider.isBlank() ? "in-memory" : provider; }
    public String uriValue() { return uri == null || uri.isBlank() ? "bolt://localhost:7687" : uri; }
    public String usernameValue() { return username == null || username.isBlank() ? "neo4j" : username; }
    public String passwordValue() { return password == null ? "" : password; }
    public String databaseValue() { return database == null || database.isBlank() ? "neo4j" : database; }
}
