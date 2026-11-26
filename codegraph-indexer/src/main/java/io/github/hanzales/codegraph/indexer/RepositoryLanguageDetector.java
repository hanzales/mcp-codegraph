package io.github.hanzales.codegraph.indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/** Deterministic extension-based detector; detection never executes build tools. */
public final class RepositoryLanguageDetector {
    public String detect(Path root) throws IOException {
        var counts = Files.walk(root).filter(Files::isRegularFile).filter(path -> !path.toString().contains("/.git/")).map(this::language).filter(language -> !language.equals("unknown")).collect(Collectors.groupingBy(value -> value, Collectors.counting()));
        return counts.entrySet().stream().max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey())).map(Map.Entry::getKey).orElse("unknown");
    }
    private String language(Path path) { var name = path.getFileName().toString().toLowerCase(); return name.endsWith(".java") ? "java" : name.endsWith(".cs") ? "csharp" : name.endsWith(".ts") ? "typescript" : name.endsWith(".js") ? "javascript" : name.endsWith(".sql") ? "sql" : "unknown"; }
}
