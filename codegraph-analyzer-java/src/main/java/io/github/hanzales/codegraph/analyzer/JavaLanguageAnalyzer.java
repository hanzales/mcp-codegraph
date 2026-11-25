package io.github.hanzales.codegraph.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.github.hanzales.codegraph.core.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Deterministic Java AST analyzer. Unresolved symbols are represented with LOW confidence edges. */
public final class JavaLanguageAnalyzer implements LanguageAnalyzer {
    public String language() { return "java"; }
    public AnalysisResult analyze(Path root, List<Path> sourceFiles) {
        var nodes = new ArrayList<GraphNode>(); var edges = new ArrayList<GraphRelationship>(); var warnings = new ArrayList<AnalysisWarning>();
        for (var file : sourceFiles) {
            try { analyzeFile(root, file, nodes, edges, warnings); }
            catch (RuntimeException | IOException ex) { warnings.add(new AnalysisWarning("JAVA_PARSE", ex.getMessage(), new SourceLocation(root.relativize(file).toString(), 1, 1))); }
        }
        return new AnalysisResult(nodes, edges, warnings);
    }
    private void analyzeFile(Path root, Path file, List<GraphNode> nodes, List<GraphRelationship> edges, List<AnalysisWarning> warnings) throws IOException {
        var relative = root.relativize(file).toString().replace('\\', '/'); var unit = StaticJavaParser.parse(file); var pkg = unit.getPackageDeclaration().map(p -> p.getNameAsString()).orElse("");
        for (var declaration : unit.findAll(ClassOrInterfaceDeclaration.class)) {
            var kind = declaration.isInterface() ? NodeKind.INTERFACE : NodeKind.CLASS; var qualified = pkg.isBlank() ? declaration.getNameAsString() : pkg + "." + declaration.getNameAsString(); var classId = id("class", qualified);
            var classAttributes = new HashMap<String, String>(); classAttributes.put("language", "java");
            for (var annotation : declaration.getAnnotations()) classAttributes.put("annotation:" + annotation.getNameAsString(), "true");
            add(nodes, new GraphNode(classId, kind, declaration.getNameAsString(), qualified, relative, line(declaration), end(declaration), classAttributes));
            for (var parent : declaration.getExtendedTypes()) edge(edges, classId, id("class", parent.getNameAsString()), "EXTENDS", relative, parent, Confidence.LOW, "JavaParser AST type reference");
            for (var parent : declaration.getImplementedTypes()) edge(edges, classId, id("class", parent.getNameAsString()), "IMPLEMENTS", relative, parent, Confidence.LOW, "JavaParser AST type reference");
            for (var field : declaration.getFields()) {
                var fieldId = id("field", qualified + "#" + field.getVariable(0).getNameAsString());
                add(nodes, new GraphNode(fieldId, NodeKind.FIELD, field.getVariable(0).getNameAsString(), qualified + "#" + field.getVariable(0).getNameAsString(), relative, line(field), end(field), annotations(field.getAnnotations())));
                edge(edges, classId, fieldId, "DECLARES", relative, field, Confidence.HIGH, "field declaration");
                if (hasAnnotation(field.getAnnotations(), "Autowired")) inject(edges, classId, field.getVariable(0).getType().asString(), relative, field, "Spring field injection");
                detectJpaField(qualified, field, fieldId, relative, nodes, edges);
            }
            for (var method : declaration.getMethods()) analyzeMethod(qualified, classId, method, relative, nodes, edges);
            for (var constructor : declaration.getConstructors()) { var methodId = id("constructor", qualified + "#" + constructor.getSignature()); add(nodes, new GraphNode(methodId, NodeKind.CONSTRUCTOR, constructor.getNameAsString(), qualified + "#" + constructor.getSignature(), relative, line(constructor), end(constructor), annotations(constructor.getAnnotations()))); edge(edges, classId, methodId, "DECLARES", relative, constructor, Confidence.HIGH, "constructor declaration"); for (var parameter : constructor.getParameters()) inject(edges, classId, parameter.getType().asString(), relative, parameter, "Spring constructor injection"); }
            for (var annotation : declaration.getAnnotations()) detectFrameworkNode(qualified, classId, annotation, relative, nodes, edges);
            detectJpaEntity(qualified, classId, declaration, relative, nodes, edges);
        }
    }
    private void analyzeMethod(String qualified, String classId, MethodDeclaration method, String file, List<GraphNode> nodes, List<GraphRelationship> edges) {
        var signature = qualified + "#" + method.getSignature(); var methodId = id("method", signature); add(nodes, new GraphNode(methodId, NodeKind.METHOD, method.getNameAsString(), signature, file, line(method), end(method), annotations(method.getAnnotations()))); edge(edges, classId, methodId, "DECLARES", file, method, Confidence.HIGH, "method declaration");
        method.findAll(MethodCallExpr.class).forEach(call -> edge(edges, methodId, id("method", call.getNameAsString()), "CALLS", file, call, Confidence.LOW, "unresolved method call"));
        method.findAll(MethodCallExpr.class).forEach(call -> detectIntegrationCall(methodId, call, file, nodes, edges));
        for (var annotation : method.getAnnotations()) detectFrameworkNode(signature, methodId, annotation, file, nodes, edges);
        for (var parameter : method.getParameters()) for (var annotation : parameter.getAnnotations()) detectFrameworkNode(signature + "#" + parameter.getNameAsString(), methodId, annotation, file, nodes, edges);
    }
    private void detectFrameworkNode(String owner, String ownerId, AnnotationExpr annotation, String file, List<GraphNode> nodes, List<GraphRelationship> edges) {
        var name = annotation.getNameAsString(); var endpoint = Set.of("RequestMapping", "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping").contains(name);
        if (endpoint) { var id = id("endpoint", owner + "@" + name + ":" + annotation); var attributes = new HashMap<String, String>(); attributes.put("annotation", name); attributes.put("route", annotationValue(annotation)); attributes.put("httpMethod", name.equals("RequestMapping") ? "ANY" : name.replace("Mapping", "").toUpperCase(Locale.ROOT)); add(nodes, new GraphNode(id, NodeKind.API_ENDPOINT, name, id, file, line(annotation), end(annotation), attributes)); edge(edges, ownerId, id, "EXPOSES", file, annotation, Confidence.MEDIUM, "Spring mapping annotation"); }
        if (Set.of("NotNull", "NotBlank", "NotEmpty", "Size", "Min", "Max", "Pattern", "Email", "Valid").contains(name)) { var id = id("validation", owner + "@" + name + ":" + line(annotation)); add(nodes, new GraphNode(id, NodeKind.VALIDATION, name, id, file, line(annotation), end(annotation), Map.of("annotation", name))); edge(edges, ownerId, id, "VALIDATES", file, annotation, Confidence.HIGH, "Jakarta validation annotation"); }
    }
    private void detectJpaEntity(String qualified, String classId, ClassOrInterfaceDeclaration declaration, String file, List<GraphNode> nodes, List<GraphRelationship> edges) {
        var entity = declaration.getAnnotationByName("Entity"); if (entity.isEmpty()) return;
        var entityId = id("entity", qualified); add(nodes, new GraphNode(entityId, NodeKind.DATABASE_ENTITY, declaration.getNameAsString(), qualified, file, line(declaration), end(declaration), Map.of("table", declaration.getAnnotationByName("Table").map(this::annotationValue).orElse(declaration.getNameAsString()))));
        edge(edges, classId, entityId, "MAPS_TO", file, entity.get(), Confidence.HIGH, "JPA @Entity annotation");
        var table = declaration.getAnnotationByName("Table").map(this::annotationValue).orElse(declaration.getNameAsString()); var tableId = id("table", table); add(nodes, new GraphNode(tableId, NodeKind.DATABASE_TABLE, table, table, file, line(declaration), end(declaration), Map.of("name", table))); edge(edges, entityId, tableId, "MAPS_TO", file, entity.get(), Confidence.MEDIUM, "JPA table mapping");
    }
    private void detectJpaField(String owner, FieldDeclaration field, String fieldId, String file, List<GraphNode> nodes, List<GraphRelationship> edges) { for (var annotation : field.getAnnotations()) if (Set.of("OneToMany", "ManyToOne", "OneToOne", "ManyToMany").contains(annotation.getNameAsString())) edge(edges, fieldId, id("class", field.getVariable(0).getType().asString()), "RELATES_TO", file, annotation, Confidence.MEDIUM, "JPA relationship annotation"); }
    private void inject(List<GraphRelationship> edges, String source, String targetType, String file, com.github.javaparser.ast.Node node, String evidence) { edge(edges, source, id("class", targetType.replaceAll("[<>].*", "")), "INJECTS", file, node, Confidence.MEDIUM, evidence); }
    private void detectIntegrationCall(String methodId, MethodCallExpr call, String file, List<GraphNode> nodes, List<GraphRelationship> edges) {
        var name = call.getNameAsString(); var scope = call.getScope().map(Object::toString).orElse("").toLowerCase(Locale.ROOT);
        var http = Set.of("getForObject", "postForObject", "exchange", "retrieve", "uri", "bodyToMono").contains(name) && (scope.contains("rest") || scope.contains("webclient") || scope.contains("http") || scope.contains("client"));
        var messaging = Set.of("send", "sendDefault", "publish", "convertAndSend").contains(name) && (scope.contains("kafka") || scope.contains("template") || scope.contains("producer") || scope.contains("event"));
        String kind = http ? "HTTP" : messaging ? "MESSAGING" : null; if (kind == null) return;
        var serviceId = id("external", kind + ":" + name); add(nodes, new GraphNode(serviceId, NodeKind.EXTERNAL_SERVICE, kind, serviceId, file, line(call), end(call), Map.of("operation", name))); edge(edges, methodId, serviceId, "CALLS", file, call, Confidence.LOW, "Spring integration call; target may be configured at runtime");
    }
    private boolean hasAnnotation(List<AnnotationExpr> annotations, String name) { return annotations.stream().anyMatch(a -> a.getNameAsString().equals(name)); }
    private String annotationValue(AnnotationExpr annotation) {
        var value = annotation.toString().replaceAll("^@\\w+\\s*", "").replaceAll("[{}\" ]", "");
        if (value.contains("name=")) value = value.substring(value.indexOf("name=") + 5);
        return value.replaceFirst("^value=", "").replaceAll("^[()]|[()]$", "").split(",", 2)[0];
    }
    private static Map<String, String> annotations(List<AnnotationExpr> annotations) { var map = new HashMap<String, String>(); annotations.forEach(a -> map.put("annotation:" + a.getNameAsString(), "true")); return map; }
    private static void add(List<GraphNode> nodes, GraphNode node) { if (nodes.stream().noneMatch(n -> n.id().equals(node.id()))) nodes.add(node); }
    private static void edge(List<GraphRelationship> edges, String source, String target, String type, String file, com.github.javaparser.ast.Node node, Confidence confidence, String evidence) { edges.add(new GraphRelationship(source, target, type, file, line(node), end(node), confidence, evidence)); }
    private static String id(String kind, String value) { return kind + ":" + value.replaceAll("\\s+", ""); }
    private static int line(com.github.javaparser.ast.Node node) { return node.getBegin().map(p -> p.line).orElse(1); }
    private static int end(com.github.javaparser.ast.Node node) { return node.getEnd().map(p -> p.line).orElse(line(node)); }
}
