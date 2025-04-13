package com.group6.logsystem.consensus;

import com.group6.logsystem.models.InternalLogEntry;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class LogReplicationService {

    private final RaftNode raftNode;
    private final List<RaftNode> peers;
    private final ScheduledExecutorService replicationScheduler;
    private final Map<String, Integer> nextIndex;
    private final Map<String, Integer> matchIndex;
    private final ReentrantLock lock = new ReentrantLock();

    public LogReplicationService(RaftNode raftNode, List<RaftNode> peers) {
        this.raftNode = raftNode;
        this.peers = peers;
        this.replicationScheduler = Executors.newSingleThreadScheduledExecutor();
        this.nextIndex = new ConcurrentHashMap<>();
        this.matchIndex = new ConcurrentHashMap<>();
    }

    // Method to start log replication
    public void startLogReplication() {
        if (raftNode.getRole() == RaftNode.Role.LEADER) {
            // Start periodic log replication for all followers
            replicationScheduler.scheduleAtFixedRate(this::replicateLogsToFollowers, 0, 100, TimeUnit.MILLISECONDS);
        }
    }

    // Periodic log replication to all followers
    private void replicateLogsToFollowers() {
        if (raftNode.getRole() != RaftNode.Role.LEADER) {
            return; // Only leader replicates logs
        }

        // Loop through all peers and send AppendEntries RPC
        for (RaftNode peer : peers) {
            int nextIdx = nextIndex.getOrDefault(peer.getNodeId(), raftNode.getCommittedEntries().size());
            int prevLogIndex = nextIdx - 1;
            int prevLogTerm = prevLogIndex >= 0 ? raftNode.getCommittedEntries().get(prevLogIndex).getTerm() : 0;
            List<InternalLogEntry> entriesToReplicate = getEntriesToReplicate(peer.getNodeId(), nextIdx);

            // Send AppendEntries RPC to peer
            sendAppendEntriesRPC(peer, prevLogIndex, prevLogTerm, entriesToReplicate);
        }
    }

    // Get entries that need to be replicated to the follower
    private List<InternalLogEntry> getEntriesToReplicate(String peerId, int nextIndex) {
        List<InternalLogEntry> entries = new ArrayList<>();
        List<InternalLogEntry> allEntries = raftNode.getCommittedEntries();

        // Only send logs starting from the nextIndex position
        for (int i = nextIndex; i < allEntries.size(); i++) {
            entries.add(allEntries.get(i));
        }

        return entries;
    }

    // Simulate sending the AppendEntries RPC
    private void sendAppendEntriesRPC(RaftNode peer, int prevLogIndex, int prevLogTerm, List<InternalLogEntry> entries) {
        // Simulate network delay (e.g., using a sleep)
        try {
            Thread.sleep(200); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Replication interrupted for " + peer.getNodeId());
            return;  // Early exit if interrupted
        }

        // Simulate handling the AppendEntries response from the follower
        handleAppendEntriesResponse(peer, prevLogIndex, prevLogTerm, entries);
    }

    // Handle the response after sending AppendEntries RPC
    private void handleAppendEntriesResponse(RaftNode peer, int prevLogIndex, int prevLogTerm, List<InternalLogEntry> entries) {
        // Simulate a successful replication response from the follower
        boolean success = true; // Simulating success

        if (success) {
            // Update the match index and next index for the peer
            matchIndex.put(peer.getNodeId(), prevLogIndex + entries.size());
            nextIndex.put(peer.getNodeId(), prevLogIndex + entries.size() + 1);
            System.out.println("Logs replicated successfully to " + peer.getNodeId());

            // Commit the log entries if quorum is reached
            commitLogs();
        } else {
            // If replication failed, we need to decrement the next index and retry
            nextIndex.put(peer.getNodeId(), prevLogIndex);
            System.out.println("Log replication failed for " + peer.getNodeId() + ", retrying...");
        }
    }

    // Commit log entries once the majority of peers have replicated them
    private void commitLogs() {
        List<InternalLogEntry> allEntries = raftNode.getCommittedEntries();
        int majority = (peers.size() / 2) + 1;

        // Check if we have a majority of followers who have replicated up to the same index
        for (int i = allEntries.size() - 1; i >= 0; i--) {
            int count = 1; // Leader always commits its own log entry
            for (RaftNode peer : peers) {
                if (matchIndex.getOrDefault(peer.getNodeId(), -1) >= i) {
                    count++;
                }
            }

            // If the majority of nodes have replicated this log entry, commit it
            if (count >= majority) {
                raftNode.setCommitIndex(i);
                System.out.println("Committed log entry at index " + i);
                break;
            }
        }
    }

    // Stop log replication when the node shuts down
    public void stopLogReplication() {
        replicationScheduler.shutdownNow();
    }
}