package io.github.hanzales.codegraph.core;

import java.util.List;

public record GitDiffReport(String repository, String base, String target, List<ChangedFile> files, List<String> warnings) {
    public GitDiffReport { files = List.copyOf(files); warnings = List.copyOf(warnings); }
}
