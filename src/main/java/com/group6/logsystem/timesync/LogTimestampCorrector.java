package com.group6.logsystem.timesync;

import com.group6.logsystem.grpc.LogEntry;

import java.util.ArrayList;
import java.util.List;

public class LogTimestampCorrector {

    private final List<LogEntry> bufferedLogs;

    public LogTimestampCorrector() {
        this.bufferedLogs = new ArrayList<>();
    }

    // Buffer logs for out-of-order timestamps
    public void bufferLog(LogEntry logEntry) {
        bufferedLogs.add(logEntry);
    }

    // Reorder the logs in timestamp order
    public void flushLogsInOrder() {
        bufferedLogs.sort((log1, log2) -> Long.compare(log1.getTimestamp(), log2.getTimestamp()));
        for (LogEntry log : bufferedLogs) {
            processLog(log);
        }
        bufferedLogs.clear();
    }

    // Process and append a log entry to the system (this could be saved to the local node's log)
    private void processLog(LogEntry logEntry) {
        System.out.println("Processed log: " + logEntry);
    }

    public List<LogEntry> getBufferedLogs() {
        return bufferedLogs;
    }
}