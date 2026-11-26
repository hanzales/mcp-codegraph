package io.github.hanzales.codegraph.server;

import org.neo4j.driver.Driver;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Explicit health signal; persistence failures are not converted to in-memory operation. */
@Component("codeGraphNeo4j")
@ConditionalOnProperty(name = "codegraph.graph.provider", havingValue = "neo4j")
public final class Neo4jHealthIndicator implements HealthIndicator {
    private final Driver driver;
    public Neo4jHealthIndicator(Driver driver) { this.driver = driver; }
    public Health health() {
        try (var session = driver.session()) { session.run("RETURN 1 AS ok").single(); return Health.up().withDetail("provider", "neo4j").build(); }
        catch (RuntimeException exception) { return Health.down().withDetail("provider", "neo4j").withDetail("message", "Neo4j connection unavailable").build(); }
    }
}
