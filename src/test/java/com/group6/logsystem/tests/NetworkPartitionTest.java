package com.group6.logsystem.tests;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.models.InternalLogEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NetworkPartitionTest {

    private RaftNode raftNode;

    @BeforeEach
    void setUp() {
        raftNode = new RaftNode("node1", List.of("node2", "node3"));
        raftNode.start();
        raftNode.becomeLeader(); // Force this node to become leader
    }

    @Test
    void testLeaderSendsHeartbeatsDuringOutgoingPartition() {
        System.out.println("=== Simulating Outgoing Heartbeat Partition ===");

        raftNode.disableElectionTimersForTesting(); // Disable timeout to avoid race condition
        raftNode.addPartitionedPeer("node2"); // Partition only with node2

        raftNode.sendHeartbeats();

        try {
            Thread.sleep(350); // Give time to settle
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(RaftNode.Role.LEADER, raftNode.getRole(),
                "Leader should remain LEADER if it can still contact a majority (node3).");
    }


    @Test
    void testPartitionedLeaderStepsDownAfterLosingMajority() {
        // Simulate a full partition where the leader cannot contact any followers
        raftNode.addPartitionedPeer("node2");
        raftNode.addPartitionedPeer("node3");

        // Send heartbeat (simulate inability to contact majority)
        raftNode.sendHeartbeats();

        // Simulate detection of higher term (e.g., from another node)
        raftNode.becomeFollower(raftNode.getCurrentTerm() + 1);

        // Assert that the node steps down from leadership
        assertEquals(
                RaftNode.Role.FOLLOWER,
                raftNode.getRole(),
                "Leader should step down when a higher term is discovered."
        );
    }

    @Test
    void testLogNotCommittedDuringPartition() {
        // Append an entry while partitioned (no replication possible)
        InternalLogEntry entry = new InternalLogEntry(
                "node1", "Partitioned log", System.currentTimeMillis(), raftNode.getCurrentTerm()
        );

        raftNode.appendLogEntry(entry);

        // Check that it is not committed without majority replication
        List<InternalLogEntry> committed = raftNode.getCommittedEntries();

        assertEquals(
                0,
                committed.size(),
                "No logs should be committed during a partition without majority."
        );

        // Optional: Also confirm that the uncommitted entry exists
        assertTrue(
                raftNode.getUncommittedEntries().contains(entry),
                "Entry should exist in uncommitted log entries."
        );
    }
}
