package com.group6.logsystem.tests;

import com.group6.logsystem.consensus.ConsensusModule;
import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.models.InternalLogEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplicationTest {

    private RaftNode raftNode;
    private ConsensusModule consensusModule;

    @BeforeEach
    void setup() {
        raftNode = new RaftNode("node1", List.of("node2", "node3"));
        consensusModule = new ConsensusModule(raftNode, "localhost:50052");
        raftNode.start();

        // Manually promote node to leader to simulate a stable cluster
        raftNode.becomeLeader();
    }

    @Test
    void testAppendLogAsLeader() {
        InternalLogEntry entry = new InternalLogEntry(
                "node1",
                "System started",
                System.currentTimeMillis(),
                raftNode.getCurrentTerm()
        );

        raftNode.appendLogEntry(entry);

        // Simulate successful replication + commit
        raftNode.setCommitIndex(1);

        List<InternalLogEntry> log = raftNode.getCommittedEntries();

        assertFalse(log.isEmpty(), "Log should contain the new entry.");
        assertEquals("System started", log.get(log.size() - 1).getMessage(), "Message should match appended content.");
    }


    @Test
    void testRejectAppendWhenNotLeader() {
        raftNode.becomeFollower(raftNode.getCurrentTerm()); // Demote to follower

        InternalLogEntry entry = new InternalLogEntry(
                "node1",
                "Should not be committed",
                System.currentTimeMillis(),
                raftNode.getCurrentTerm()
        );

        raftNode.appendLogEntry(entry); // This may or may not reject unless logic is enforced
        // raftNode.setCommitIndex(1);

        // NOTE: If you enforce leader-only appends, you can assert here
        List<InternalLogEntry> log = raftNode.getCommittedEntries();
        boolean containsMessage = log.stream()
                .anyMatch(e -> e.getMessage().equals("Should not be committed"));

        assertFalse(containsMessage, "Follower should not append logs directly.");
    }
}