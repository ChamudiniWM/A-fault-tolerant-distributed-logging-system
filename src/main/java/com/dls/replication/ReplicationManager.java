package com.dls.replication;

import com.dls.common.LocalLogEntry;
import com.dls.common.NodeInfo;
import com.dls.raft.RaftNode;
import com.dls.raft.RaftState;
import com.dls.raft.rpc.RaftGrpc;
import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;
import com.dls.raft.rpc.LogEntry;
import com.dls.timesync.TimestampCorrector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

public class ReplicationManager {
    private final RaftNode raftNode;
    private final LogRetriever logRetriever;
    private final int quorumSize;
    private final ConcurrentHashMap<String, CompletableFuture<AppendEntriesResponse>> pendingReplications;
    private final ConcurrentHashMap<String, LocalLogEntry> deduplicationCache;
    private final ConcurrentHashMap<String, Integer> nextIndex; // Track next index for each follower

    public ReplicationManager(RaftNode raftNode, LogRetriever logRetriever) {
        this.raftNode = raftNode;
        this.logRetriever = logRetriever;
        this.quorumSize = (raftNode.getPeers().size() / 2) + 1;
        this.pendingReplications = new ConcurrentHashMap<>();
        this.deduplicationCache = new ConcurrentHashMap<>();
        this.nextIndex = new ConcurrentHashMap<>();

        // Initialize nextIndex for each peer
        for (NodeInfo peer : raftNode.getPeers()) {
            if (!peer.getNodeId().equals(raftNode.getSelf().getNodeId())) {
                nextIndex.put(peer.getNodeId(), 0); // Start from beginning
            }
        }
    }

    public boolean replicateLogEntry(LocalLogEntry entry) {
        // Check for duplicates
        if (isDuplicate(entry)) {
            System.out.println("Duplicate log entry detected: " + entry.getId());
            return true;
        }

        if (!raftNode.getState().equals(RaftState.LEADER)) {
            return false;
        }

        synchronized (raftNode.getRaftLog()) {
            System.out.println("Appending to leader log: " + entry.getId());
            raftNode.getRaftLog().appendEntry(entry, entry.getTerm());
        }

        // Convert to gRPC LogEntry
        LogEntry grpcEntry = entry.toGrpcLogEntry();

        // Track successful replications
        AtomicInteger successCount = new AtomicInteger(0);
        String requestId = entry.getId() + "-" + System.currentTimeMillis();
        List<CompletableFuture<AppendEntriesResponse>> futures = new ArrayList<>();

        // Send to all followers
        for (NodeInfo peer : raftNode.getPeers()) {
            if (peer.getNodeId().equals(raftNode.getSelf().getNodeId())) {
                continue;
            }

            RaftGrpc.RaftStub stub = raftNode.getPeerStubs().get(peer);
            if (stub == null) {
                continue;
            }

            // Get the next index for this peer
            int peerNextIndex = nextIndex.getOrDefault(peer.getNodeId(), 0);

            // Create AppendEntries request with appropriate prevLogIndex
            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setLeaderId(raftNode.getSelf().getNodeId())
                    .setPrevLogIndex(peerNextIndex - 1) // Previous index
                    .setPrevLogTerm(peerNextIndex > 0 ?
                            raftNode.getRaftLog().getLogEntries().get(peerNextIndex - 1).getTerm() : 0)
                    .addEntries(grpcEntry)
                    .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                    .build();

            CompletableFuture<AppendEntriesResponse> future = new CompletableFuture<>();
            pendingReplications.put(requestId, future);
            futures.add(future);

            stub.appendEntries(request, new io.grpc.stub.StreamObserver<>() {
                @Override
                public void onNext(AppendEntriesResponse response) {
                    if (response.getSuccess()) {
                        successCount.incrementAndGet();
                        // Update nextIndex for successful replication
                        nextIndex.put(peer.getNodeId(), peerNextIndex + 1);
                    } else {
                        // Decrement nextIndex on failure
                        nextIndex.put(peer.getNodeId(), Math.max(0, peerNextIndex - 1));
                    }
                    future.complete(response);
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Error replicating to " + peer.getNodeId() + ": " + t.getMessage());
                    // Decrement nextIndex on error
                    nextIndex.put(peer.getNodeId(), Math.max(0, peerNextIndex - 1));
                    future.completeExceptionally(t);
                }

                @Override
                public void onCompleted() {
                    pendingReplications.remove(requestId);
                }
            });
        }

        // Wait for all responses with timeout
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Error waiting for replication responses: " + e.getMessage());
            return false;
        }

        // Check if quorum was reached
        boolean success = successCount.get() >= quorumSize;
        if (success) {
            deduplicationCache.put(entry.getId(), entry);
        }
        return success;
    }

    private boolean isDuplicate(LocalLogEntry entry) {
        return deduplicationCache.containsKey(entry.getId());
    }

    public List<LocalLogEntry> getLogs(int startIndex, int endIndex) {
        List<LogEntry> grpcLogs = raftNode.getRaftLog().getLogEntries();
        List<LocalLogEntry> logs = new ArrayList<>();

        for (int i = startIndex; i <= endIndex && i < grpcLogs.size(); i++) {
            LogEntry grpcEntry = grpcLogs.get(i);
            if (grpcEntry != null) {
                logs.add(LocalLogEntry.fromGrpcLogEntry(grpcEntry));
            }
        }

        return logs;
    }

    public void cleanupDeduplicationCache() {
        // Remove entries older than 1 hour
        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        deduplicationCache.entrySet().removeIf(entry ->
                entry.getValue().getTimestamp() < oneHourAgo
        );
    }

    public int getQuorumSize() {
        return quorumSize;
    }

    public boolean isQuorumReached(int successCount) {
        return successCount >= quorumSize;
    }
}