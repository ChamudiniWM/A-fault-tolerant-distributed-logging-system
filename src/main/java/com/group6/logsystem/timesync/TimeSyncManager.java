package com.group6.logsystem.timesync;

import com.group6.logsystem.grpc.LogEntry;

public class TimeSyncManager {

    private final LogicalClock logicalClock;
    private final LogTimestampCorrector timestampCorrector;

    public TimeSyncManager() {
        this.logicalClock = new LogicalClock();
        this.timestampCorrector = new LogTimestampCorrector();
    }

    // Method to update the timestamp for an incoming log entry
    public long synchronizeTimestamp(LogEntry logEntry) {
        // First, update the logical clock when receiving a log
        long updatedTimestamp = logicalClock.receive(logEntry.getTimestamp());

        // Then, correct the log's timestamp if necessary
        logEntry = logEntry.toBuilder().setTimestamp(updatedTimestamp).build();
        timestampCorrector.bufferLog(logEntry);

        return updatedTimestamp;
    }

    // Method to flush and reorder the logs in timestamp order
    public void flushLogs() {
        timestampCorrector.flushLogsInOrder();
    }

    // Getter for the logical clock
    public LogicalClock getLogicalClock() {
        return logicalClock;
    }
}
