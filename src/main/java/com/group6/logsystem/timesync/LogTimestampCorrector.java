package com.group6.logsystem.timesync;

import com.group6.logsystem.grpc.LogEntry;

import java.util.PriorityQueue;
import java.util.Comparator;

public class LogTimestampCorrector {

    private final PriorityQueue<LogEntry> reorderBuffer;

    public LogTimestampCorrector() {
        reorderBuffer = new PriorityQueue<>(Comparator.comparingLong(LogEntry::getTimestamp));
    }

    public void bufferLog(LogEntry entry) {
        reorderBuffer.offer(entry);
    }

    // Simulate flushing in order
    public void flushLogsInOrder() {
        while (!reorderBuffer.isEmpty()) {
            LogEntry entry = reorderBuffer.poll();
            System.out.println("Ordered Log: " + entry.getMessage() + " @ " + entry.getTimestamp());
        }
    }
}
