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

    @Test void extractsSpringInjectionJpaMappingsAndIntegrationCalls() throws Exception {
        var root = Files.createTempDirectory("codegraph-frameworks");
        var file = root.resolve("OrderService.java");
        Files.writeString(file, """
                package demo;
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.stereotype.Service;
                import jakarta.persistence.Entity;
                import jakarta.persistence.Table;
                import jakarta.persistence.ManyToOne;
                import org.springframework.boot.context.properties.ConfigurationProperties;
                @Service class OrderService {
                  @Autowired PaymentService payment;
                  KafkaTemplate kafkaTemplate;
                  String save(Order order) { payment.send(order); kafkaTemplate.send(order); return "ok"; }
                }
                @Entity @Table(name=\"orders\") class Order { @ManyToOne Customer customer; }
                class PaymentService { void send(Order order) {} }
                class Customer {}
                @ConfigurationProperties(prefix="payments") class PaymentProperties { String url; }
                """);
        var result = new JavaLanguageAnalyzer().analyze(root, List.of(file));
        assertTrue(result.nodes().stream().anyMatch(n -> n.kind() == NodeKind.DATABASE_ENTITY && n.name().equals("Order")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.kind() == NodeKind.DATABASE_TABLE && n.name().equals("orders")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.kind() == NodeKind.EXTERNAL_SERVICE));
        assertTrue(result.nodes().stream().anyMatch(n -> n.kind() == NodeKind.CONFIGURATION && n.attributes().get("prefix").equals("payments")));
        assertTrue(result.relationships().stream().anyMatch(r -> r.type().equals("INJECTS")));
        assertTrue(result.relationships().stream().anyMatch(r -> r.type().equals("RELATES_TO")));
        assertTrue(result.relationships().stream().anyMatch(r -> r.type().equals("CALLS") && r.confidence() == io.github.hanzales.codegraph.core.Confidence.HIGH));
    }
}
