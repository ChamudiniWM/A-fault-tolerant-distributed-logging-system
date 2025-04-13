package com.group6.logsystem.grpc;

import com.group6.logsystem.interfaces.ILogService;
import com.group6.logsystem.models.InternalLogEntry;

import java.util.ArrayList;
import java.util.List;

public class LogServiceImplAdapter implements ILogService {

    private final List<InternalLogEntry> logEntries = new ArrayList<>();

    @Override
    public void appendLog(InternalLogEntry entry) {
        synchronized (logEntries) {
            logEntries.add(entry);
        }
    }

    @Override
    public List<InternalLogEntry> queryLogs(String nodeId, long startTime, long endTime) {
        List<InternalLogEntry> results = new ArrayList<>();
        synchronized (logEntries) {
            for (InternalLogEntry entry : logEntries) {
                if (entry.getTimestamp() >= startTime &&
                        entry.getTimestamp() <= endTime &&
                        entry.getNodeId().equals(nodeId)) {
                    results.add(entry);
                }
            }
        }
        return results;
    }

    @Override
    public List<InternalLogEntry> getAllLogs() {
        synchronized (logEntries) {
            return new ArrayList<>(logEntries);
        }
    }
}