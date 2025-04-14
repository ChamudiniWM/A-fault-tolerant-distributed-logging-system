package com.group6.logsystem.tests;

import com.group6.logsystem.consensus.ConsensusModule;
import com.group6.logsystem.consensus.RaftNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsensusModuleTest {

    private RaftNode raftNode;
    private ConsensusModule consensusModule;

    @BeforeEach
    void setUp() {
        raftNode = new RaftNode("node1", List.of("node2", "node3"));
        consensusModule = new ConsensusModule(raftNode, "localhost:50052");
        raftNode.start(); // Start background tasks
    }

    @Test
    void testLeaderElection() {
        // Simulate election timeout and becoming candidate
        consensusModule.onElectionTimeout();
        assertEquals(RaftNode.Role.CANDIDATE, raftNode.getRole(), "Node should become candidate");

        // Simulate receiving majority votes and becoming leader
        consensusModule.acknowledgeVote("node2", raftNode.getCurrentTerm());
        consensusModule.acknowledgeVote("node3", raftNode.getCurrentTerm());

        assertEquals(RaftNode.Role.LEADER, raftNode.getRole(), "Node should become leader after receiving majority votes");
    }

    @Test
    void testLeaderCrashAndReElection() {
        // Simulate leader crash and demotion
        consensusModule.updateLeaderStatus(false);
        assertEquals(RaftNode.Role.FOLLOWER, raftNode.getRole(), "Node should become follower after crash");

        // Simulate new election
        consensusModule.onElectionTimeout();
        assertEquals(RaftNode.Role.CANDIDATE, raftNode.getRole(), "Node should become candidate again");

        // Simulate vote grant
        consensusModule.acknowledgeVote("node2", raftNode.getCurrentTerm());
        consensusModule.acknowledgeVote("node3", raftNode.getCurrentTerm());

        assertEquals(RaftNode.Role.LEADER, raftNode.getRole(), "Node should be re-elected as leader");
    }

    @Test
    void testGetNodeStatus() {
        String status = consensusModule.getNodeStatus();
        assertTrue(status.contains("Node ID"), "Status should include node ID");
        assertTrue(status.contains("Role"), "Status should include node role");
    }

    @Test
    void testForceFailoverToFollower() {
        raftNode.becomeLeader(); // Manually become leader
        consensusModule.handleFailover();
        assertEquals(RaftNode.Role.FOLLOWER, raftNode.getRole(), "Node should revert to follower on failover");
    }
}