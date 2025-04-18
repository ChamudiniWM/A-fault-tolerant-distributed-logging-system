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

        // Explicitly set the currentTerm to 0
        raftNode.setCurrentTerm(0);

        raftNode.start();
        raftNode.becomeLeader(); // Promote to leader after setting the term
    }

    @Test
    void testAppendLogAsLeader() {
        // Capture current term immediately after promotion
        raftNode.becomeLeader(); // <- Make sure term is updated here
        int currentTerm = raftNode.getCurrentTerm(); // <- Capture right after promotion

        InternalLogEntry entry = new InternalLogEntry(
                currentTerm,                 // Term
                "node1",                     // Node ID
                "System started",            // Message
                System.currentTimeMillis(),  // Timestamp
                raftNode.getLastLogIndex() + 1 // Index, incremented from the last log index
        );

        raftNode.appendLogEntry(entry);
        raftNode.setCommitIndex(1);

        List<InternalLogEntry> log = raftNode.getCommittedEntries();

        System.out.println("Expected Term: " + currentTerm);
        System.out.println("Actual Term: " + log.get(log.size() - 1).getTerm());

        assertFalse(log.isEmpty(), "Log should contain the new entry.");
        assertEquals("System started", log.get(log.size() - 1).getMessage());
        assertEquals(currentTerm, log.get(log.size() - 1).getTerm(),
                "Log entry term should match the leader's current term.");
    }

    @Test
    void testRejectAppendWhenNotLeader() {
        raftNode.becomeFollower(raftNode.getCurrentTerm());

        InternalLogEntry entry = new InternalLogEntry(
                raftNode.getCurrentTerm(),
                "node1",
                "Should not be committed",
                System.currentTimeMillis(),
                1
        );

        raftNode.appendLogEntry(entry);

        List<InternalLogEntry> log = raftNode.getCommittedEntries();
        boolean containsMessage = log.stream()
                .anyMatch(e -> e.getMessage().equals("Should not be committed"));

        assertFalse(containsMessage, "Follower should not append logs directly.");
    }
}
