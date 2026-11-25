package io.github.hanzales.codegraph.core;

import java.util.List;

public record ChangedFile(String oldPath, String newPath, String changeType, List<ChangedRange> ranges, List<String> changedSymbols) {
    public ChangedFile { ranges = List.copyOf(ranges); changedSymbols = List.copyOf(changedSymbols); }
}
