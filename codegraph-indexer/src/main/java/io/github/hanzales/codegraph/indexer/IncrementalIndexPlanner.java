package io.github.hanzales.codegraph.indexer;

import io.github.hanzales.codegraph.core.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

/** Builds bounded content-based plans; no source code or build command is executed. */
public final class IncrementalIndexPlanner {
    public IndexManifest manifest(Path root, List<Path> files, String analyzerVersion, String schemaVersion, String configurationFingerprint) throws IOException {
        var values = new TreeMap<String, SourceFileFingerprint>();
        for (var file : files) { var relative = root.relativize(file).toString().replace(FileSystems.getDefault().getSeparator(), "/"); var bytes = Files.readAllBytes(file); values.put(relative, new SourceFileFingerprint(relative, hash(bytes), bytes.length)); }
        return new IndexManifest(analyzerVersion, schemaVersion, configurationFingerprint, values);
    }
    public IncrementalIndexPlan plan(IndexingMode requested, IndexManifest previous, IndexManifest current, GraphRepository previousGraph) {
        if (requested == IndexingMode.FULL) return fallback(requested, "Full indexing was explicitly requested", current);
        if (previous == null) return fallback(requested, "No previous manifest is available", current);
        if (!Objects.equals(previous.analyzerVersion(), current.analyzerVersion()) || !Objects.equals(previous.graphSchemaVersion(), current.graphSchemaVersion())) return fallback(requested, "Analyzer or graph schema version changed", current);
        if (!Objects.equals(previous.configurationFingerprint(), current.configurationFingerprint())) return fallback(requested, "Indexing configuration changed", current);
        var old = previous.files(); var now = current.files(); var added = new TreeSet<String>(now.keySet()); added.removeAll(old.keySet());
        var deleted = new TreeSet<String>(old.keySet()); deleted.removeAll(now.keySet()); var modified = new TreeSet<String>();
        now.keySet().stream().filter(old::containsKey).filter(path -> !old.get(path).sha256().equals(now.get(path).sha256())).forEach(modified::add);
        var renamed = new TreeMap<String, String>();
        for (var deletedPath : new ArrayList<>(deleted)) for (var addedPath : new ArrayList<>(added)) if (old.get(deletedPath).sha256().equals(now.get(addedPath).sha256())) { renamed.put(deletedPath, addedPath); deleted.remove(deletedPath); added.remove(addedPath); break; }
        var changed = new TreeSet<String>(); changed.addAll(added); changed.addAll(modified); changed.addAll(deleted); changed.addAll(renamed.keySet()); changed.addAll(renamed.values());
        var invalidated = new TreeSet<>(changed);
        if (previousGraph != null) {
            var changedNodes = previousGraph.nodes().stream().filter(n -> changed.contains(n.file())).map(GraphNode::id).collect(java.util.stream.Collectors.toSet());
            previousGraph.relationships().stream().filter(e -> changedNodes.contains(e.sourceId()) || changedNodes.contains(e.targetId())).map(GraphRelationship::file).filter(Objects::nonNull).forEach(invalidated::add);
        }
        return new IncrementalIndexPlan(requested, false, "", added, modified, deleted, renamed, invalidated, List.of("Unresolved semantic dependencies may require a full run"));
    }
    private IncrementalIndexPlan fallback(IndexingMode requested, String reason, IndexManifest current) { return new IncrementalIndexPlan(requested, true, reason, current.files().keySet(), Set.of(), Set.of(), Map.of(), current.files().keySet(), List.of(reason)); }
    private static String hash(byte[] bytes) { try { var digest = MessageDigest.getInstance("SHA-256").digest(bytes); var out = new StringBuilder(); for (var b : digest) out.append(String.format("%02x", b)); return out.toString(); } catch (Exception e) { throw new IllegalStateException(e); } }
}
