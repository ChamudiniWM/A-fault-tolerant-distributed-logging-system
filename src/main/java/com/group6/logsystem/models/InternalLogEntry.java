package com.group6.logsystem.models;

import com.group6.logsystem.grpc.LogEntry;

import java.util.Objects;

public class InternalLogEntry {
    private final String logId;
    private final int index;       // New field for log index
    private final String nodeId;
    private final String message;
    private final long timestamp;
    private final int term;

    public InternalLogEntry(String logId,int index, String nodeId, String message, long timestamp, int term) {
        this.logId = logId;
        this.index = index;
        this.nodeId = nodeId;
        this.message = message;
        this.timestamp = timestamp;
        this.term = term;
    }


    public String getLogId() {
        return logId;
    }

    public int getIndex() {
        return index;
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

    public int getTerm() {
        return term;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + nodeId + ": " + message + " (Term " + term + ", Index " + index +"LogId"+logId + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalLogEntry that = (InternalLogEntry) o;
        return index == that.index &&
                timestamp == that.timestamp &&
                term == that.term &&
                Objects.equals(logId, that.logId) &&
                Objects.equals(nodeId, that.nodeId) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId , index, nodeId, message, timestamp, term);
    }

    public LogEntry toProto() {
        return LogEntry.newBuilder()
                .setLogId(logId) // Add this to your proto message too
                .setIndex(index) // Add this to your proto message too
                .setNodeId(nodeId)
                .setMessage(message)
                .setTimestamp(timestamp)
                .setTerm(term)
                .build();
    }

    public InternalLogEntry clone() {
        return new InternalLogEntry(this.logId,this.index, this.nodeId, this.message, this.timestamp, this.term);
    }
}