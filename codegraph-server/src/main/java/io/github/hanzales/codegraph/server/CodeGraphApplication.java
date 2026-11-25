package io.github.hanzales.codegraph.server;

import io.github.hanzales.codegraph.analyzer.JavaLanguageAnalyzer;
import io.github.hanzales.codegraph.core.GraphRepository;
import io.github.hanzales.codegraph.graph.InMemoryGraphRepository;
import io.github.hanzales.codegraph.indexer.ProjectIndexer;
import io.github.hanzales.codegraph.mcp.CodeGraphToolService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CodeGraphApplication {
    public static void main(String[] args) { SpringApplication.run(CodeGraphApplication.class, args); }
    @Bean GraphRepository graphRepository() { return new InMemoryGraphRepository(); }
    @Bean ProjectIndexer projectIndexer(GraphRepository graph) { return new ProjectIndexer(new JavaLanguageAnalyzer(), graph); }
    @Bean CodeGraphToolService codeGraphToolService(GraphRepository graph, ProjectIndexer indexer) { return new CodeGraphToolService(graph, indexer); }
    @Bean ToolCallbackProvider codeGraphToolCallbacks(CodeGraphTools tools) { return MethodToolCallbackProvider.builder().toolObjects(tools).build(); }
}
