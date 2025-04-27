package com.group6.logsystem.faulttolerance;

import com.group6.logsystem.consensus.RaftNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Responsible for detecting node failures in the distributed system.
 * Uses heartbeat mechanism to track node health and marks nodes as partitioned when they fail.
 */
public class FailureDetector {
    private final RaftNode raftNode;
    private final Map<String, Long> lastHeartbeat;
    private final Map<String, Integer> missedHeartbeats;
    private final ScheduledExecutorService scheduler;
    private final long heartbeatInterval = 100; // ms
    private final long heartbeatTimeout = 500; // ms
    private final int maxMissedHeartbeats = 3;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public FailureDetector(RaftNode raftNode, List<String> peers) {
        this.raftNode = raftNode;
        this.lastHeartbeat = new HashMap<>();
        this.missedHeartbeats = new HashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Initialize heartbeat tracking for all peers
        peers.forEach(peer -> {
            lastHeartbeat.put(peer, System.currentTimeMillis());
            missedHeartbeats.put(peer, 0);
        });
    }

    /**
     * Start the failure detector service
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            System.out.println("Starting failure detector for node " + raftNode.getNodeId());
            scheduler.scheduleAtFixedRate(this::checkNodeHealth, 0, heartbeatInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Record a heartbeat received from a peer
     */
    public void receiveHeartbeat(String peerId) {
        lastHeartbeat.put(peerId, System.currentTimeMillis());
        missedHeartbeats.put(peerId, 0); // Reset missed heartbeats counter
        
        // If the peer was previously marked as partitioned, remove it
        if (raftNode.isPartitioned(peerId)) {
            System.out.println("Node " + peerId + " has recovered and is now available.");
            raftNode.removePartitionedPeer(peerId);
        }
    }

    /**
     * Check the health of all peers
     */
    private void checkNodeHealth() {
        long now = System.currentTimeMillis();
        
        lastHeartbeat.forEach((peerId, lastTime) -> {
            if (now - lastTime > heartbeatTimeout) {
                // Increment missed heartbeats counter
                int missed = missedHeartbeats.getOrDefault(peerId, 0) + 1;
                missedHeartbeats.put(peerId, missed);
                
                if (missed >= maxMissedHeartbeats) {
                    if (!raftNode.isPartitioned(peerId)) {
                        System.out.println("Node " + peerId + " detected as unavailable after " + missed + " missed heartbeats.");
                        raftNode.addPartitionedPeer(peerId);
                        
                        // Trigger failover if needed
                        if (raftNode.getRole() == RaftNode.Role.LEADER) {
                            checkQuorum();
                        }
                    }
                } else {
                    System.out.println("Node " + peerId + " missed heartbeat: " + missed + "/" + maxMissedHeartbeats);
                }
            }
        });
    }
    
    /**
     * Check if the leader still has quorum
     */
    private void checkQuorum() {
        int totalNodes = raftNode.getPeers().size() + 1; // Include self
        int availableNodes = totalNodes - countPartitionedPeers();
        int majority = (totalNodes / 2) + 1;
        
        if (availableNodes < majority) {
            System.out.println("Leader " + raftNode.getNodeId() + " lost quorum. Stepping down.");
            raftNode.becomeFollower(raftNode.getCurrentTerm());
        }
    }

    /**
     * Count the number of partitioned peers by checking each peer's status
     */
    private int countPartitionedPeers() {
        int count = 0;
        for (String peer : raftNode.getPeers()) {
            if (raftNode.isPartitioned(peer)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Shutdown the failure detector
     */
    public void shutdown() {
        if (isRunning.compareAndSet(true, false)) {
            System.out.println("Shutting down failure detector for node " + raftNode.getNodeId());
            scheduler.shutdownNow();
        }
    }
}