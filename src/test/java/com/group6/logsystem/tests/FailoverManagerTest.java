package com.group6.logsystem.tests;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.faulttolerance.FailoverManager;
import com.group6.logsystem.faulttolerance.LogClient;
import com.group6.logsystem.models.InternalLogEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FailoverManagerTest {
    private RaftNode raftNode;
    private FailoverManager failoverManager;
    private InternalLogEntry testLogEntry;
    
    @Mock
    private LogClient mockLogClient;
    
    private AutoCloseable closeable;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        raftNode = new RaftNode("node1", Arrays.asList("node2", "node3", "node4"));
        failoverManager = new FailoverManager(raftNode);
        
        // Create a test log entry
        testLogEntry = new InternalLogEntry(
            UUID.randomUUID().toString(),
            1,
            "node1",
            "Test log content",
            System.currentTimeMillis(),
            1
        );
        
        // Start the node
        raftNode.start();
    }
    
    @AfterEach
    void cleanup() throws Exception {
        failoverManager.shutdown();
        raftNode.shutdown();
        closeable.close();
    }

    @Test
    void testSubmitLogAsLeader() throws Exception {
        // Make node1 the leader
        raftNode.becomeLeader();
        
        // Submit a log entry
        CompletableFuture<Boolean> future = failoverManager.submitLog(testLogEntry);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertTrue(result, "Log submission as leader should succeed");
    }

    @Test
    void testSubmitLogAsFollower() throws Exception {
        // Ensure node1 is a follower
        raftNode.becomeFollower(1);
        
        // Set mock log client factory
        failoverManager.setLogClientFactory(peerId -> mockLogClient);
        
        // Mock leader discovery
        failoverManager.setCurrentLeader("node2");
        
        // Mock successful log submission to leader
        when(mockLogClient.storeLog(any(InternalLogEntry.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // Submit a log entry
        CompletableFuture<Boolean> future = failoverManager.submitLog(testLogEntry);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertTrue(result, "Log submission as follower should succeed");
        
        // Verify log was sent to leader
        verify(mockLogClient).storeLog(testLogEntry);
    }

    @Test
    void testHandleNodeFailure() throws Exception {
        // Make node1 the leader
        raftNode.becomeLeader();
        
        // Set the current leader
        failoverManager.setCurrentLeader("node2");
        
        // Handle node2 failure
        failoverManager.handleNodeFailure("node2");
        
        // Verify node2 is marked as partitioned
        assertTrue(raftNode.isPartitioned("node2"), "Failed node should be marked as partitioned");
        
        // Verify current leader is cleared if it was the failed node
        assertNull(failoverManager.getCurrentLeader(), "Current leader should be cleared if it failed");
    }

    @Test
    void testHandleNodeRecovery() throws Exception {
        // Make node1 the leader
        raftNode.becomeLeader();
        
        // Mark node2 as partitioned
        raftNode.addPartitionedPeer("node2");
        
        // Handle node2 recovery
        failoverManager.handleNodeRecovery("node2");
        
        // Verify node2 is no longer marked as partitioned
        assertFalse(raftNode.isPartitioned("node2"), "Recovered node should not be marked as partitioned");
    }

    @Test
    void testLeaderDiscovery() throws Exception {
        // Ensure node1 is a follower
        raftNode.becomeFollower(1);
        
        // Set mock log client factory
        failoverManager.setLogClientFactory(peerId -> mockLogClient);
        
        // Mock successful leader discovery
        when(mockLogClient.getLeader())
            .thenReturn(CompletableFuture.completedFuture("node3"));
        
        // Start the failover manager to trigger leader discovery
        failoverManager.start();
        
        // Wait for leader discovery
        TimeUnit.SECONDS.sleep(1);
        
        // Verify the discovered leader
        assertEquals("node3", failoverManager.getCurrentLeader(), "Should discover the correct leader");
    }

    @Test
    void testProcessPendingLogs() throws Exception {
        // Ensure node1 is a follower
        raftNode.becomeFollower(1);
        
        // Set mock log client factory
        failoverManager.setLogClientFactory(peerId -> mockLogClient);
        
        // Initially no leader
        failoverManager.setCurrentLeader(null);
        
        // Submit a log entry (should be queued)
        CompletableFuture<Boolean> future = failoverManager.submitLog(testLogEntry);
        
        // Now set a leader
        failoverManager.setCurrentLeader("node2");
        
        // Mock successful log submission to leader
        when(mockLogClient.storeLog(any(InternalLogEntry.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // Trigger processing of pending logs
        failoverManager.processPendingLogs();
        
        // Wait for the future to complete
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertTrue(result, "Queued log submission should succeed after leader is available");
        
        // Verify log was sent to leader
        verify(mockLogClient).storeLog(testLogEntry);
    }

    @Test
    void testLeaderFailureAndRediscovery() throws Exception {
        // Ensure node1 is a follower
        raftNode.becomeFollower(1);
        
        // Set mock log client factory
        failoverManager.setLogClientFactory(peerId -> mockLogClient);
        
        // Set initial leader
        failoverManager.setCurrentLeader("node2");
        
        // Mock leader failure
        when(mockLogClient.storeLog(any(InternalLogEntry.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Leader unavailable")));
        
        // Then mock successful leader discovery of a new leader
        when(mockLogClient.getLeader())
            .thenReturn(CompletableFuture.completedFuture("node3"));
        
        // Submit a log entry
        failoverManager.submitLog(testLogEntry);
        
        // Wait for leader rediscovery
        TimeUnit.SECONDS.sleep(1);
        
        // Verify the new leader
        assertEquals("node3", failoverManager.getCurrentLeader(), "Should discover a new leader after failure");
    }
}