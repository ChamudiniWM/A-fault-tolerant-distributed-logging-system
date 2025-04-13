package com.group6.logsystem.consensus;

import com.group6.logsystem.grpc.RaftGrpcClient;
import com.group6.logsystem.grpc.*;
import com.group6.logsystem.models.InternalLogEntry;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RaftNode {

    public enum Role {
        LEADER, FOLLOWER, CANDIDATE
    }

    private static class RaftLogEntry {
        private final int term;
        private final InternalLogEntry entry;

        public RaftLogEntry(int term, InternalLogEntry entry) {
            this.term = term;
            this.entry = entry;
        }

        public int getTerm() {
            return term;
        }

        public InternalLogEntry getEntry() {
            return entry;
        }
    }

    // Persistent state
    private int currentTerm = 0;
    private String votedFor = null;
    private final List<RaftLogEntry> log = new ArrayList<>();

    // Volatile state
    private int commitIndex = 0;
    private int lastApplied = 0;

    private final Set<String> receivedVotes = new HashSet<>();

    private final Map<String, Integer> nextIndex = new ConcurrentHashMap<>();
    private final Map<String, Integer> matchIndex = new ConcurrentHashMap<>();

    private final String nodeId;
    private Role role = Role.FOLLOWER;
    private final List<String> peers;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> electionTimeoutTask;
    private final Random random = new Random();
    private final AtomicBoolean electionInProgress = new AtomicBoolean(false);

    private final Set<String> partitionedPeers = new HashSet<>();
    private final RaftGrpcClient raftGrpcClient;

    public RaftNode(String nodeId, List<String> peers) {
        this.nodeId = nodeId;
        this.peers = peers;
        this.raftGrpcClient = new RaftGrpcClient("localhost", 50051);
        resetElectionTimeout();
    }

    // ------------------- NEW METHOD -------------------
    public synchronized int getLastLogIndex() {
        return log.isEmpty() ? 0 : log.size() - 1;
    }
    // --------------------------------------------------

    public int getNextLogIndex() {
        return log.size();
    }

    public synchronized int getCurrentTerm() {
        return currentTerm;
    }

    public synchronized void setCurrentTerm(int term) {
        this.currentTerm = term;
    }

    public synchronized Role getRole() {
        return role;
    }

    public synchronized void setRole(Role role) {
        this.role = role;
    }

    public synchronized String getVotedFor() {
        return votedFor;
    }

    public synchronized void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<String> getPeers() {
        return peers;
    }

    public synchronized void appendLogEntry(InternalLogEntry entry) {
        if (this.role != Role.LEADER) {
            System.out.println(nodeId + " is not the leader. Rejecting log append.");
            return;
        }
        log.add(new RaftLogEntry(currentTerm, entry));
    }

    public synchronized int getCommitIndex() {
        return commitIndex;
    }

    public synchronized void setCommitIndex(int index) {
        if (index > commitIndex) {
            commitIndex = Math.min(index, log.size());
            System.out.println("Commit index updated to: " + commitIndex);

            for (int i = lastApplied; i < commitIndex; i++) {
                System.out.println("Applying entry at index " + i + ": " + log.get(i).getEntry().getMessage());
            }
            lastApplied = commitIndex;
        }
    }

    public synchronized List<InternalLogEntry> getCommittedEntries() {
        List<InternalLogEntry> committed = new ArrayList<>();
        for (int i = 0; i < commitIndex; i++) {
            committed.add(log.get(i).getEntry());
        }
        return committed;
    }

    public synchronized List<InternalLogEntry> getUncommittedEntries() {
        List<InternalLogEntry> uncommitted = new ArrayList<>();
        for (int i = commitIndex; i < log.size(); i++) {
            uncommitted.add(log.get(i).getEntry());
        }
        return uncommitted;
    }

    public synchronized void becomeFollower(int term) {
        this.role = Role.FOLLOWER;
        this.currentTerm = term;
        this.votedFor = null;
        electionInProgress.set(false);

        if (electionTimeoutTask != null) {
            electionTimeoutTask.cancel(false);
        }

        resetElectionTimeout();
        System.out.println(nodeId + " became FOLLOWER in term " + term);
    }

    public synchronized void becomeCandidate() {
        this.role = Role.CANDIDATE;
        this.currentTerm += 1;
        this.votedFor = nodeId;
        electionInProgress.set(true);
        resetElectionTimeout();
        System.out.println(nodeId + " became CANDIDATE for term " + currentTerm);
        startElection();
    }

    public synchronized void becomeLeader() {
        this.role = Role.LEADER;
        electionInProgress.set(false);
        System.out.println(nodeId + " became LEADER for term " + currentTerm);
        for (String peer : peers) {
            nextIndex.put(peer, log.size());
            matchIndex.put(peer, 0);
        }
        startHeartbeats();
    }

    private void resetElectionTimeout() {
        if (role == Role.LEADER) return;

        if (electionTimeoutTask != null) {
            electionTimeoutTask.cancel(false);
        }

        int timeout = 150 + random.nextInt(150);
        electionTimeoutTask = scheduler.schedule(this::onElectionTimeout, timeout, TimeUnit.MILLISECONDS);
    }

    private void onElectionTimeout() {
        if (!electionInProgress.get()) {
            becomeCandidate();
        }
    }

    private void startElection() {
        currentTerm++;
        role = Role.CANDIDATE;
        votedFor = nodeId;

        receivedVotes.clear();
        receivedVotes.add(nodeId);

        System.out.println(nodeId + " is requesting votes for term " + currentTerm);

        for (String peerId : peers) {
            RequestVoteRequest requestVoteRequest = RequestVoteRequest.newBuilder()
                    .setTerm(currentTerm)
                    .setCandidateId(nodeId)
                    .setLastLogIndex(getLastLogIndex())
                    .setLastLogTerm(log.isEmpty() ? 0 : log.get(log.size() - 1).getTerm())
                    .build();

            raftGrpcClient.sendRequestVote(requestVoteRequest, new StreamObserver<RequestVoteResponse>() {
                @Override
                public void onNext(RequestVoteResponse value) {
                    if (value.getVoteGranted()) {
                        System.out.println(nodeId + " received vote from " + peerId);
                        receivedVotes.add(peerId);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("Error while requesting vote from " + peerId + ": " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("Completed RequestVote RPC to " + peerId);
                }
            });
        }
    }

    public synchronized void receiveVoteRequest(String candidateId, int term) {
        if (term > currentTerm) {
            becomeFollower(term);
            votedFor = candidateId;
        }
    }

    private void startHeartbeats() {
        scheduler.scheduleAtFixedRate(() -> {
            if (role == Role.LEADER) {
                sendHeartbeats();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    public void sendHeartbeats() {
        int successfulHeartbeats = 0;

        for (String peer : peers) {
            System.out.println(nodeId + " sending heartbeat to " + peer);

            if (isPartitioned(peer)) {
                System.out.println(nodeId + " cannot send heartbeat to " + peer + " due to partition");
            } else {
                successfulHeartbeats++;
            }
        }

        int totalNodes = peers.size() + 1;
        int majority = (totalNodes / 2) + 1;

        if ((successfulHeartbeats + 1) >= majority) {
            System.out.println(nodeId + " is still the leader.");
        } else {
            System.out.println(nodeId + " lost majority, becoming candidate.");
            becomeCandidate();
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public void start() {
        System.out.println("Starting Raft Node: " + nodeId);
        resetElectionTimeout();
        if (getRole() == Role.LEADER) {
            startHeartbeats();
        }
    }

    private boolean isPartitioned(String peer) {
        return partitionedPeers.contains(peer);
    }

    public void addPartitionedPeer(String peerId) {
        partitionedPeers.add(peerId);
    }

    public void removePartitionedPeer(String peerId) {
        partitionedPeers.remove(peerId);
    }

    public void disableElectionTimersForTesting() {
        if (electionTimeoutTask != null) {
            electionTimeoutTask.cancel(false);
        }
        scheduler.shutdownNow();
    }
}