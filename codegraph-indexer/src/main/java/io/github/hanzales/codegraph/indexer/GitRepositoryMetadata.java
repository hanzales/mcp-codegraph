package io.github.hanzales.codegraph.indexer;

/** Read-only source state discovered from Git or a non-Git directory. */
public record GitRepositoryMetadata(boolean git, String branch, String commitHash,
                                    boolean dirty, String workingTreeFingerprint,
                                    String sourceFingerprint) {
    public boolean exactCommittedSnapshot() { return git && !dirty && !commitHash.isBlank(); }
}
