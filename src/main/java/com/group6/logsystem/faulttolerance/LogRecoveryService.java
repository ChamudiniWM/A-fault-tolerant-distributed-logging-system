package com.group6.logsystem.faulttolerance;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.models.InternalLogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LogRecoveryService {
    private final RaftNode raftNode;
    private final ExecutorService executor;
    private final AtomicBoolean isRecovering = new AtomicBoolean(false);
    private Function<String, LogClient> logClientFactory;
    private Function<String, LogRedundancyManager> redundancyManagerFactory;

    public LogRecoveryService(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.executor = Executors.newSingleThreadExecutor();
        this.logClientFactory = LogClient::new;
        this.redundancyManagerFactory = nodeId -> new LogRedundancyManager(raftNode, true, 3);
    }

    public void setLogClientFactory(Function<String, LogClient> factory) {
        this.logClientFactory = factory;
    }

    public void setRedundancyManagerFactory(Function<String, LogRedundancyManager> factory) {
        this.redundancyManagerFactory = factory;
    }

    public CompletableFuture<Integer> recoverLogs() {
        System.out.println("Starting log recovery for node " + raftNode.getNodeId());

        if (!isRecovering.compareAndSet(false, true)) {
            System.out.println("Recovery already in progress");
            return CompletableFuture.completedFuture(0);
        }

        return discoverLeader()
                .thenCompose(leader -> {
                    if (leader == null) {
                        System.err.println("Failed to discover leader for log recovery");
                        return recoverFromRedundantSources(raftNode.getLastLogIndex() + 1);
                    }

                    System.out.println("Discovered leader: " + leader);
                    long lastLogIndex = raftNode.getLastLogIndex();

                    return requestMissingLogs(leader, lastLogIndex + 1)
                            .thenCompose(count -> {
                                if (count == 0) {
                                    return recoverFromRedundantSources(lastLogIndex + 1);
                                }
                                return CompletableFuture.completedFuture(count);
                            });
                })
                .whenComplete((result, ex) -> {
                    isRecovering.set(false);
                    if (ex != null) {
                        System.err.println("Error during log recovery: " + ex.getMessage());
                    } else {
                        System.out.println("Recovered " + result + " logs");
                    }
                });
    }

    private CompletableFuture<String> discoverLeader() {
        List<String> availablePeers = raftNode.getPeers().stream()
                .filter(peer -> !raftNode.isPartitioned(peer))
                .collect(Collectors.toList());

        if (availablePeers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<String> leaderFuture = new CompletableFuture<>();
        AtomicInteger index = new AtomicInteger(0);
        checkNextPeerForLeader(availablePeers, index, leaderFuture);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            if (!leaderFuture.isDone()) {
                leaderFuture.complete(null);
            }
            scheduler.shutdown();
        }, 10, TimeUnit.SECONDS);

        return leaderFuture;
    }

    private void checkNextPeerForLeader(List<String> peers, AtomicInteger index,
                                     CompletableFuture<String> leaderFuture) {
        if (index.get() >= peers.size() || leaderFuture.isDone()) {
            if (!leaderFuture.isDone()) {
                leaderFuture.complete(null);
            }
            return;
        }

        String peer = peers.get(index.getAndIncrement());
        LogClient client = logClientFactory.apply(peer);

        client.getLeader()
                .thenAccept(leader -> {
                    if (leader != null && !leader.isEmpty()) {
                        leaderFuture.complete(leader);
                    } else {
                        checkNextPeerForLeader(peers, index, leaderFuture);
                    }
                })
                .exceptionally(ex -> {
                    checkNextPeerForLeader(peers, index, leaderFuture);
                    return null;
                });
    }

    private CompletableFuture<Integer> requestMissingLogs(String nodeId, long fromIndex) {
        LogClient client = logClientFactory.apply(nodeId);

        return client.getLogs(fromIndex)
                .thenCompose(logs -> {
                    if (logs == null || logs.isEmpty()) {
                        return CompletableFuture.completedFuture(0);
                    }

                    AtomicInteger count = new AtomicInteger(0);
                    List<CompletableFuture<Boolean>> futures = logs.stream()
                            .<CompletableFuture<Boolean>>map(log -> 
                                raftNode.applyLog(log)
                                    .thenApply(success -> {
                                        if (success) {
                                            count.incrementAndGet();
                                        }
                                        return success;
                                    }))
                            .collect(Collectors.toList());

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> count.get());
                });
    }

    private CompletableFuture<Integer> recoverFromRedundantSources(long fromIndex) {
        System.out.println("Attempting to recover logs from redundant sources");
    
        List<String> logIds = getLogIdsToRecover(fromIndex);
        if (logIds.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }
    
        LogRedundancyManager redundancyManager = redundancyManagerFactory.apply(raftNode.getNodeId());
        AtomicInteger recoveredCount = new AtomicInteger(0);
    
        // Create all the recovery futures
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (String logId : logIds) {
            CompletableFuture<Boolean> recoveryFuture = redundancyManager.retrieveRedundantLog(logId)
                .thenCompose(log -> {
                    if (log != null) {
                        return raftNode.applyLog(log)
                                .thenApply(success -> {
                                    if (success) {
                                        recoveredCount.incrementAndGet();
                                    }
                                    return success;
                                });
                    }
                    return CompletableFuture.completedFuture(false);
                })
                .exceptionally(ex -> {
                    System.err.println("Failed to recover log " + logId + ": " + ex.getMessage());
                    return false;
                });
            
            futures.add(recoveryFuture);
        }
    
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> recoveredCount.get());
    }

    private List<String> getLogIdsToRecover(long fromIndex) {
        List<String> logIds = new ArrayList<>();
        long currentIndex = fromIndex;
        long maxIndex = currentIndex + 100;

        while (currentIndex < maxIndex) {
            logIds.add("log-" + currentIndex);
            currentIndex++;
        }

        return logIds;
    }

    public void shutdown() {
        executor.shutdown();
    }
}