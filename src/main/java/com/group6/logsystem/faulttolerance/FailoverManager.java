package com.group6.logsystem.faulttolerance;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.models.InternalLogEntry;
import com.group6.logsystem.grpc.LogServiceGrpc;
import com.group6.logsystem.grpc.LogRequest;
import com.group6.logsystem.grpc.LogResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages failover operations when nodes fail or recover.
 * Handles redirecting log streams and recovering logs.
 */
public class FailoverManager {
    private final RaftNode raftNode;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ManagedChannel> peerChannels;
    private final Map<String, LogServiceGrpc.LogServiceStub> peerStubs;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Queue<InternalLogEntry> pendingLogs = new ConcurrentLinkedQueue<>();
    private final Set<String> knownLeaders = ConcurrentHashMap.newKeySet();
    private String currentLeader;

    public FailoverManager(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.peerChannels = new HashMap<>();
        this.peerStubs = new HashMap<>();
        this.currentLeader = null;
        
        // Initialize connections to peers
        initializePeerConnections();
    }

    private void initializePeerConnections() {
        for (String peer : raftNode.getPeers()) {
            try {
                // Extract host and port from peer address (format: host:port)
                String[] parts = peer.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                
                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(host, port)
                        .usePlaintext()
                        .build();
                
                peerChannels.put(peer, channel);
                peerStubs.put(peer, LogServiceGrpc.newStub(channel));
                
                System.out.println("Initialized connection to peer: " + peer);
            } catch (Exception e) {
                System.err.println("Failed to initialize connection to peer " + peer + ": " + e.getMessage());
            }
        }
    }

    /**
     * Start the failover manager
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            System.out.println("Starting failover manager for node " + raftNode.getNodeId());
            
            // Schedule periodic leader discovery
            scheduler.scheduleAtFixedRate(this::discoverLeader, 0, 5, TimeUnit.SECONDS);
            
            // Schedule processing of pending logs
            scheduler.scheduleAtFixedRate(this::processPendingLogs, 1, 1, TimeUnit.SECONDS);
        }
    }

    /**
     * Discover the current leader in the cluster
     */
    private void discoverLeader() {
        // If we are the leader, no need to discover
        if (raftNode.getRole() == RaftNode.Role.LEADER) {
            currentLeader = raftNode.getNodeId();
            return;
        }
        
        // Check if we know who the leader is
        if (currentLeader != null && !raftNode.isPartitioned(currentLeader)) {
            // We already know the leader and it's not partitioned
            return;
        }
        
        // We need to find the leader
        for (String peer : raftNode.getPeers()) {
            if (!raftNode.isPartitioned(peer)) {
                // TODO: Implement a proper leader discovery mechanism
                // For now, just assume the first non-partitioned peer is the leader
                currentLeader = peer;
                System.out.println("Discovered leader: " + currentLeader);
                return;
            }
        }
        
        // No leader found
        currentLeader = null;
        System.out.println("No leader found in the cluster");
    }

    /**
     * Process any pending logs that couldn't be sent due to leader unavailability
     */
    private void processPendingLogs() {
        if (currentLeader == null) {
            // No leader, can't process logs
            return;
        }
        
        // Process up to 10 pending logs at a time
        for (int i = 0; i < 10; i++) {
            InternalLogEntry entry = pendingLogs.poll();
            if (entry == null) {
                // No more pending logs
                break;
            }
            
            sendLogToLeader(entry);
        }
    }

    /**
     * Send a log entry to the current leader
     */
    private void sendLogToLeader(InternalLogEntry entry) {
        if (currentLeader == null) {
            // No leader, add to pending logs
            pendingLogs.add(entry);
            return;
        }
        
        // If we are the leader, append directly
        if (currentLeader.equals(raftNode.getNodeId())) {
            raftNode.appendLogEntry(entry);
            return;
        }
        
        // Send to the leader
        LogServiceGrpc.LogServiceStub stub = peerStubs.get(currentLeader);
        if (stub == null) {
            System.err.println("No stub available for leader: " + currentLeader);
            pendingLogs.add(entry);
            return;
        }
        
        LogRequest request = LogRequest.newBuilder()
                .setLogId(entry.getLogId())
                .setNodeId(entry.getNodeId())
                .setMessage(entry.getMessage())
                .setTimestamp(entry.getTimestamp())
                .setTerm(entry.getTerm())
                .build();
        
        stub.sendLog(request, new StreamObserver<LogResponse>() {
            @Override
            public void onNext(LogResponse response) {
                if (!response.getSuccess()) {
                    System.err.println("Failed to send log to leader: " + response.getMessage());
                    pendingLogs.add(entry);
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error sending log to leader: " + t.getMessage());
                pendingLogs.add(entry);
                // Leader might be down, trigger rediscovery
                currentLeader = null;
            }

            @Override
            public void onCompleted() {
                // Log sent successfully
            }
        });
    }

    /**
     * Handle a node failure
     */
    public void handleNodeFailure(String nodeId) {
        System.out.println("Handling failure of node: " + nodeId);
        
        // If the failed node was the leader, clear the current leader
        if (nodeId.equals(currentLeader)) {
            currentLeader = null;
            System.out.println("Leader failed, triggering leader discovery");
        }
        
        // If we are the leader, we need to update our view of the cluster
        if (raftNode.getRole() == RaftNode.Role.LEADER) {
            raftNode.addPartitionedPeer(nodeId);
        }
    }

    /**
     * Handle a node recovery
     */
    public void handleNodeRecovery(String nodeId) {
        System.out.println("Handling recovery of node: " + nodeId);
        
        // If we are the leader, we need to update our view of the cluster
        if (raftNode.getRole() == RaftNode.Role.LEADER) {
            raftNode.removePartitionedPeer(nodeId);
            
            // TODO: Implement log synchronization for the recovered node
        }
    }

    /**
     * Submit a log entry to the cluster
     */
    public CompletableFuture<Boolean> submitLog(InternalLogEntry entry) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        // If we are the leader, append directly
        if (raftNode.getRole() == RaftNode.Role.LEADER) {
            try {
                raftNode.appendLogEntry(entry);
                result.complete(true);
            } catch (Exception e) {
                System.err.println("Error appending log entry: " + e.getMessage());
                result.complete(false);
            }
            return result;
        }
        
        // Otherwise, send to the leader
        sendLogToLeader(entry);
        
        // For simplicity, we'll assume it succeeded
        // In a real implementation, we'd wait for confirmation
        result.complete(true);
        
        return result;
    }

    /**
     * Shutdown the failover manager
     */
    public void shutdown() {
        if (isRunning.compareAndSet(true, false)) {
            System.out.println("Shutting down failover manager for node " + raftNode.getNodeId());
            scheduler.shutdownNow();
            
            for (ManagedChannel channel : peerChannels.values()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}