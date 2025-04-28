//package com.group6.logsystem.tests;
//
//import com.group6.logsystem.consensus.ConsensusModule;
//import com.group6.logsystem.consensus.RaftNode;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class LeaderElectionTest {
//
//    private RaftNode raftNode;
//    private ConsensusModule consensusModule;
//
//    @BeforeEach
//    void setup() {
//        raftNode = new RaftNode("node1", List.of("node2", "node3"));
//        consensusModule = new ConsensusModule(raftNode, "localhost:50052");
//        raftNode.start(); // Start background threads like election timer
//    }
//
//    @Test
//    void testBecomeCandidateOnTimeout() {
//        consensusModule.onElectionTimeout();
//        assertEquals(RaftNode.Role.CANDIDATE, raftNode.getRole(), "Node should become CANDIDATE on election timeout.");
//    }
//
//    @Test
//    void testBecomeLeaderAfterVotes() {
//        consensusModule.onElectionTimeout(); // Become candidate
//
//        // Simulate receiving votes from peers
//        consensusModule.acknowledgeVote("node2", raftNode.getCurrentTerm());
//        consensusModule.acknowledgeVote("node3", raftNode.getCurrentTerm());
//
//        assertEquals(RaftNode.Role.LEADER, raftNode.getRole(), "Node should become LEADER after majority vote.");
//    }
//
//    @Test
//    void testDoesNotBecomeLeaderWithoutMajority() {
//        consensusModule.onElectionTimeout(); // Become candidate
//
//        // Only one vote received, not enough for majority
//        consensusModule.acknowledgeVote("node2", raftNode.getCurrentTerm());
//
//        assertNotEquals(RaftNode.Role.LEADER, raftNode.getRole(), "Node should not become LEADER without majority.");
//    }
//}