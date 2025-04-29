package com.dls.faulttolerance;

import com.dls.common.NodeInfo;
import com.dls.raft.RaftNode;
import com.dls.raft.RaftState;
import com.dls.raft.rpc.RaftGrpc;
import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FailoverManager {
    private final RaftNode raftNode;
    private final ConcurrentHashMap<String, NodeInfo> failedNodes;
    private final ConcurrentHashMap<String, NodeInfo> backupNodes;
    private final ConcurrentHashMap<String, AtomicLong> lastFailureTime;
    private final ConcurrentHashMap<String, AtomicInteger> failureCount;

    public FailoverManager(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.failedNodes = new ConcurrentHashMap<>();
        this.backupNodes = new ConcurrentHashMap<>();
        this.lastFailureTime = new ConcurrentHashMap<>();
        this.failureCount = new ConcurrentHashMap<>();
    }

    public void handleNodeFailure(NodeInfo failedNode) {
        // Add to failed nodes
        failedNodes.put(failedNode.getNodeId(), failedNode);

        // Update failure metrics
        lastFailureTime.put(failedNode.getNodeId(), new AtomicLong(System.currentTimeMillis()));
        failureCount.putIfAbsent(failedNode.getNodeId(), new AtomicInteger(0));
        failureCount.get(failedNode.getNodeId()).incrementAndGet();

        System.out.println("Node " + failedNode.getNodeId() + " marked as failed. " +
                "Total failures: " + failureCount.get(failedNode.getNodeId()).get());

        // If we're the leader, initiate failover
        if (raftNode.getState().equals(RaftState.LEADER)) {
            initiateFailover(failedNode);
        }
    }

    private void initiateFailover(NodeInfo failedNode) {
        // Find a backup node to take over
        NodeInfo backupNode = findBackupNode(failedNode);
        if (backupNode != null) {
            // Transfer leadership to backup node
            transferLeadership(backupNode);

            // Update backup node status
            backupNodes.put(failedNode.getNodeId(), backupNode);
            System.out.println("Failover initiated: " + failedNode.getNodeId() +
                    " -> " + backupNode.getNodeId());
        } else {
            System.out.println("No suitable backup node found for " + failedNode.getNodeId());
        }
    }

    private NodeInfo findBackupNode(NodeInfo failedNode) {
        // Find the healthiest available node that's not failed
        NodeInfo bestBackup = null;
        long bestResponseTime = Long.MAX_VALUE;

        for (NodeInfo node : raftNode.getPeers()) {
            if (!node.getNodeId().equals(raftNode.getSelf().getNodeId()) &&
                    !failedNodes.containsKey(node.getNodeId())) {

                // Get response time from failure detector
                long responseTime = raftNode.getFailureDetector().getLastResponseTime(node);
                if (responseTime < bestResponseTime) {
                    bestResponseTime = responseTime;
                    bestBackup = node;
                }
            }
        }

        return bestBackup;
    }

    private void transferLeadership(NodeInfo newLeader) {
        // Create a leadership transfer request
        AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setLeaderId(raftNode.getSelf().getNodeId())
                .setPrevLogIndex(raftNode.getRaftLog().getLastLogIndex())
                .setPrevLogTerm(raftNode.getRaftLog().getLastLogTerm())
                .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                .build();

        // Send leadership transfer request
        RaftGrpc.RaftStub stub = raftNode.getPeerStubs().get(newLeader);
        if (stub != null) {
            stub.appendEntries(request, new io.grpc.stub.StreamObserver<>() {
                @Override
                public void onNext(AppendEntriesResponse response) {
                    if (response.getSuccess()) {
                        System.out.println("Leadership transfer initiated to " + newLeader.getNodeId());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("Failed to transfer leadership to " + newLeader.getNodeId() +
                            ": " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    // Leadership transfer completed
                }
            });
        }
    }

    public void handleNodeRecovery(NodeInfo recoveredNode) {
        // Remove from failed nodes
        failedNodes.remove(recoveredNode.getNodeId());

        // Clear failure metrics
        lastFailureTime.remove(recoveredNode.getNodeId());
        failureCount.remove(recoveredNode.getNodeId());

        System.out.println("Node " + recoveredNode.getNodeId() + " has recovered");

        // If we're the leader, reintegrate the recovered node
        if (raftNode.getState().equals(RaftState.LEADER)) {
            reintegrateNode(recoveredNode);
        }
    }

    private void reintegrateNode(NodeInfo recoveredNode) {
        // Recreate the stub for the recovered node
        ManagedChannel channel = ManagedChannelBuilder.forAddress(recoveredNode.getHost(), recoveredNode.getPort())
                .usePlaintext()
                .build();
        RaftGrpc.RaftStub stub = RaftGrpc.newStub(channel);
        raftNode.getPeerStubs().put(recoveredNode, stub);

        // Log the reintegration
        System.out.println("Node " + recoveredNode.getNodeId() + " has been reintegrated into the cluster");
    }

    public boolean isNodeFailed(String nodeId) {
        return failedNodes.containsKey(nodeId);
    }

    public List<NodeInfo> getFailedNodes() {
        return List.copyOf(failedNodes.values());
    }

    public long getLastFailureTime(String nodeId) {
        AtomicLong time = lastFailureTime.get(nodeId);
        return time != null ? time.get() : 0;
    }

    public int getFailureCount(String nodeId) {
        AtomicInteger count = failureCount.get(nodeId);
        return count != null ? count.get() : 0;
    }

    public NodeInfo getBackupNode(String failedNodeId) {
        return backupNodes.get(failedNodeId);
    }
}