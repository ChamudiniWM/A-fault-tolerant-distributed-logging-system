package com.group6.logsystem.util;

import com.group6.logsystem.models.InternalLogEntry;  // Internal model
import com.group6.logsystem.grpc.LogEntry;

public class LogEntryConverter {

    // gRPC → Internal
    public static InternalLogEntry fromProto(LogEntry protoEntry) {
        return new InternalLogEntry(
                protoEntry.getLogId(),  // Assuming logId is part of the LogEntry
                protoEntry.getIndex(),
                protoEntry.getNodeId(),
                protoEntry.getMessage(),
                protoEntry.getTimestamp(),
                protoEntry.getTerm()
        );
    }


    // Internal → gRPC
    public static LogEntry toProto(InternalLogEntry logEntry) {
        return LogEntry.newBuilder()
                .setLogId(logEntry.getLogId())
                .setIndex(logEntry.getIndex())
                .setNodeId(logEntry.getNodeId())
                .setMessage(logEntry.getMessage())
                .setTimestamp(logEntry.getTimestamp())
                .setTerm(logEntry.getTerm())
                .build();
    }
}