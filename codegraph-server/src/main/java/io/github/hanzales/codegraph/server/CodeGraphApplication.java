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
import org.springframework.context.annotation.Bean;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class CodeGraphApplication {
    public static void main(String[] args) { SpringApplication.run(CodeGraphApplication.class, args); }
    @Bean @ConditionalOnMissingBean(GraphRepository.class) GraphRepository graphRepository() { return new InMemoryGraphRepository(); }
    @Bean @ConditionalOnProperty(name = "codegraph.graph.provider", havingValue = "neo4j") Driver neo4jDriver(
            @Value("${codegraph.graph.uri:bolt://localhost:7687}") String uri,
            @Value("${codegraph.graph.username:neo4j}") String username,
            @Value("${codegraph.graph.password:change-me}") String password) {
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
    @Bean @ConditionalOnProperty(name = "codegraph.graph.provider", havingValue = "neo4j") GraphRepository neo4jGraphRepository(Driver driver,
            @Value("${codegraph.graph.database:neo4j}") String database) { return new Neo4jGraphRepository(driver, database); }
    @Bean ProjectIndexer projectIndexer(GraphRepository graph) { return new ProjectIndexer(new JavaLanguageAnalyzer(), graph); }
    @Bean RepositoryPathPolicy repositoryPathPolicy(@Value("${codegraph.security.allowed-roots:}") String roots) throws Exception { var configured = Arrays.stream(roots.split(",")).map(String::trim).filter(value -> !value.isBlank()).map(Path::of).toList(); return new RepositoryPathPolicy(configured); }
    @Bean WorkspaceIndexService workspaceIndexService(RepositoryPathPolicy policy) { return new WorkspaceIndexService(policy, new DefaultLanguageAnalyzerSelector()); }
    @Bean CodeGraphToolService codeGraphToolService(GraphRepository graph, ProjectIndexer indexer, WorkspaceIndexService workspaceIndexService) { return new CodeGraphToolService(graph, indexer, workspaceIndexService); }
    @Bean ToolCallbackProvider codeGraphToolCallbacks(CodeGraphTools tools) { return MethodToolCallbackProvider.builder().toolObjects(tools).build(); }
}
