package io.github.hanzales.codegraph.core;

import java.util.List;
import java.util.Map;

/** Additive compatibility adapter from the original MVP graph records. */
public final class GraphContractAdapter {
    private GraphContractAdapter() { }
    public static GraphContractNode node(GraphNode legacy, String repositoryId, String snapshotId, String language, String analyzer, String analyzerVersion) {
        // Keep the legacy kind prefix in the logical key so legacy relationship endpoints resolve to the same node.
        var logical = GraphIdentifiers.logical(language, legacy.id());
        var id = GraphIdentifiers.node(repositoryId, snapshotId, logical);
        return new GraphContractNode(id, logical, repositoryId, snapshotId, kind(legacy.kind()), legacy.name(), legacy.qualifiedName(), language, analyzer, analyzerVersion, new GraphSourceRange(legacy.startLine(), legacy.endLine()), Map.copyOf(legacy.attributes()));
    }
    public static GraphContractRelationship relationship(GraphRelationship legacy, String repositoryId, String snapshotId, String language, String analyzer, String analyzerVersion) {
        var source = GraphIdentifiers.node(repositoryId, snapshotId, GraphIdentifiers.logical(language, legacy.sourceId()));
        var target = GraphIdentifiers.node(repositoryId, snapshotId, GraphIdentifiers.logical(language, legacy.targetId()));
        var resolution = legacy.confidence() == Confidence.UNRESOLVED ? ResolutionStatus.UNRESOLVED : legacy.confidence() == Confidence.HIGH ? ResolutionStatus.CONFIRMED : ResolutionStatus.INFERRED;
        var evidence = List.of(new GraphEvidence(repositoryId, snapshotId, safePath(legacy.file()), new GraphSourceRange(legacy.startLine(), legacy.endLine()), legacy.evidence()));
        return new GraphContractRelationship(GraphIdentifiers.relationship(source, target, legacy.type()), source, target, legacy.type(), resolution, legacy.confidence(), evidence, Map.of("language", language, "analyzer", analyzer, "analyzerVersion", analyzerVersion));
    }
    private static GraphEntityKind kind(NodeKind kind) { return switch (kind) {
        case API_ENDPOINT -> GraphEntityKind.ENDPOINT;
        case DATABASE_TABLE -> GraphEntityKind.DATABASE_TABLE;
        case DATABASE_ENTITY -> GraphEntityKind.DATABASE_TABLE;
        case EXTERNAL_SERVICE -> GraphEntityKind.EXTERNAL_SERVICE;
        case CONFIGURATION -> GraphEntityKind.CONFIGURATION;
        case VALIDATION -> GraphEntityKind.VALIDATION;
        case BUSINESS_RULE -> GraphEntityKind.BUSINESS_RULE;
        case EVENT -> GraphEntityKind.EVENT;
        case INTERFACE -> GraphEntityKind.INTERFACE;
        case METHOD -> GraphEntityKind.METHOD;
        case CONSTRUCTOR -> GraphEntityKind.CONSTRUCTOR;
        case FIELD -> GraphEntityKind.FIELD;
        case CLASS -> GraphEntityKind.CLASS;
        case ENUM -> GraphEntityKind.ENUM;
        case RECORD -> GraphEntityKind.RECORD;
        case MODULE -> GraphEntityKind.MODULE;
        case PACKAGE -> GraphEntityKind.NAMESPACE;
        case REPOSITORY -> GraphEntityKind.REPOSITORY;
        default -> GraphEntityKind.CLASS;
    }; }
    private static String safePath(String path) { return path == null || path.isBlank() ? "unknown" : path.replace('\\', '/').replaceFirst("^/+", ""); }
}
