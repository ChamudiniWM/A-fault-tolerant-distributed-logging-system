package com.group6.logsystem.faulttolerance;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.models.InternalLogEntry;
import com.group6.logsystem.faulttolerance.redundancy.ReplicationStrategy;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main manager class that coordinates all fault tolerance components.
 */
public class FaultToleranceManager {
    private final RaftNode raftNode;
    private final FailureDetector failureDetector;
    private final LogRedundancyManager redundancyManager;
    private final FailoverManager failoverManager;
    private final LogRecoveryService recoveryService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ReplicationStrategy replicationStrategy;

    public FaultToleranceManager(RaftNode raftNode, ReplicationStrategy replicationStrategy) {
        this.raftNode = raftNode;
        this.replicationStrategy = replicationStrategy;
        
        // Initialize components
        this.failureDetector = new FailureDetector(raftNode, raftNode.getPeers());
        
        boolean useErasureCoding = replicationStrategy == ReplicationStrategy.ERASURE_CODING || 
                                  replicationStrategy == ReplicationStrategy.HYBRID;
        
        int replicationFactor = 3; // Default replication factor
        this.redundancyManager = new LogRedundancyManager(raftNode, useErasureCoding, replicationFactor);
        this.failoverManager = new FailoverManager(raftNode);
        this.recoveryService = new LogRecoveryService(raftNode);
    }

    /**
     * Start all fault tolerance components
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            System.out.println("Starting fault tolerance manager for node " + raftNode.getNodeId());
            
            // Start components
            failureDetector.start();
            failoverManager.start();
            
            // If this node is recovering, trigger log recovery
            if (raftNode.getRole() == RaftNode.Role.FOLLOWER) {
                recoverLogs();
            }
        }
    }

    /**
     * Handle a node failure
     */
    public void handleNodeFailure(String nodeId) {
        System.out.println("Handling failure of node: " + nodeId);
        
        // Mark the node as partitioned
        raftNode.addPartitionedPeer(nodeId);
        
        // Notify the failover manager
        failoverManager.handleNodeFailure(nodeId);
    }

    /**
     * Handle a node recovery
     */
    public void handleNodeRecovery(String nodeId) {
        System.out.println("Handling recovery of node: " + nodeId);
        
        // Mark the node as available
        raftNode.removePartitionedPeer(nodeId);
        
        // Notify the failover manager
        failoverManager.handleNodeRecovery(nodeId);
    }

    /**
     * Submit a log entry with fault tolerance
     */
    public CompletableFuture<Boolean> submitLog(InternalLogEntry entry) {
        // Use the failover manager to submit the log
        return failoverManager.submitLog(entry)
                .thenCompose(success -> {
                    if (success && raftNode.getRole() == RaftNode.Role.LEADER) {
                        // If we're the leader and the log was submitted successfully,
                        // replicate it according to our strategy
                        if (replicationStrategy == ReplicationStrategy.HYBRID) {
                            return redundancyManager.implementHybridRedundancy(entry);
                        } else {
                            return redundancyManager.replicateLog(entry);
                        }
                    } else {
                        return CompletableFuture.completedFuture(success);
                    }
                });
    }

    /**
     * Retrieve a log entry with fault tolerance
     * @param logId The ID of the log to retrieve
     * @return A future that completes with the retrieved log entry
     */
    public CompletableFuture<InternalLogEntry> retrieveLog(String logId) {
        // First try to get the log from the local store by querying all logs
        CompletableFuture<InternalLogEntry> localLogFuture = CompletableFuture.supplyAsync(() -> {
            // Query all logs with a wide time range to find our specific log
            List<InternalLogEntry> entries = raftNode.queryLogs("", Long.MIN_VALUE, Long.MAX_VALUE);
            for (InternalLogEntry entry : entries) {
                if (entry.getLogId().equals(logId)) {
                    return entry;
                }
            }
            return null;
        });
        
        return localLogFuture.thenCompose(localLog -> {
            if (localLog != null) {
                // We found it locally
                return CompletableFuture.completedFuture(localLog);
            }
            
            // If not found locally, try to retrieve from redundant storage
            return redundancyManager.retrieveRedundantLog(logId)
                .thenApply(log -> {
                    if (log != null) {
                        // Store the recovered log locally for future use
                        raftNode.appendLogEntry(log);
                    }
                    return log;
                });
        });
    }

    /**
     * Recover logs after rejoining the cluster
     */
    public CompletableFuture<Integer> recoverLogs() {
        System.out.println("Initiating log recovery for node " + raftNode.getNodeId());
        return recoveryService.recoverLogs();
    }

    /**
     * Shutdown all fault tolerance components
     */
    public void shutdown() {
        if (isRunning.compareAndSet(true, false)) {
            System.out.println("Shutting down fault tolerance manager for node " + raftNode.getNodeId());
            
            // Shutdown components
            failureDetector.shutdown();
            redundancyManager.shutdown();
            failoverManager.shutdown();
            recoveryService.shutdown();
        }
    }

    /**
     * Get the current replication strategy
     */
    public ReplicationStrategy getReplicationStrategy() {
        return replicationStrategy;
    }
}