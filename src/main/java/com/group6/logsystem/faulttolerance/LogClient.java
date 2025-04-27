package com.group6.logsystem.faulttolerance;

import com.group6.logsystem.models.InternalLogEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for interacting with other nodes in the cluster
 */
public class LogClient {
    private final String nodeId;
    private final ExecutorService executor;
    
    /**
     * Create a new log client
     * @param nodeId The ID of the node to connect to
     */
    public LogClient(String nodeId) {
        this.nodeId = nodeId;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Store a log entry on a remote node
     * @param entry The log entry to store
     * @return A future that completes with true if the log was stored successfully
     */
    public CompletableFuture<Boolean> storeLog(InternalLogEntry entry) {
        // In a real implementation, this would make an RPC call to the remote node
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network latency
                Thread.sleep(50);
                
                // Simulate a 15% chance of failure
                if (Math.random() < 0.15) {
                    throw new IOException("Simulated network failure");
                }
                
                return true;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    /**
     * Retrieve a specific log entry
     * @param logId The ID of the log to retrieve
     * @return A future that completes with the retrieved log entry
     */
    public CompletableFuture<InternalLogEntry> retrieveLog(String logId) {
        // In a real implementation, this would make an RPC call to the remote node
        // For now, we'll simulate the network call
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network latency
                Thread.sleep(50);
                
                // Simulate a 20% chance of failure
                if (Math.random() < 0.2) {
                    throw new IOException("Simulated network failure");
                }
                
                // Create a dummy log entry for demonstration
                return new InternalLogEntry(
                    logId,                      // logId
                    0,                         // index (default for simulation)
                    this.nodeId,               // nodeId (client's nodeId)
                    "Recovered log content",   // message
                    System.currentTimeMillis(), // timestamp
                    1                          // term (default for simulation)
                );
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    /**
     * Check if a node has a specific log
     * @param logId The ID of the log to check
     * @return A future that completes with true if the log exists
     */
    public CompletableFuture<Boolean> hasLog(String logId) {
        // In a real implementation, this would make an RPC call to the remote node
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network latency
                Thread.sleep(30);
                
                // Simulate a 10% chance of failure
                if (Math.random() < 0.1) {
                    throw new IOException("Simulated network failure");
                }
                
                // Simulate a 70% chance that the log exists
                return Math.random() < 0.7;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    /**
     * Get the current leader from a node
     * @return A future that completes with the leader's node ID
     */
    public CompletableFuture<String> getLeader() {
        // In a real implementation, this would make an RPC call to the remote node
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network latency
                Thread.sleep(40);
                
                // Simulate a 15% chance of failure
                if (Math.random() < 0.15) {
                    throw new IOException("Simulated network failure");
                }
                
                // Simulate a 80% chance that the node knows the leader
                if (Math.random() < 0.8) {
                    // Return a random leader ID for demonstration
                    return "node-" + (int)(Math.random() * 5);
                } else {
                    return null;
                }
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    /**
     * Get logs from a specific index
     * @param fromIndex The index to start from
     * @return A future that completes with a list of log entries
     */
    public CompletableFuture<List<InternalLogEntry>> getLogs(long fromIndex) {
        // In a real implementation, this would make an RPC call to the remote node
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network latency
                Thread.sleep(100);
                
                // Simulate a 25% chance of failure
                if (Math.random() < 0.25) {
                    throw new IOException("Simulated network failure");
                }
                
                // Create dummy log entries for demonstration
                List<InternalLogEntry> logs = new ArrayList<>();
                long currentIndex = fromIndex;
                int count = (int)(Math.random() * 10) + 1; // 1-10 logs
                
                for (int i = 0; i < count; i++) {
                    logs.add(new InternalLogEntry(
                        "log-" + currentIndex,                     // logId
                        (int) currentIndex,                        // index
                        this.nodeId,                               // nodeId
                        "Log content " + currentIndex,             // message
                        System.currentTimeMillis() - (count - i) * 1000, // timestamp
                        1                                          // term
                    ));
                    currentIndex++;
                }
                
                return logs;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
    
    /**
     * Close the client and release resources
     */
    public void close() {
        executor.shutdown();
    }
}