package io.github.hanzales.codegraph.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpHttpSecurityFilterTest {
    @Test void comparesTokensWithoutEarlyExit() {
        assertTrue(McpHttpSecurityFilter.constantTimeEquals("secret", "secret"));
        assertFalse(McpHttpSecurityFilter.constantTimeEquals("secret", "wrong"));
    }
}
