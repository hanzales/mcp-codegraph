package io.github.hanzales.codegraph.core;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Comparator;

/** Deterministic JSON serializer for graph interchange and fixtures. */
public final class GraphContractSerializer {
    private final ObjectMapper mapper;
    public GraphContractSerializer() { mapper = new ObjectMapper().findAndRegisterModules().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true).configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true); }
    public String write(GraphContractDocument document) throws Exception {
        var normalized = new GraphContractDocument(document.contract(), document.schemaVersion(), document.repositoryId(), document.snapshotId(), document.commitHash(), document.analyzer(), document.nodes().stream().sorted(Comparator.comparing(GraphContractNode::id)).toList(), document.relationships().stream().sorted(Comparator.comparing(GraphContractRelationship::id)).toList(), document.metadata());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(normalized);
    }
    public GraphContractDocument read(String json) throws Exception { return mapper.readValue(json, GraphContractDocument.class); }
}
