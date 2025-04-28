package com.dls.common;

import com.dls.raft.rpc.LogEntry;  // gRPC-generated LogEntry

public class LocalLogEntry {
    private int term;
    private String command;

    public LocalLogEntry(int term, String command) {
        this.term = term;
        this.command = command;
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

    @Override
    public String toString() {
        return "LocalLogEntry{term=" + term + ", command='" + command + "'}";
    }

    // Convert from gRPC LogEntry to Java LocalLogEntry
    public static LocalLogEntry fromGrpcLogEntry(LogEntry grpcLogEntry) {
        return new LocalLogEntry(grpcLogEntry.getTerm(), grpcLogEntry.getCommand());
    }

    // Convert from Java LocalLogEntry to gRPC LogEntry
    public LogEntry toGrpcLogEntry() {
        return LogEntry.newBuilder()
                .setTerm(this.term)
                .setCommand(this.command)
                .build();
    }
}