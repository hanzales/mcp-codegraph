package io.github.hanzales.codegraph.core;

public record GraphRelationship(String sourceId, String targetId, String type, String file, int startLine, int endLine, Confidence confidence, String evidence) { }
