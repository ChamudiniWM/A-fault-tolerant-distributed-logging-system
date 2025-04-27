package com.group6.logsystem.tests;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.faulttolerance.LogClient;
import com.group6.logsystem.faulttolerance.LogRecoveryService;
import com.group6.logsystem.faulttolerance.LogRedundancyManager;
import com.group6.logsystem.models.InternalLogEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LogRecoveryTest {
    private RaftNode raftNode;
    private LogRecoveryService recoveryService;

    @Mock
    private LogClient mockLogClient;

    @Mock
    private LogRedundancyManager mockRedundancyManager;

    private List<InternalLogEntry> testLogs;
    private AutoCloseable closeable;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        raftNode = new RaftNode("node1", Arrays.asList("node2", "node3", "node4"));
        recoveryService = new LogRecoveryService(raftNode);

        testLogs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            testLogs.add(new InternalLogEntry(
                "log-" + i,
                i,
                "node2",
                "Test log " + i,
                System.currentTimeMillis() - (5 - i) * 1000,
                1
            ));
        }

        raftNode.start();
        raftNode.becomeFollower(1);
    }

    @AfterEach
    void cleanup() throws Exception {
        recoveryService.shutdown();
        raftNode.shutdown();
        closeable.close();
    }

    @Test
    void testRecoverLogsFromLeader() throws Exception {
        recoveryService.setLogClientFactory(peerId -> mockLogClient);
        when(mockLogClient.getLeader())
            .thenReturn(CompletableFuture.completedFuture("node2"));
        when(mockLogClient.getLogs(anyLong()))
            .thenReturn(CompletableFuture.completedFuture(testLogs));
        when(raftNode.applyLog(any(InternalLogEntry.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        CompletableFuture<Integer> future = recoveryService.recoverLogs();
        Integer recoveredCount = future.get(5, TimeUnit.SECONDS);

        assertEquals(testLogs.size(), recoveredCount, "Should recover all logs from leader");
        verify(mockLogClient).getLeader();
        verify(mockLogClient).getLogs(anyLong());
        for (InternalLogEntry log : testLogs) {
            verify(raftNode).applyLog(log);
        }
    }

    @Test
    void testRecoverLogsFromRedundantSources() throws Exception {
        recoveryService.setLogClientFactory(peerId -> mockLogClient);
        recoveryService.setRedundancyManagerFactory(node -> mockRedundancyManager);
        when(mockLogClient.getLeader())
            .thenReturn(CompletableFuture.completedFuture(null));
        for (int i = 0; i < testLogs.size(); i++) {
            when(mockRedundancyManager.retrieveRedundantLog("log-" + i))
                .thenReturn(CompletableFuture.completedFuture(testLogs.get(i)));
        }
        when(raftNode.applyLog(any(InternalLogEntry.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        CompletableFuture<Integer> future = recoveryService.recoverLogs();
        Integer recoveredCount = future.get(5, TimeUnit.SECONDS);

        assertEquals(testLogs.size(), recoveredCount, "Should recover all logs from redundant sources");
        verify(mockLogClient).getLeader();
        for (int i = 0; i < testLogs.size(); i++) {
            verify(mockRedundancyManager).retrieveRedundantLog("log-" + i);
            verify(raftNode).applyLog(testLogs.get(i));
        }
    }

    @Test
    void testRecoveryWithLeaderFailure() throws Exception {
        recoveryService.setLogClientFactory(peerId -> mockLogClient);
        recoveryService.setRedundancyManagerFactory(node -> mockRedundancyManager);
        when(mockLogClient.getLeader())
            .thenReturn(CompletableFuture.completedFuture("node2"));
        when(mockLogClient.getLogs(anyLong()))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Leader unavailable")));
        for (int i = 0; i < testLogs.size(); i++) {
            when(mockRedundancyManager.retrieveRedundantLog("log-" + i))
                .thenReturn(CompletableFuture.completedFuture(testLogs.get(i)));
        }
        when(raftNode.applyLog(any(InternalLogEntry.class)))
            .thenReturn(CompletableFuture.completedFuture(true));

        CompletableFuture<Integer> future = recoveryService.recoverLogs();
        Integer recoveredCount = future.get(5, TimeUnit.SECONDS);

        assertEquals(testLogs.size(), recoveredCount, "Should recover all logs from redundant sources after leader failure");
        verify(mockLogClient).getLeader();
        verify(mockLogClient).getLogs(anyLong());
        for (int i = 0; i < testLogs.size(); i++) {
            verify(mockRedundancyManager).retrieveRedundantLog("log-" + i);
            verify(raftNode).applyLog(testLogs.get(i));
        }
    }

    @Test
    void testNoAvailablePeers() throws Exception {
        recoveryService.setLogClientFactory(peerId -> mockLogClient);
        raftNode.addPartitionedPeer("node2");
        raftNode.addPartitionedPeer("node3");
        raftNode.addPartitionedPeer("node4");

        CompletableFuture<Integer> future = recoveryService.recoverLogs();
        Integer recoveredCount = future.get(5, TimeUnit.SECONDS);

        assertEquals(0, recoveredCount, "Should not recover any logs when no peers are available");
        verify(mockLogClient, never()).getLeader();
    }

    @Test
    void testPartialLogRecovery() throws Exception {
        recoveryService.setLogClientFactory(peerId -> mockLogClient);
        when(mockLogClient.getLeader())
            .thenReturn(CompletableFuture.completedFuture("node2"));
        when(mockLogClient.getLogs(anyLong()))
            .thenReturn(CompletableFuture.completedFuture(testLogs));
        when(raftNode.applyLog(any(InternalLogEntry.class)))
            .thenAnswer(invocation -> {
                InternalLogEntry log = invocation.getArgument(0);
                boolean success = log.getIndex() % 2 == 1;
                return CompletableFuture.completedFuture(success);
            });

        CompletableFuture<Integer> future = recoveryService.recoverLogs();
        Integer recoveredCount = future.get(5, TimeUnit.SECONDS);

        assertEquals(2, recoveredCount, "Should recover only logs that were successfully applied");
    }

    @Test
    void testLogRecoveryWithMissingLog() throws Exception {
        recoveryService.setLogClientFactory(peerId -> mockLogClient);
        recoveryService.setRedundancyManagerFactory(node -> mockRedundancyManager);
        when(mockLogClient.getLeader())
            .thenReturn(CompletableFuture.completedFuture(null));
        when(mockRedundancyManager.retrieveRedundantLog("log-0"))
            .thenReturn(CompletableFuture.failedFuture(new IOException("Log data not found")));

        CompletableFuture<Integer> future = recoveryService.recoverLogs();
        Integer recoveredCount = future.get(5, TimeUnit.SECONDS);

        assertEquals(0, recoveredCount, "No logs should be recovered when redundant source fails");
        verify(mockLogClient).getLeader();
        verify(mockRedundancyManager).retrieveRedundantLog("log-0");
        verify(raftNode, never()).applyLog(any(InternalLogEntry.class));
    }
}