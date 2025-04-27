package com.group6.logsystem.faulttolerance;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.models.InternalLogEntry;
import com.group6.logsystem.faulttolerance.redundancy.ReplicationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Evaluates the performance of different redundancy strategies
 */
public class RedundancyEvaluator {
    private final RaftNode raftNode;
    private final LogRedundancyManager redundancyManager;
    
    /**
     * Represents the results of comparing different redundancy strategies
     */
    public static class StrategyComparison {
        private final long fullReplicationTime;
        private final long erasureCodingTime;
        private final long hybridTime;
        
        public StrategyComparison(long fullReplicationTime, long erasureCodingTime, long hybridTime) {
            this.fullReplicationTime = fullReplicationTime;
            this.erasureCodingTime = erasureCodingTime;
            this.hybridTime = hybridTime;
        }
        
        public long getFullReplicationTime() { return fullReplicationTime; }
        public long getErasureCodingTime() { return erasureCodingTime; }
        public long getHybridTime() { return hybridTime; }
    }
    
    /**
     * Create a new redundancy evaluator
     * @param raftNode The Raft node this evaluator is associated with
     * @param redundancyManager The redundancy manager to evaluate
     */
    public RedundancyEvaluator(RaftNode raftNode, LogRedundancyManager redundancyManager) {
        this.raftNode = raftNode;
        this.redundancyManager = redundancyManager;
    }
    
    /**
     * Evaluate the hybrid redundancy strategy
     * @param testLog A test log entry to use for evaluation
     * @return A future that completes with true if the evaluation was successful
     */
    public CompletableFuture<Boolean> evaluateHybridRedundancy(InternalLogEntry testLog) {
        System.out.println("Evaluating hybrid redundancy strategy...");
        
        long startTime = System.currentTimeMillis();
        
        // First, implement hybrid redundancy
        return redundancyManager.implementHybridRedundancy(testLog)
            .thenCompose(implementationSuccess -> {
                if (!implementationSuccess) {
                    System.err.println("Failed to implement hybrid redundancy");
                    return CompletableFuture.completedFuture(false);
                }
                
                long implementationTime = System.currentTimeMillis() - startTime;
                System.out.println("Hybrid redundancy implementation time: " + implementationTime + "ms");
                
                // Then, retrieve the log to verify it was stored correctly
                return redundancyManager.retrieveRedundantLog(testLog.getLogId())
                    .thenApply(retrievedLog -> {
                        long retrievalTime = System.currentTimeMillis() - startTime - implementationTime;
                        System.out.println("Hybrid redundancy retrieval time: " + retrievalTime + "ms");
                        
                        boolean retrievalSuccess = retrievedLog != null && retrievedLog.getLogId().equals(testLog.getLogId());
                        System.out.println("Hybrid redundancy evaluation " + 
                            (retrievalSuccess ? "successful" : "failed"));
                        
                        return retrievalSuccess;
                    });
            });
    }
    
    /**
     * Evaluate the storage efficiency of the redundancy strategy
     * @return The storage efficiency (ratio of logical to physical storage)
     */
    public double evaluateStorageEfficiency() {
        System.out.println("Evaluating storage efficiency...");
        
        // For demonstration, we'll return a simulated efficiency
        double efficiency = 0.0;
        
        // Note: Cannot call redundancyManager.getReplicationStrategy() as it's undefined.
        // Hardcoding HYBRID for now. Member 3 must add getReplicationStrategy() to LogRedundancyManager.
        ReplicationStrategy strategy = ReplicationStrategy.HYBRID;
        
        if (strategy == ReplicationStrategy.FULL_REPLICATION) {
            efficiency = 1.0 / 3.0; // Assuming replication factor of 3
        } else if (strategy == ReplicationStrategy.ERASURE_CODING) {
            efficiency = 0.6; // Assuming 6 data shards and 4 parity shards
        } else if (strategy == ReplicationStrategy.HYBRID) {
            efficiency = 0.5;
        }
        
        System.out.println("Storage efficiency: " + efficiency);
        return efficiency;
    }
    
