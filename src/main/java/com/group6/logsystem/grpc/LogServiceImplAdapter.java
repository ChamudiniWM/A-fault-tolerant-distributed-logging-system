package com.group6.logsystem.grpc;

import com.group6.logsystem.consensus.ConsensusModule;
import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.interfaces.ILogService;
import com.group6.logsystem.models.InternalLogEntry;
import java.util.ArrayList;
import java.util.List;

public class LogServiceImplAdapter implements ILogService {

    private final RaftNode raftNode;
    private final ConsensusModule consensusModule;

    // Updated constructor to take both RaftNode and ConsensusModule
    public LogServiceImplAdapter(RaftNode raftNode, ConsensusModule consensusModule) {
        this.raftNode = raftNode;
        this.consensusModule = consensusModule;
    }

    @Override
    public void appendLog(InternalLogEntry entry) {
        // Delegate log request to ConsensusModule
        consensusModule.handleClientLogRequest(entry);
    }

    @Override
    public List<InternalLogEntry> queryLogs(String nodeId, long startTime, long endTime) {
        List<InternalLogEntry> results = new ArrayList<>();
        for (InternalLogEntry entry : raftNode.getCommittedEntries()) {
            if (entry.getTimestamp() >= startTime &&
                    entry.getTimestamp() <= endTime &&
                    entry.getNodeId().equals(nodeId)) {
                results.add(entry);
            }
        }
        return results;
    }

    @Override
    public List<InternalLogEntry> getAllLogs() {
        return raftNode.getCommittedEntries();
    }
}
