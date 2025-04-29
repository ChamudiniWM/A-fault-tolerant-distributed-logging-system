package com.dls.faulttolerance;

import com.dls.common.LocalLogEntry;
import com.dls.common.NodeInfo;
import com.dls.raft.RaftNode;
import com.dls.raft.RaftState;
import com.dls.raft.rpc.RaftGrpc;
import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;
import com.dls.raft.rpc.LogEntry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RedundancyManager {
    private final RaftNode raftNode;
    private final int quorumSize;
    private final ConcurrentHashMap<String, CompletableFuture<AppendEntriesResponse>> pendingReplications;

    public RedundancyManager(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.quorumSize = (raftNode.getPeers().size() / 2) + 1; // Simple majority quorum
        this.pendingReplications = new ConcurrentHashMap<>();
    }

    public boolean replicateLogEntry(LocalLogEntry entry) {
        if (!raftNode.getState().equals(RaftState.LEADER)) {
            return false;
        }

        synchronized (raftNode.getRaftLog()) {
            System.out.println("Appending to leader log: " + entry.getId());
            raftNode.getRaftLog().appendEntry(entry, entry.getTerm()); // Use appendEntry
        }

        // Convert to gRPC LogEntry
        LogEntry grpcEntry = entry.toGrpcLogEntry();

        // Create AppendEntries request
        AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setLeaderId(raftNode.getSelf().getNodeId())
                .setPrevLogIndex(raftNode.getRaftLog().getLastLogIndex())
                .setPrevLogTerm(raftNode.getRaftLog().getLastLogTerm())
                .addEntries(grpcEntry)
                .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                .build();

        // Track successful replications
        AtomicInteger successCount = new AtomicInteger(0);
        String requestId = entry.getId() + "-" + System.currentTimeMillis();

        // Send to all followers
        for (NodeInfo peer : raftNode.getPeers()) {
            if (peer.getNodeId().equals(raftNode.getSelf().getNodeId())) {
                continue;
            }

            RaftGrpc.RaftStub stub = raftNode.getPeerStubs().get(peer);
            if (stub == null) {
                continue;
            }

            CompletableFuture<AppendEntriesResponse> future = new CompletableFuture<>();
            pendingReplications.put(requestId, future);

            stub.appendEntries(request, new io.grpc.stub.StreamObserver<>() {
                @Override
                public void onNext(AppendEntriesResponse response) {
                    if (response.getSuccess()) {
                        successCount.incrementAndGet();
                    }
                    future.complete(response);
                }

                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void onCompleted() {
                    // Clean up
                    pendingReplications.remove(requestId);
                }
            });
        }

        // Wait for quorum
        return successCount.get() >= quorumSize;
    }

    public void handleNodeFailure(NodeInfo failedNode) {
        // Remove the failed node's stub
        raftNode.getPeerStubs().remove(failedNode);

        // Log the failure
        System.out.println("Node " + failedNode.getNodeId() + " has failed. Removed from replication targets.");
    }

    public void handleNodeRecovery(NodeInfo recoveredNode) {
        // Recreate the stub for the recovered node
        ManagedChannel channel = ManagedChannelBuilder.forAddress(recoveredNode.getHost(), recoveredNode.getPort())
                .usePlaintext()
                .build();
        RaftGrpc.RaftStub stub = RaftGrpc.newStub(channel);
        raftNode.getPeerStubs().put(recoveredNode, stub);

        // Log the recovery
        System.out.println("Node " + recoveredNode.getNodeId() + " has recovered. Re-added to replication targets.");
    }
}