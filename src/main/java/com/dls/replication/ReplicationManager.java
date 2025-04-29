package com.dls.replication;

import com.dls.common.LocalLogEntry;
import com.dls.replication.LogRetriever;

import java.util.List;

public class ReplicationManager {

    private final LogRetriever logRetriever;

    public ReplicationManager(LogRetriever logRetriever) {
        this.logRetriever = logRetriever;
    }

    /**
     * Receive logs from other nodes, correct timestamps, and apply them.
     *
     * @param rawEntries List of raw log entries received.
     */
    public void replicateLogs(List<LocalLogEntry> rawEntries) {
        // Correct timestamps and reorder logs
        List<LocalLogEntry> orderedLogs = logRetriever.correctAndOrderLogs(rawEntries);

        // Now apply logs one by one
        for (LocalLogEntry entry : orderedLogs) {
            applyLog(entry);
        }
    }

    /**
     * Apply a single log entry.
     * You can expand this depending on your Raft log structure.
     */
    private void applyLog(LocalLogEntry entry) {
        // TODO: Actually append the entry to the Raft log
        System.out.println("Applying log entry ID: " + entry.getId() + ", Corrected Timestamp: " + entry.getTimestamp());
    }
}
