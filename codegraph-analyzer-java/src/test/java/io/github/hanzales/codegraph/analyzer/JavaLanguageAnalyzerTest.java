package io.github.hanzales.codegraph.analyzer;

import io.github.hanzales.codegraph.core.NodeKind;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JavaLanguageAnalyzerTest {
    @Test void extractsSymbolsSpringEndpointsAndValidations() throws Exception {
        var root = Files.createTempDirectory("codegraph");
        var file = root.resolve("OrderController.java");
        Files.writeString(file, "package demo; import org.springframework.web.bind.annotation.GetMapping; import jakarta.validation.constraints.NotBlank; class OrderController { @GetMapping(\"/orders\") String list(@NotBlank String customerId) { return service(); } String service(){ return \"ok\"; } }");
        var result = new JavaLanguageAnalyzer().analyze(root, List.of(file));
        assertTrue(result.nodes().stream().anyMatch(n -> n.kind() == NodeKind.CLASS && n.name().equals("OrderController")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.kind() == NodeKind.API_ENDPOINT));
        assertTrue(result.nodes().stream().anyMatch(n -> n.kind() == NodeKind.VALIDATION));
        assertTrue(result.relationships().stream().anyMatch(r -> r.type().equals("DECLARES")));
    }
}
