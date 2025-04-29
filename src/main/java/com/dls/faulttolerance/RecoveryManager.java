package com.dls.faulttolerance;

import com.dls.common.LocalLogEntry;
import com.dls.common.NodeInfo;
import com.dls.raft.RaftNode;
import com.dls.raft.RaftState;
import com.dls.raft.rpc.RaftGrpc;
import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;
import com.dls.raft.rpc.LogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RecoveryManager {
    private final RaftNode raftNode;
    private final ConcurrentHashMap<String, List<LocalLogEntry>> recoveryLogs;
    private final ConcurrentHashMap<String, Integer> lastKnownIndices;
    private final ConcurrentHashMap<String, AtomicLong> recoveryStartTime;
    private final ConcurrentHashMap<String, AtomicInteger> recoveryAttempts;
    private static final int MAX_RECOVERY_ATTEMPTS = 3;
    private static final long RECOVERY_TIMEOUT = 30000; // 30 seconds

    public RecoveryManager(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.recoveryLogs = new ConcurrentHashMap<>();
        this.lastKnownIndices = new ConcurrentHashMap<>();
        this.recoveryStartTime = new ConcurrentHashMap<>();
        this.recoveryAttempts = new ConcurrentHashMap<>();
    }

    public void startRecovery(NodeInfo recoveringNode) {
        if (raftNode.getState() != RaftState.LEADER) {
            System.out.println("Cannot start recovery: not the leader");
            return;
        }

        if (isRecovering(recoveringNode.getNodeId())) {
            System.out.println("Recovery already in progress for node " + recoveringNode.getNodeId());
            return;
        }

        // Initialize recovery metrics
        recoveryStartTime.put(recoveringNode.getNodeId(), new AtomicLong(System.currentTimeMillis()));
        recoveryAttempts.put(recoveringNode.getNodeId(), new AtomicInteger(0));

        System.out.println("Starting recovery for node " + recoveringNode.getNodeId());

        // Get the missing logs
        List<LocalLogEntry> missingLogs = getMissingLogs(recoveringNode);
        if (missingLogs.isEmpty()) {
            System.out.println("No missing logs found for node " + recoveringNode.getNodeId());
            return;
        }

        recoveryLogs.put(recoveringNode.getNodeId(), missingLogs);
        sendRecoveryLogs(recoveringNode, missingLogs);
    }

    private List<LocalLogEntry> getMissingLogs(NodeInfo node) {
        int lastKnownIndex = lastKnownIndices.getOrDefault(node.getNodeId(), -1);
        List<LogEntry> currentLogs = raftNode.getRaftLog().getLogEntries();
        List<LocalLogEntry> missingLogs = new ArrayList<>();

        for (int i = lastKnownIndex + 1; i < currentLogs.size(); i++) {
            LogEntry entry = currentLogs.get(i);
            missingLogs.add(LocalLogEntry.fromGrpcLogEntry(entry));
        }

        return missingLogs;
    }

    private void sendRecoveryLogs(NodeInfo recoveringNode, List<LocalLogEntry> logs) {
        if (logs.isEmpty()) {
            return;
        }

        RaftGrpc.RaftStub stub = raftNode.getPeerStubs().get(recoveringNode);
        if (stub == null) {
            System.out.println("No stub found for recovering node " + recoveringNode.getNodeId());
            return;
        }

        // Check if we've exceeded recovery timeout
        long startTime = recoveryStartTime.get(recoveringNode.getNodeId()).get();
        if (System.currentTimeMillis() - startTime > RECOVERY_TIMEOUT) {
            System.out.println("Recovery timeout for node " + recoveringNode.getNodeId());
            cancelRecovery(recoveringNode);
            return;
        }

        // Check if we've exceeded max recovery attempts
        int attempts = recoveryAttempts.get(recoveringNode.getNodeId()).incrementAndGet();
        if (attempts > MAX_RECOVERY_ATTEMPTS) {
            System.out.println("Max recovery attempts reached for node " + recoveringNode.getNodeId());
            cancelRecovery(recoveringNode);
            return;
        }

        List<LogEntry> entries = new ArrayList<>();
        for (LocalLogEntry entry : logs) {
            entries.add(entry.toGrpcLogEntry());
        }

        AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setLeaderId(raftNode.getSelf().getNodeId())
                .setPrevLogIndex(logs.get(0).getIndex() - 1)
                .setPrevLogTerm(logs.get(0).getTerm())
                .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                .addAllEntries(entries)
                .build();

        stub.appendEntries(request, new io.grpc.stub.StreamObserver<>() {
            @Override
            public void onNext(AppendEntriesResponse response) {
                if (response.getSuccess()) {
                    System.out.println("Successfully sent recovery logs to " + recoveringNode.getNodeId());
                    // Update last known index
                    lastKnownIndices.put(recoveringNode.getNodeId(),
                            getLastKnownIndex(recoveringNode) + logs.size());

                    // Clear recovery metrics
                    recoveryLogs.remove(recoveringNode.getNodeId());
                    recoveryStartTime.remove(recoveringNode.getNodeId());
                    recoveryAttempts.remove(recoveringNode.getNodeId());
                } else {
                    System.out.println("Failed to send recovery logs to " + recoveringNode.getNodeId());
                    if (response.getTerm() > raftNode.getCurrentTerm()) {
                        // Step down if we discover a higher term
                        raftNode.becomeFollower(response.getTerm());
                    } else {
                        // Retry recovery
                        sendRecoveryLogs(recoveringNode, logs);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error sending recovery logs to " + recoveringNode.getNodeId() +
                        ": " + t.getMessage());
                // Retry recovery
                sendRecoveryLogs(recoveringNode, logs);
            }

            @Override
            public void onCompleted() {
                // Recovery completed
            }
        });
    }

    public void cancelRecovery(NodeInfo node) {
        recoveryLogs.remove(node.getNodeId());
        lastKnownIndices.remove(node.getNodeId());
        recoveryStartTime.remove(node.getNodeId());
        recoveryAttempts.remove(node.getNodeId());
        System.out.println("Recovery cancelled for node " + node.getNodeId());
    }

    public boolean isRecovering(String nodeId) {
        return recoveryLogs.containsKey(nodeId);
    }

    public List<LocalLogEntry> getRecoveryLogs(String nodeId) {
        return recoveryLogs.get(nodeId);
    }

    private int getLastKnownIndex(NodeInfo node) {
        return lastKnownIndices.getOrDefault(node.getNodeId(), -1);
    }

    public long getRecoveryStartTime(String nodeId) {
        AtomicLong time = recoveryStartTime.get(nodeId);
        return time != null ? time.get() : 0;
    }

    public int getRecoveryAttempts(String nodeId) {
        AtomicInteger attempts = recoveryAttempts.get(nodeId);
        return attempts != null ? attempts.get() : 0;
    }
}
