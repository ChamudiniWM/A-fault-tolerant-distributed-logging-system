//package com.group6.logsystem.tests;
//
//import com.group6.logsystem.consensus.RaftNode;
//import com.group6.logsystem.models.InternalLogEntry;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class ConflictResolutionTest {
//
//    private RaftNode staleLeader;
//    private RaftNode newLeader;
//
//    @BeforeEach
//    void setUp() {
//        // Simulate two nodes: staleLeader and newLeader
//        staleLeader = new RaftNode("node1", List.of("node2"));
//        newLeader = new RaftNode("node2", List.of("node1"));
//
//        // Start both nodes
//        staleLeader.start();
//        newLeader.start();
//
//        // Force node1 to become leader with stale log
//        staleLeader.becomeLeader();
//        InternalLogEntry staleEntry = new InternalLogEntry(
//                staleLeader.getCurrentTerm(),  // term
//                "node1",                        // nodeId
//                "old_log_entry",                // message
//                System.currentTimeMillis(),     // timestamp
//                1                               // index
//        );
//
//        staleLeader.appendLogEntry(staleEntry);
//
//        // Simulate partition (no replication to node2)
//        // ...
//
//        // Now, node2 becomes leader with a newer term and newer log
//        newLeader.becomeFollower(2); // Update term
//        newLeader.becomeCandidate();
//        newLeader.becomeLeader();
//        InternalLogEntry correctEntry = new InternalLogEntry(
//                newLeader.getCurrentTerm(),    // term
//                "node2",                       // nodeId
//                "correct_log_entry",          // message
//                System.currentTimeMillis(),   // timestamp
//                2                             // index
//        );
//
//        newLeader.appendLogEntry(correctEntry);
//    }
//
//    @Test
//    void testStaleLeaderStepsDownOnHigherTerm() {
//        staleLeader.becomeFollower(3); // Simulate learning of newer term from new leader
//        assertEquals(RaftNode.Role.FOLLOWER, staleLeader.getRole(), "Stale leader should step down.");
//    }
//
//    @Test
//    void testStaleLogsDoNotGetCommitted() {
//        List<InternalLogEntry> committed = staleLeader.getCommittedEntries();
//        assertEquals(0, committed.size(), "Stale leader should not commit old logs.");
//    }
//
//    @Test
//    void testCorrectLogsAreReplicated() {
//        List<InternalLogEntry> newLogs = newLeader.getCommittedEntries();
//        // In actual Raft, replication would confirm before commit. We're simplifying here.
//        newLeader.setCommitIndex(1); // Simulate successful replication and commit
//
//        newLogs = newLeader.getCommittedEntries();
//        assertEquals(1, newLogs.size(), "New leader should have one committed log.");
//        assertEquals("correct_log_entry", newLogs.get(0).message(), "Committed log should be from new leader.");
//    }
//}