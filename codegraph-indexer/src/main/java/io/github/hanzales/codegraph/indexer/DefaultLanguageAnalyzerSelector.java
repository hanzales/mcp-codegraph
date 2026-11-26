package io.github.hanzales.codegraph.indexer;

import io.github.hanzales.codegraph.analyzer.JavaLanguageAnalyzer;
import io.github.hanzales.codegraph.core.LanguageAnalyzer;
import java.nio.file.Path;
import java.util.Optional;

/** Phase 1 selector: Java is implemented; other detected languages are reported as unsupported. */
public final class DefaultLanguageAnalyzerSelector implements LanguageAnalyzerSelector {
    private final RepositoryLanguageDetector detector;
    public DefaultLanguageAnalyzerSelector() { this(new RepositoryLanguageDetector()); }
    public DefaultLanguageAnalyzerSelector(RepositoryLanguageDetector detector) { this.detector = detector; }
    public Optional<LanguageAnalyzer> select(String requestedLanguage, Path repositoryRoot) throws Exception {
        var language = requestedLanguage == null || requestedLanguage.isBlank() || requestedLanguage.equalsIgnoreCase("auto") ? detector.detect(repositoryRoot) : requestedLanguage.toLowerCase();
        return language.equals("java") ? Optional.of(new JavaLanguageAnalyzer()) : Optional.empty();
    }
    public String detect(String requestedLanguage, Path repositoryRoot) throws Exception { return requestedLanguage == null || requestedLanguage.isBlank() || requestedLanguage.equalsIgnoreCase("auto") ? detector.detect(repositoryRoot) : requestedLanguage.toLowerCase(); }
}
