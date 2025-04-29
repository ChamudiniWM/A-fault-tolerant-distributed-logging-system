package com.dls.faulttolerance;

import com.dls.common.NodeInfo;
import com.dls.raft.RaftNode;
import com.dls.raft.rpc.RaftGrpc;
import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FailureDetector {
    private final RaftNode raftNode;
    private final Map<NodeInfo, AtomicInteger> failureCounts;
    private final Map<NodeInfo, AtomicLong> lastResponseTimes;
    private final Map<NodeInfo, AtomicInteger> responseTimeHistory;
    private final ScheduledExecutorService scheduler;
    private static final int MAX_FAILURES = 3;
    private static final int HEARTBEAT_INTERVAL = 150; // ms
    private static final int FAILURE_TIMEOUT = 500; // ms
    private static final int RESPONSE_TIME_WINDOW = 10; // Number of responses to track
    private static final long MAX_RESPONSE_TIME = 1000; // ms

    public FailureDetector(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.failureCounts = new ConcurrentHashMap<>();
        this.lastResponseTimes = new ConcurrentHashMap<>();
        this.responseTimeHistory = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);

        // Initialize metrics for all peers
        for (NodeInfo peer : raftNode.getPeers()) {
            if (!peer.getNodeId().equals(raftNode.getSelf().getNodeId())) {
                failureCounts.put(peer, new AtomicInteger(0));
                lastResponseTimes.put(peer, new AtomicLong(0));
                responseTimeHistory.put(peer, new AtomicInteger(0));
            }
        }
    }

    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(this::checkNodeHealth, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void checkNodeHealth() {
        for (NodeInfo peer : raftNode.getPeers()) {
            if (peer.getNodeId().equals(raftNode.getSelf().getNodeId())) {
                continue;
            }

            RaftGrpc.RaftStub stub = raftNode.getPeerStubs().get(peer);
            if (stub == null) {
                continue;
            }

            long startTime = System.currentTimeMillis();

            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setLeaderId(raftNode.getSelf().getNodeId())
                    .setPrevLogIndex(raftNode.getRaftLog().getLastLogIndex())
                    .setPrevLogTerm(raftNode.getRaftLog().getLastLogTerm())
                    .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                    .build();

            try {
                stub.appendEntries(request, new io.grpc.stub.StreamObserver<>() {
                    @Override
                    public void onNext(AppendEntriesResponse response) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        updateResponseMetrics(peer, responseTime);

                        if (response.getSuccess()) {
                            handleSuccess(peer);
                        } else {
                            handleFailure(peer, "Unsuccessful response");
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        handleFailure(peer, t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        // Reset failure count on successful completion
                        failureCounts.get(peer).set(0);
                    }
                });
            } catch (Exception e) {
                handleFailure(peer, e.getMessage());
            }
        }
    }

    private void updateResponseMetrics(NodeInfo peer, long responseTime) {
        lastResponseTimes.get(peer).set(responseTime);

        // Update response time history
        AtomicInteger history = responseTimeHistory.get(peer);
        int currentHistory = history.get();
        if (currentHistory < RESPONSE_TIME_WINDOW) {
            history.incrementAndGet();
        }

        // Log slow responses
        if (responseTime > MAX_RESPONSE_TIME) {
            System.out.println("Warning: Slow response from " + peer.getNodeId() +
                    " (response time: " + responseTime + "ms)");
        }
    }

    private void handleSuccess(NodeInfo peer) {
        failureCounts.get(peer).set(0);
        System.out.println("Node " + peer.getNodeId() + " is healthy. " +
                "Last response time: " + lastResponseTimes.get(peer).get() + "ms");
    }

    private void handleFailure(NodeInfo peer, String reason) {
        AtomicInteger count = failureCounts.get(peer);
        if (count != null) {
            int failures = count.incrementAndGet();
            System.out.println("Failure detected for node " + peer.getNodeId() +
                    " (attempt " + failures + "/" + MAX_FAILURES + "): " + reason);

            if (failures >= MAX_FAILURES) {
                System.out.println("Node " + peer.getNodeId() + " has been marked as failed after " +
                        failures + " consecutive failures");
                notifyNodeFailure(peer);
            }
        }
    }

    private void notifyNodeFailure(NodeInfo failedNode) {
        raftNode.getRedundancyManager().handleNodeFailure(failedNode);
    }

    public void stopMonitoring() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public long getLastResponseTime(NodeInfo peer) {
        return lastResponseTimes.get(peer).get();
    }

    public int getFailureCount(NodeInfo peer) {
        return failureCounts.get(peer).get();
    }

    public boolean isNodeHealthy(NodeInfo peer) {
        return failureCounts.get(peer).get() < MAX_FAILURES;
    }
}