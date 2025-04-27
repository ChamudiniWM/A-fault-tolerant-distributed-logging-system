package com.group6.logsystem.tests;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.faulttolerance.FailoverManager;
import com.group6.logsystem.faulttolerance.FailureDetector;
import com.group6.logsystem.faulttolerance.FaultToleranceManager;
import com.group6.logsystem.faulttolerance.LogRecoveryService;
import com.group6.logsystem.faulttolerance.LogRedundancyManager;
import com.group6.logsystem.faulttolerance.redundancy.ReplicationStrategy;
import com.group6.logsystem.models.InternalLogEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FaultToleranceManagerTest {
    private RaftNode raftNode;
    private FaultToleranceManager ftManager;
    private InternalLogEntry testLogEntry;
    
    @Mock
    private FailureDetector mockFailureDetector;
    
    @Mock
    private LogRedundancyManager mockRedundancyManager;
    
    @Mock
    private FailoverManager mockFailoverManager;
    
    @Mock
    private LogRecoveryService mockRecoveryService;
    
    private AutoCloseable closeable;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        raftNode = new RaftNode("node1", Arrays.asList("node2", "node3", "node4"));
        
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
        
        // Create fault tolerance manager with mocked components
        ftManager = new FaultToleranceManager(raftNode, ReplicationStrategy.HYBRID);
        ftManager.setFailureDetector(mockFailureDetector);
        ftManager.setRedundancyManager(mockRedundancyManager);
        ftManager.setFailoverManager(mockFailoverManager);
        ftManager.setRecoveryService(mockRecoveryService);
    }
    
    @AfterEach
    void cleanup() throws Exception {
        ftManager.shutdown();
        raftNode.shutdown();
        closeable.close();
    }

    @Test
    void testStart() {
        // Start the fault tolerance manager
        ftManager.start();
        
        // Verify components were started
        verify(mockFailureDetector).start();
        verify(mockFailoverManager).start();
        
        // If node is a follower, recovery should be triggered
        if (raftNode.getRole() == RaftNode.Role.FOLLOWER) {
            verify(mockRecoveryService).recoverLogs();
        }
    }

    @Test
    void testHandleNodeFailure() {
        // Handle node failure
        ftManager.handleNodeFailure("node2");
        
        // Verify node is marked as partitioned
        assertTrue(raftNode.isPartitioned("node2"), "Failed node should be marked as partitioned");
        
        // Verify failover manager was notified
        verify(mockFailoverManager).handleNodeFailure("node2");
    }

    @Test
    void testHandleNodeRecovery() {
        // Mark node as partitioned
        raftNode.addPartitionedPeer("node2");
        
        // Handle node recovery
        ftManager.handleNodeRecovery("node2");
        
        // Verify node is no longer marked as partitioned
        assertFalse(raftNode.isPartitioned("node2"), "Recovered node should not be marked as partitioned");
        
        // Verify failover manager was notified
        verify(mockFailoverManager).handleNodeRecovery("node2");
    }

    @Test
    void testSubmitLogAsLeader() throws Exception {
        // Make node1 the leader
        raftNode.becomeLeader();
        
        // Mock successful log submission
        when(mockFailoverManager.submitLog(any(InternalLogEntry.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // Mock successful hybrid redundancy
        when(mockRedundancyManager.implementHybridRedundancy(any(InternalLogEntry.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // Submit a log entry
        CompletableFuture<Boolean> future = ftManager.submitLog(testLogEntry);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertTrue(result, "Log submission as leader should succeed");
        
        // Verify components were used correctly
        verify(mockFailoverManager).submitLog(testLogEntry);
        verify(mockRedundancyManager).implementHybridRedundancy(testLogEntry);
    }

    @Test
    void testSubmitLogAsFollower() throws Exception {
        // Ensure node1 is a follower
        raftNode.becomeFollower(1);
        
        // Mock successful log submission
        when(mockFailoverManager.submitLog(any(InternalLogEntry.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
        
        // Submit a log entry
        CompletableFuture<Boolean> future = ftManager.submitLog(testLogEntry);
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertTrue(result, "Log submission as follower should succeed");
        
        // Verify components were used correctly
        verify(mockFailoverManager).submitLog(testLogEntry);
        verify(mockRedundancyManager, never()).implementHybridRedundancy(any(InternalLogEntry.class));
    }

    @Test
    void testRetrieveLogFromLocal() throws Exception {
        // Mock successful local log retrieval
        when(raftNode.queryLogs(anyString(), anyLong(), anyLong()))
            .thenReturn(Collections.singletonList(testLogEntry));
        
        // Retrieve a log entry
        CompletableFuture<InternalLogEntry> future = ftManager.retrieveLog(testLogEntry.getLogId());
        InternalLogEntry result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertNotNull(result, "Should retrieve the log");
        assertEquals(testLogEntry.getLogId(), result.getLogId(), "Retrieved log should have correct ID");
        
        // Verify redundancy manager was not used (log found locally)
        verify(mockRedundancyManager, never()).retrieveRedundantLog(anyString());
    }

    @Test
    void testRetrieveLogFromRedundantStorage() throws Exception {
        // Mock local log retrieval failure
        when(raftNode.queryLogs(anyString(), anyLong(), anyLong()))
            .thenReturn(Collections.emptyList());
        
        // Mock successful redundant log retrieval
        when(mockRedundancyManager.retrieveRedundantLog(anyString()))
            .thenReturn(CompletableFuture.completedFuture(testLogEntry));
        
        // Retrieve a log entry
        CompletableFuture<InternalLogEntry> future = ftManager.retrieveLog(testLogEntry.getLogId());
        InternalLogEntry result = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertNotNull(result, "Should retrieve the log from redundant storage");
        assertEquals(testLogEntry.getLogId(), result.getLogId(), "Retrieved log should have correct ID");
        
        // Verify redundancy manager was used
        verify(mockRedundancyManager).retrieveRedundantLog(testLogEntry.getLogId());
        
        // Verify log was stored locally
        verify(raftNode).appendLogEntry(testLogEntry);
    }

    @Test
    void testRecoverLogs() throws Exception {
        // Mock successful log recovery
        when(mockRecoveryService.recoverLogs())
            .thenReturn(CompletableFuture.completedFuture(10));
        
        // Recover logs
        CompletableFuture<Integer> future = ftManager.recoverLogs();
        Integer recoveredCount = future.get(5, TimeUnit.SECONDS);
        
        // Verify the result
        assertEquals(10, recoveredCount, "Should recover the expected number of logs");
        
        // Verify recovery service was used
        verify(mockRecoveryService).recoverLogs();
    }

    @Test
    void testShutdown() {
        // Start the manager
        ftManager.start();
        
        // Shutdown the manager
        ftManager.shutdown();
        
        // Verify components were shutdown
        verify(mockFailureDetector).shutdown();
        verify(mockRedundancyManager).shutdown();
        verify(mockFailoverManager).shutdown();
        verify(mockRecoveryService).shutdown();
    }
}