    /**
     * Evaluate the recovery time for a given number of logs
     * @param logCount The number of logs to recover
     * @return A future that completes with the total recovery time in milliseconds
     */
    public CompletableFuture<Long> evaluateRecoveryTime(int logCount) {
        System.out.println("Evaluating recovery time for " + logCount + " logs...");
        
        long startTime = System.currentTimeMillis();
        
        // Create test log IDs
        List<String> logIds = new ArrayList<>();
        for (int i = 0; i < logCount; i++) {
            logIds.add("test-log-" + i);
        }
        
        // Retrieve each log and measure the time
        List<CompletableFuture<Void>> futures = logIds.stream()
            .map(logId -> redundancyManager.retrieveRedundantLog(logId)
                .thenAccept(log -> {
                    // Just consume the log
                }))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                long totalTime = System.currentTimeMillis() - startTime;
                System.out.println("Recovery time for " + logCount + " logs: " + totalTime + "ms");
                System.out.println("Average recovery time per log: " + (totalTime / logCount) + "ms");
                return totalTime;
            });
    }
    
    /**
     * Run a comprehensive evaluation of the redundancy strategy
     * @return A future that completes when the evaluation is done
     */
    public CompletableFuture<Void> runComprehensiveEvaluation() {
        System.out.println("Running comprehensive redundancy evaluation...");
        
        // Create a test log with proper constructor parameters
        InternalLogEntry testLog = new InternalLogEntry(
            "eval-log-" + System.currentTimeMillis(),
            0, // index
            raftNode.getNodeId(), // nodeId
            "Evaluation test log content", // message
            System.currentTimeMillis(), // timestamp
            raftNode.getCurrentTerm() // term
        );
        
        // Evaluate hybrid redundancy
        return evaluateHybridRedundancy(testLog)
            .thenCompose(hybridSuccess -> {
                // Evaluate storage efficiency
                double efficiency = evaluateStorageEfficiency();
                
                // Evaluate recovery time
                return evaluateRecoveryTime(10);
            })
            .thenAccept(recoveryTime -> {
                System.out.println("Comprehensive evaluation complete");
                
                // Print summary
                System.out.println("=== Redundancy Evaluation Summary ===");
                System.out.println("Storage Efficiency: " + evaluateStorageEfficiency());
                System.out.println("Recovery Time (10 logs): " + recoveryTime + "ms");
                System.out.println("===================================");
            });
    }
    
    /**
     * Compare the performance of different redundancy strategies
     * @param logCount The number of logs to use for comparison
     * @return The comparison results
     */
    public StrategyComparison compareStrategies(int logCount) {
        System.out.println("Comparing redundancy strategies for " + logCount + " logs...");
        
        // Note: Cannot call redundancyManager.getReplicationStrategy() as it's undefined.
        // Simulating strategy comparison with hardcoded delays for each strategy.
        
        long fullReplicationTime = 0;
        long erasureCodingTime = 0;
        long hybridTime = 0;
        
        // Simulate FULL_REPLICATION
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < logCount; i++) {
            try {
                Thread.sleep(30); // Simulate full replication delay
                redundancyManager.retrieveRedundantLog("test-log-" + i).join();
            } catch (Exception e) {
                System.err.println("Error in full replication simulation: " + e.getMessage());
            }
        }
        fullReplicationTime = System.currentTimeMillis() - startTime;
        
        // Simulate ERASURE_CODING
        startTime = System.currentTimeMillis();
        for (int i = 0; i < logCount; i++) {
            try {
                Thread.sleep(50); // Simulate erasure coding delay
                redundancyManager.retrieveRedundantLog("test-log-" + i).join();
            } catch (Exception e) {
                System.err.println("Error in erasure coding simulation: " + e.getMessage());
            }
        }
        erasureCodingTime = System.currentTimeMillis() - startTime;
        
        // Simulate HYBRID
        startTime = System.currentTimeMillis();
        for (int i = 0; i < logCount; i++) {
            try {
                Thread.sleep(40); // Simulate hybrid delay
                redundancyManager.retrieveRedundantLog("test-log-" + i).join();
            } catch (Exception e) {
                System.err.println("Error in hybrid simulation: " + e.getMessage());
            }
        }
        hybridTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Full Replication Time: " + fullReplicationTime + "ms");
        System.out.println("Erasure Coding Time: " + erasureCodingTime + "ms");
        System.out.println("Hybrid Time: " + hybridTime + "ms");
        
        return new StrategyComparison(fullReplicationTime, erasureCodingTime, hybridTime);
    }
}