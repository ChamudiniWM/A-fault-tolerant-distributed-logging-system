package com.group6.logsystem.faulttolerance;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.models.InternalLogEntry;
import com.group6.logsystem.faulttolerance.redundancy.ErasureCodingHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages log redundancy through replication and/or erasure coding
 */
public class LogRedundancyManager {
    private final RaftNode raftNode;
    private final boolean useErasureCoding;
    private final int replicationFactor;
    private final ErasureCodingHandler erasureCodingHandler;
    private final ExecutorService executor;

    /**
     * Create a new log redundancy manager
     * @param raftNode The Raft node this manager is associated with
     * @param useErasureCoding Whether to use erasure coding for redundancy
     * @param replicationFactor The number of nodes to replicate logs to
     */
    public LogRedundancyManager(RaftNode raftNode, boolean useErasureCoding, int replicationFactor) {
        this.raftNode = raftNode;
        this.useErasureCoding = useErasureCoding;
        this.replicationFactor = replicationFactor;
        this.executor = Executors.newFixedThreadPool(4);
        
        if (useErasureCoding) {
            this.erasureCodingHandler = new ErasureCodingHandler(raftNode.getNodeId());
        } else {
            this.erasureCodingHandler = null;
        }
    }

    /**
     * Replicate a log entry to other nodes
     * @param entry The log entry to replicate
     * @return A future that completes when the replication is done
     */
    public CompletableFuture<Boolean> replicateLog(InternalLogEntry entry) {
        if (useErasureCoding) {
            return erasureCodingHandler.encodeAndStoreLog(entry);
        } else {
            return replicateToNodes(entry, replicationFactor);
        }
    }

    /**
     * Replicate a log entry to a specific number of nodes
     * @param entry The log entry to replicate
     * @param targetReplicationFactor The number of nodes to replicate to
     * @return A future that completes when the replication is done
     */
    public CompletableFuture<Boolean> replicateToNodes(InternalLogEntry entry, int targetReplicationFactor) {
        List<String> availablePeers = raftNode.getPeers().stream()
            .filter(peer -> !raftNode.isPartitioned(peer))
            .collect(Collectors.toList());
        
        if (availablePeers.size() < targetReplicationFactor - 1) {
            System.err.println("Warning: Not enough available peers for desired replication factor");
        }
        
        // Calculate how many peers we need to replicate to
        int peersToReplicate = Math.min(availablePeers.size(), targetReplicationFactor - 1);
        
        if (peersToReplicate == 0) {
            // No peers to replicate to, but we have the log locally
            return CompletableFuture.completedFuture(true);
        }
        
        // Create a list of futures for each replication
        List<CompletableFuture<Boolean>> replicationFutures = new ArrayList<>();
        
        // Replicate to the selected peers
        for (int i = 0; i < peersToReplicate; i++) {
            String peer = availablePeers.get(i);
            LogClient client = new LogClient(peer);
            replicationFutures.add(client.storeLog(entry));
        }
        
        // Wait for all replications to complete
        return CompletableFuture.allOf(replicationFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                // Count successful replications
                long successCount = replicationFutures.stream()
                    .filter(f -> {
                        try {
                            return f.join();
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
                
                // We consider replication successful if at least half of the peers got the log
                return successCount >= Math.ceil(peersToReplicate / 2.0);
            });
    }

    /**
     * Retrieve a log entry from redundant storage
     * @param logId The ID of the log to retrieve
     * @return A future that completes with the retrieved log entry
     */
    public CompletableFuture<InternalLogEntry> retrieveRedundantLog(String logId) {
        if (useErasureCoding) {
            return erasureCodingHandler.retrieveLog(logId)
                .exceptionally(ex -> {
                    System.err.println("Failed to retrieve log using erasure coding: " + ex.getMessage());
                    // Fall back to replication if available
                    if (replicationFactor > 1) {
                        return retrieveReplicatedLog(logId).join();
                    }
                    return null;
                });
        } else {
            return retrieveReplicatedLog(logId);
        }
    }

    /**
     * Retrieve a log entry from replicated storage
     * @param logId The ID of the log to retrieve
     * @return A future that completes with the retrieved log entry
     */
    private CompletableFuture<InternalLogEntry> retrieveReplicatedLog(String logId) {
        List<String> availablePeers = raftNode.getPeers().stream()
            .filter(peer -> !raftNode.isPartitioned(peer))
            .collect(Collectors.toList());
        
        if (availablePeers.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No available peers to retrieve log from"));
        }
        
        // Try to retrieve the log from each peer until successful
        CompletableFuture<InternalLogEntry> result = new CompletableFuture<>();
        retrieveFromNextPeer(logId, availablePeers, 0, result);
        return result;
    }

    /**
     * Recursively try to retrieve a log from peers
     */
    private void retrieveFromNextPeer(String logId, List<String> peers, int index, 
                                     CompletableFuture<InternalLogEntry> result) {
        if (index >= peers.size()) {
            result.completeExceptionally(
                new IllegalStateException("Failed to retrieve log from any peer"));
            return;
        }
        
        String peer = peers.get(index);
        LogClient client = new LogClient(peer);
        
        client.retrieveLog(logId)
            .thenAccept(result::complete)
            .exceptionally(ex -> {
                // Try the next peer
                retrieveFromNextPeer(logId, peers, index + 1, result);
                return null;
            });
    }

    /**
     * Implement hybrid redundancy strategy
     * @param entry The log entry to store with redundancy
     * @return A future that completes when the redundancy is established
     */
    public CompletableFuture<Boolean> implementHybridRedundancy(InternalLogEntry entry) {
        // First replicate to a subset of nodes
        int hybridReplicationFactor = Math.max(2, replicationFactor - 1);
        
        return replicateToNodes(entry, hybridReplicationFactor)
            .thenCompose(success -> {
                if (success) {
                    // Then apply erasure coding for additional redundancy
                    return erasureCodingHandler.encodeAndStoreLog(entry);
                } else {
                    return CompletableFuture.completedFuture(false);
                }
            });
    }

    /**
     * Check if a log entry exists in redundant storage
     * @param logId The ID of the log to check
     * @return A future that completes with true if the log exists
     */
    public CompletableFuture<Boolean> hasRedundantLog(String logId) {
        if (useErasureCoding) {
            return erasureCodingHandler.hasLog(logId)
                .thenCompose(exists -> {
                    if (exists) {
                        return CompletableFuture.completedFuture(true);
                    } else if (replicationFactor > 1) {
                        // Check replicated storage
                        return hasReplicatedLog(logId);
                    } else {
                        return CompletableFuture.completedFuture(false);
                    }
                });
        } else {
            return hasReplicatedLog(logId);
        }
    }

    /**
     * Check if a log entry exists in replicated storage
     */
    private CompletableFuture<Boolean> hasReplicatedLog(String logId) {
        List<String> availablePeers = raftNode.getPeers().stream()
            .filter(peer -> !raftNode.isPartitioned(peer))
            .collect(Collectors.toList());
        
        if (availablePeers.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        
        // Check if any peer has the log
        List<CompletableFuture<Boolean>> futures = availablePeers.stream()
            .map(peer -> {
                LogClient client = new LogClient(peer);
                return client.hasLog(logId);
            })
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream().anyMatch(CompletableFuture::join));
    }

    /**
     * Shutdown the redundancy manager
     */
    public void shutdown() {
        executor.shutdown();
        if (erasureCodingHandler != null) {
            erasureCodingHandler.shutdown();
        }
    }
}
