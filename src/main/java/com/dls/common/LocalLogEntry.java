package com.dls.common;

import com.dls.raft.rpc.LogEntry;  // gRPC-generated LogEntry

import java.util.ArrayList;
import java.util.List;

public class LocalLogEntry {
    private int index;             // <-- Added index
    private int term;
    private String command;
    private long timestamp;
    private String id;
    private String data;
    private String metadata;

    public LocalLogEntry(int index, int term, String command, long timestamp, String id) {
        this.index = index;
        this.term = term;
        this.command = command;
        this.timestamp = timestamp;
        this.id = id;
    }

    // Create a LocalLogEntry from a raw log message (e.g., for LoggingServiceLoggerImpl)
    public static LocalLogEntry fromLogMessage(String nodeId, String message, long timestamp, int index, int term) {
        LocalLogEntry entry = new LocalLogEntry(index, term, message, timestamp, nodeId);
        entry.setMetadata("raw");
        return entry;
    }

    public LogEntry toGrpcLogEntry(int index, int term) {
        return LogEntry.newBuilder()
                .setCommand(this.getCommand())  // Assuming getCommand() is a method in LocalLogEntry
                .setIndex(index)
                .setTerm(term)
                .build();
    }

    // Static method to convert a list of LocalLogEntry to List<LogEntry>
    public static List<LogEntry> fromLocalLogEntryList(List<LocalLogEntry> localLogEntries, int term) {
        List<LogEntry> grpcEntries = new ArrayList<>();
        for (LocalLogEntry entry : localLogEntries) {
            grpcEntries.add(entry.toGrpcLogEntry(grpcEntries.size(), term));  // Pass current index and term
        }
        return grpcEntries;
    }


    // Convenience constructor if no ID provided
    public LocalLogEntry(int index, int term, String command, long timestamp) {
        this(index, term, command, timestamp, null);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "LocalLogEntry{" +
                "index=" + index +
                ", term=" + term +
                ", command='" + command + '\'' +
                ", timestamp=" + timestamp +
                (id != null ? ", id='" + id + '\'' : "") +
                '}';
    }

    // Convert from gRPC LogEntry to Java LocalLogEntry
    public static LocalLogEntry fromGrpcLogEntry(LogEntry grpcLogEntry) {
        LocalLogEntry entry = new LocalLogEntry(
                grpcLogEntry.getIndex(),
                grpcLogEntry.getTerm(),
                grpcLogEntry.getCommand(),
                grpcLogEntry.getTimestamp(),
                grpcLogEntry.getId()
        );

        entry.setData(grpcLogEntry.getData());
        entry.setMetadata(grpcLogEntry.getMetadata());

        return entry;
    }

    public static List<LocalLogEntry> fromGrpcLogEntryList(List<LogEntry> grpcEntries) {
        List<LocalLogEntry> localEntries = new java.util.ArrayList<>();
        for (LogEntry entry : grpcEntries) {
            localEntries.add(fromGrpcLogEntry(entry));
        }
        return localEntries;
    }

    // Convert from Java LocalLogEntry to gRPC LogEntry
    public LogEntry toGrpcLogEntry() {
        LogEntry.Builder builder = LogEntry.newBuilder()
                .setIndex(this.index)
                .setTerm(this.term)
                .setCommand(this.command)
                .setTimestamp(this.timestamp);

        if (this.id != null) {
            builder.setId(this.id);
        }
        if (this.data != null) {
            builder.setData(this.data);
        }
        if (this.metadata != null) {
            builder.setMetadata(this.metadata);
        }

        return builder.build();
    }
}