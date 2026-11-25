package io.github.hanzales.codegraph.core;

import java.nio.file.Path;
import java.util.List;

public interface LanguageAnalyzer { String language(); AnalysisResult analyze(Path root, List<Path> sourceFiles); }
