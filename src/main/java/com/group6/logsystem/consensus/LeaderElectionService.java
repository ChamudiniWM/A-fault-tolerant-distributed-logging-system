package com.group6.logsystem.consensus;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LeaderElectionService {

    private final RaftNode raftNode;
    private final List<RaftNode> peers;
    private final AtomicBoolean electionInProgress = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);  // Thread pool for handling votes

    public LeaderElectionService(RaftNode raftNode, List<RaftNode> peers) {
        this.raftNode = raftNode;
        this.peers = peers;
    }

    // Method to initiate the leader election
    public void startElection() {
        if (electionInProgress.compareAndSet(false, true)) {
            System.out.println(raftNode.getNodeId() + " starting leader election.");
            // Increment current term and vote for self
            raftNode.becomeCandidate();
            // Send RequestVote RPCs to all peers
            sendRequestVoteRPCs();
        }
    }

    // Sends RequestVote RPC to all peers
    private void sendRequestVoteRPCs() {
        int currentTerm = raftNode.getCurrentTerm();
        String candidateId = raftNode.getNodeId();

        // Send RPC to each peer
        for (RaftNode peer : peers) {
            executorService.submit(() -> handleVoteResponse(peer, candidateId, currentTerm));
        }
    }

    // Simulates receiving a vote response from a peer
    private void handleVoteResponse(RaftNode peer, String candidateId, int term) {
        // Simulate some delay as the RPC is being processed
        try {
            Thread.sleep(200); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate vote response based on the peer's state
        if (peer.getCurrentTerm() <= term) {
            System.out.println("Peer " + peer.getNodeId() + " voted for " + candidateId);
            synchronized (this) {
                peer.receiveVoteRequest(candidateId, term); // Update peer's vote status
            }
            onVoteGranted();
        } else {
            System.out.println("Peer " + peer.getNodeId() + " rejected " + candidateId + "'s vote request due to a higher term.");
        }
    }

    // Handles the vote being granted by peers
    private synchronized void onVoteGranted() {
        // If the candidate gets a majority of votes, it becomes the leader
        int voteCount = 0;
        for (RaftNode peer : peers) {
            if (peer.getVotedFor() != null && peer.getVotedFor().equals(raftNode.getNodeId())) {
                voteCount++;
            }
        }

        // Check if this node has received enough votes to become the leader
        if (voteCount >= (peers.size() / 2) + 1) {
            raftNode.becomeLeader();
            electionInProgress.set(false); // Election is finished
            System.out.println(raftNode.getNodeId() + " is elected as the LEADER.");
        }
    }

    // Method to handle the vote request from a candidate
    public synchronized void receiveVoteRequest(String candidateId, int term) {
        if (term > raftNode.getCurrentTerm()) {
            raftNode.becomeFollower(term);  // Convert to follower if we have a higher term
            raftNode.receiveVoteRequest(candidateId, term);
        }
    }

    // Shutdown the executor service when done
    public void shutdown() {
        executorService.shutdown();
    }
}