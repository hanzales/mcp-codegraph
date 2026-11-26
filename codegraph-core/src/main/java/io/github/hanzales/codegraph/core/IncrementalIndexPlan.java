package io.github.hanzales.codegraph.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic plan and safety explanation for an incremental run. */
public record IncrementalIndexPlan(IndexingMode requestedMode, boolean fullFallback,
                                   String fallbackReason, Set<String> addedFiles,
                                   Set<String> modifiedFiles, Set<String> deletedFiles,
                                   Map<String, String> renamedFiles, Set<String> invalidatedFiles,
                                   List<String> diagnostics) {
    public IncrementalIndexPlan {
        fallbackReason = fallbackReason == null ? "" : fallbackReason;
        addedFiles = Set.copyOf(addedFiles == null ? Set.of() : addedFiles);
        modifiedFiles = Set.copyOf(modifiedFiles == null ? Set.of() : modifiedFiles);
        deletedFiles = Set.copyOf(deletedFiles == null ? Set.of() : deletedFiles);
        renamedFiles = Map.copyOf(renamedFiles == null ? Map.of() : renamedFiles);
        invalidatedFiles = Set.copyOf(invalidatedFiles == null ? Set.of() : invalidatedFiles);
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }
}
