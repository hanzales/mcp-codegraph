package io.github.hanzales.codegraph.indexer;

import io.github.hanzales.codegraph.core.LanguageAnalyzer;
import java.nio.file.Path;
import java.util.Optional;

public interface LanguageAnalyzerSelector { Optional<LanguageAnalyzer> select(String requestedLanguage, Path repositoryRoot) throws Exception; }
