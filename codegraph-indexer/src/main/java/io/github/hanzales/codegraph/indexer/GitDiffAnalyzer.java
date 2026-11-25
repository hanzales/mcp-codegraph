package io.github.hanzales.codegraph.indexer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import io.github.hanzales.codegraph.core.*;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.api.Git;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;

/** Read-only Git diff analysis. It never changes the working tree or executes project builds. */
public final class GitDiffAnalyzer {
    public GitDiffReport workingTree(Path root, boolean staged) throws Exception {
        try (var repository = open(root); var git = new Git(repository)) {
            if (!staged) return workingTreeReport(root, repository, git.status().call());
            var command = git.diff().setCached(staged);
            return report(root, "HEAD", staged ? "INDEX" : "WORKTREE", command.call(), repository);
        }
    }
    private GitDiffReport workingTreeReport(Path root, Repository repository, org.eclipse.jgit.api.Status status) throws Exception {
        var files = new ArrayList<ChangedFile>();
        var paths = new TreeSet<String>();
        paths.addAll(status.getModified()); paths.addAll(status.getChanged()); paths.addAll(status.getAdded());
        paths.addAll(status.getUntracked()); paths.addAll(status.getRemoved()); paths.addAll(status.getMissing());
        var cache = repository.readDirCache();
        for (var path : paths) {
            var entry = cache.getEntry(path);
            var oldBytes = entry == null ? new byte[0] : repository.open(entry.getObjectId()).getBytes();
            var file = root.resolve(path);
            var newBytes = Files.exists(file) ? Files.readAllBytes(file) : new byte[0];
            var ranges = edits(oldBytes, newBytes);
            var type = status.getUntracked().contains(path) || status.getAdded().contains(path) ? "ADD" :
                    (status.getRemoved().contains(path) || status.getMissing().contains(path) ? "DELETE" : "MODIFY");
            files.add(new ChangedFile(path, type.equals("DELETE") ? DiffEntry.DEV_NULL : path, type, ranges, symbols(file, ranges)));
        }
        return new GitDiffReport(root.toAbsolutePath().normalize().toString(), "INDEX", "WORKTREE", files, List.of());
    }
    private List<ChangedRange> edits(byte[] oldBytes, byte[] newBytes) {
        var oldText = new RawText(oldBytes); var newText = new RawText(newBytes);
        var edits = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.MYERS).diff(RawTextComparator.DEFAULT, oldText, newText);
        return edits.stream().map(e -> new ChangedRange(e.getBeginA(), e.getLengthA(), e.getBeginB(), e.getLengthB())).toList();
    }
    public GitDiffReport commits(Path root, String base, String target) throws Exception {
        try (var repository = open(root); var git = new Git(repository); var revWalk = new RevWalk(repository)) {
            var oldTree = tree(repository, revWalk, base); var newTree = tree(repository, revWalk, target);
            var entries = git.diff().setOldTree(oldTree).setNewTree(newTree).call();
            return report(root, base, target, entries, repository);
        }
    }
    private GitDiffReport report(Path root, String base, String target, List<DiffEntry> entries, Repository repository) throws Exception {
        var files = new ArrayList<ChangedFile>(); var warnings = new ArrayList<String>();
        for (var entry : entries) {
            try {
                var ranges = ranges(repository, entry);
                var path = entry.getNewPath().equals(DiffEntry.DEV_NULL) ? entry.getOldPath() : entry.getNewPath();
                files.add(new ChangedFile(entry.getOldPath(), entry.getNewPath(), entry.getChangeType().name(), ranges, symbols(root.resolve(path), ranges)));
            } catch (Exception ex) { warnings.add(entry.getNewPath() + ": " + ex.getMessage()); }
        }
        return new GitDiffReport(root.toAbsolutePath().normalize().toString(), base, target, files, warnings);
    }
    private List<ChangedRange> ranges(Repository repository, DiffEntry entry) throws Exception {
        var formatter = new DiffFormatter(new ByteArrayOutputStream()); formatter.setRepository(repository); formatter.setDetectRenames(true);
        var header = formatter.toFileHeader(entry); formatter.close();
        return header.toEditList().stream().map(e -> new ChangedRange(e.getBeginA(), e.getLengthA(), e.getBeginB(), e.getLengthB())).toList();
    }
    private List<String> symbols(Path file, List<ChangedRange> ranges) throws Exception {
        if (!file.toString().endsWith(".java") || !Files.exists(file)) return List.of();
        var unit = StaticJavaParser.parse(file); var symbols = new LinkedHashSet<String>();
        for (TypeDeclaration<?> type : unit.findAll(TypeDeclaration.class)) if (overlaps(type.getBegin().map(p -> p.line).orElse(1), type.getEnd().map(p -> p.line).orElse(1), ranges)) symbols.add(type.getFullyQualifiedName().orElse(type.getNameAsString()));
        for (CallableDeclaration<?> callable : unit.findAll(CallableDeclaration.class)) if (overlaps(callable.getBegin().map(p -> p.line).orElse(1), callable.getEnd().map(p -> p.line).orElse(1), ranges)) symbols.add(callable.getDeclarationAsString(false, false, false));
        return List.copyOf(symbols);
    }
    private boolean overlaps(int start, int end, List<ChangedRange> ranges) { return ranges.stream().anyMatch(r -> { var changedStart = r.newStart() == 0 ? 1 : r.newStart(); var changedEnd = changedStart + Math.max(r.newLength(), 1) - 1; return start <= changedEnd && end >= changedStart; }); }
    private Repository open(Path root) throws Exception { return new FileRepositoryBuilder().setWorkTree(root.toFile()).findGitDir(root.toFile()).readEnvironment().build(); }
    private CanonicalTreeParser tree(Repository repository, RevWalk walk, String ref) throws Exception { var commit = walk.parseCommit(repository.resolve(ref)); var parser = new CanonicalTreeParser(); try (var reader = repository.newObjectReader()) { parser.reset(reader, commit.getTree()); } return parser; }
}
