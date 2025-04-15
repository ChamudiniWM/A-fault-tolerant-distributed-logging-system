package com.group6.logsystem.tests;

import com.group6.logsystem.consensus.ConsensusModule;
import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.grpc.LogServiceImplAdapter;
import com.group6.logsystem.models.InternalLogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LogRetrievalTest {

    private RaftNode raftNode;
    private ConsensusModule consensusModule;
    private LogServiceImplAdapter adapter;

    @BeforeEach
    void setUp() {
        raftNode = new RaftNode("node1", List.of("node2", "node3"));
        raftNode.start();
        raftNode.becomeLeader();
        consensusModule = new ConsensusModule(raftNode, "localhost:50051");
        adapter = new LogServiceImplAdapter(raftNode, consensusModule);
    }

    @Test
    void testOptimizedLogRetrieval() {
        System.out.println("Running testOptimizedLogRetrieval - VERSION 2025-04-15");
        long baseTime = System.currentTimeMillis();
        raftNode.appendLogEntry(new InternalLogEntry(UUID.randomUUID().toString(), 0, "node1", "Log 1", baseTime, 1));
        raftNode.appendLogEntry(new InternalLogEntry(UUID.randomUUID().toString(), 1, "node1", "Log 2", baseTime + 1000, 1));
        raftNode.appendLogEntry(new InternalLogEntry(UUID.randomUUID().toString(), 2, "node2", "Log 3", baseTime + 2000, 1));
        raftNode.setCommitIndex(3); // Commit all logs

        long startTime = baseTime - 1000; // Ensure startTime is before baseTime
        long endTime = baseTime + 1500; // Changed to baseTime + 1500 to include Log 2

        System.out.println("Query range: [" + startTime + ", " + endTime + "]");

        // Query logs for node1 within the time range
        List<InternalLogEntry> results = adapter.queryLogs("node1", startTime, endTime);
        System.out.println("Test results for node1: " + results);
        assertEquals(2, results.size()); // Should return Log 1 and Log 2
        assertEquals("Log 1", results.get(0).getMessage());
        assertEquals("Log 2", results.get(1).getMessage());

        // Query logs for node2
        results = adapter.queryLogs("node2", startTime, endTime + 1000);
        System.out.println("Test results for node2: " + results);
        assertEquals(1, results.size()); // Should return Log 3
        assertEquals("Log 3", results.get(0).getMessage());
    }
}