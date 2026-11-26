package io.github.hanzales.codegraph.core;

/** Stable relationship vocabulary. Unknown future values can be carried by the JSON contract as strings. */
public enum GraphRelationshipType {
    CONTAINS, DECLARES, CALLS, REFERENCES, IMPLEMENTS, INHERITS, EXTENDS,
    DEPENDS_ON, INJECTS, VALIDATES, ENFORCES, EXPOSES, SUBMITS_TO, READS,
    WRITES, PUBLISHES, CONSUMES, INTEGRATES_WITH, CONFIGURED_BY, TESTED_BY,
    MAPS_TO, RELATES_TO
}
