package com.group6.logsystem.models;

public class InternalLogEntry {
    private final String nodeId;
    private final String message;
    private final long timestamp;

    public InternalLogEntry(String nodeId, String message, long timestamp) {
        this.nodeId = nodeId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + nodeId + ": " + message;
    }
}