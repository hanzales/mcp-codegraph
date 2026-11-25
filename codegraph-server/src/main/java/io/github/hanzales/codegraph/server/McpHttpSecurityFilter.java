package io.github.hanzales.codegraph.server;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/** Minimal network boundary for the optional Streamable-HTTP transport. */
@Component
@ConditionalOnProperty(name = "codegraph.transport", havingValue = "streamable-http")
public final class McpHttpSecurityFilter extends OncePerRequestFilter {
    private final String token;
    private final List<String> allowedOrigins;
    private final String endpoint;

    public McpHttpSecurityFilter(
            @Value("${codegraph.http.auth-token:}") String token,
            @Value("${codegraph.http.allowed-origins:http://localhost:8080}") String origins,
            @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}") String endpoint) {
        this.token = token;
        this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        this.endpoint = endpoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().equals(endpoint)) { chain.doFilter(request, response); return; }
        var origin = request.getHeader("Origin");
        if (origin != null && !allowedOrigins.contains(origin)) { response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed"); return; }
        if (token.isBlank()) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "MCP HTTP authentication is not configured"); return; }
        var authorization = request.getHeader("Authorization");
        var presented = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : "";
        if (!constantTimeEquals(token, presented)) { response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid MCP HTTP credentials"); return; }
        chain.doFilter(request, response);
    }

    static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
