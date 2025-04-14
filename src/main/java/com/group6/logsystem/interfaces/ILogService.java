package com.group6.logsystem.interfaces;

import com.group6.logsystem.models.InternalLogEntry;

import java.util.List;

public interface ILogService {
    void appendLog(InternalLogEntry entry);
    List<InternalLogEntry> queryLogs(String nodeId, long startTime, long endTime);
    List<InternalLogEntry> getAllLogs();
}