package io.github.hanzales.codegraph.server;

import io.github.hanzales.codegraph.analyzer.JavaLanguageAnalyzer;
import io.github.hanzales.codegraph.core.GraphRepository;
import io.github.hanzales.codegraph.graph.InMemoryGraphRepository;
import io.github.hanzales.codegraph.graph.Neo4jGraphRepository;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import io.github.hanzales.codegraph.indexer.ProjectIndexer;
import io.github.hanzales.codegraph.indexer.RepositoryPathPolicy;
import io.github.hanzales.codegraph.indexer.DefaultLanguageAnalyzerSelector;
import io.github.hanzales.codegraph.indexer.WorkspaceIndexService;
import io.github.hanzales.codegraph.mcp.CodeGraphToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties(GraphStorageProperties.class)
public class CodeGraphApplication {
    public static void main(String[] args) { SpringApplication.run(CodeGraphApplication.class, args); }
    @Bean @ConditionalOnMissingBean(GraphRepository.class) GraphRepository graphRepository() { return new InMemoryGraphRepository(); }
    @Bean @ConditionalOnProperty(name = "codegraph.graph.provider", havingValue = "neo4j") Driver neo4jDriver(
            GraphStorageProperties properties) {
        if (properties.passwordValue().isBlank()) throw new IllegalStateException("Neo4j provider requires codegraph.graph.password or CODEGRAPH_GRAPH_PASSWORD");
        return GraphDatabase.driver(properties.uriValue(), AuthTokens.basic(properties.usernameValue(), properties.passwordValue()));
    }
    @Bean @ConditionalOnProperty(name = "codegraph.graph.provider", havingValue = "neo4j") GraphRepository neo4jGraphRepository(Driver driver,
            GraphStorageProperties properties) { var repository = new Neo4jGraphRepository(driver, properties.databaseValue()); repository.initializeSchema(); return repository; }
    @Bean ProjectIndexer projectIndexer(GraphRepository graph) { return new ProjectIndexer(new JavaLanguageAnalyzer(), graph); }
    @Bean RepositoryPathPolicy repositoryPathPolicy(@Value("${codegraph.security.allowed-roots:}") String roots) throws Exception { var configured = Arrays.stream(roots.split(",")).map(String::trim).filter(value -> !value.isBlank()).map(Path::of).toList(); return new RepositoryPathPolicy(configured); }
    @Bean WorkspaceIndexService workspaceIndexService(RepositoryPathPolicy policy, ObjectProvider<Driver> driverProvider,
            GraphStorageProperties properties) {
        if (!properties.providerName().equalsIgnoreCase("neo4j")) return new WorkspaceIndexService(policy, new DefaultLanguageAnalyzerSelector());
        var driver = driverProvider.getIfAvailable(() -> { throw new IllegalStateException("Neo4j provider selected but no driver is configured"); });
        return new WorkspaceIndexService(policy, new DefaultLanguageAnalyzerSelector(), (workspace, repository) -> new Neo4jGraphRepository(driver, properties.databaseValue(), workspace, repository.repositoryId(), "working-tree"));
    }
    @Bean CodeGraphToolService codeGraphToolService(GraphRepository graph, ProjectIndexer indexer, WorkspaceIndexService workspaceIndexService) { return new CodeGraphToolService(graph, indexer, workspaceIndexService); }
    @Bean ToolCallbackProvider codeGraphToolCallbacks(CodeGraphTools tools) { return MethodToolCallbackProvider.builder().toolObjects(tools).build(); }
}
