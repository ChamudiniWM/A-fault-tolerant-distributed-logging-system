package com.dls.raft;

import com.dls.common.NodeInfo;
import com.dls.common.RejectionTracker;
import com.dls.raft.rpc.AppendEntriesRequest;
import java.util.List;
import java.util.concurrent.*;

import com.dls.raft.rpc.RaftGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.Map;

public class RaftNode {

    private final NodeInfo self;
    private final List<NodeInfo> peers;

    private volatile RaftState state;
    private volatile int currentTerm;
    protected volatile String votedFor;

    private final RaftLog raftLog;
    private final LeaderElection leaderElection;
    private final ConsensusModule consensusModule;

    private ScheduledFuture<?> electionTimeoutTask;
    private ScheduledFuture<?> heartbeatTask;

    private static final int HEARTBEAT_INTERVAL = 150; // ms
    private static final int ELECTION_TIMEOUT_MIN = 450; // ms
    private static final int ELECTION_TIMEOUT_MAX = 700; // ms

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Field to cache gRPC stubs
    private final Map<NodeInfo, RaftGrpc.RaftStub> peerStubs = new ConcurrentHashMap<>();


    public RaftNode(NodeInfo self, List<NodeInfo> peers) {
        this.self = self;
        this.peers = peers;
        this.state = RaftState.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = null;

        this.raftLog = new RaftLog();
        this.leaderElection = new LeaderElection(this);
        this.consensusModule = new ConsensusModule(this);

        createGrpcStubs(); // Create gRPC stubs at startup

        System.out.println("RaftNode initialized for node: " + self.getNodeId() + " in state: " + state + " with term: " + currentTerm);
        RejectionTracker.startLogging();
        startElectionTimer();
    }

    public synchronized void becomeFollower(int term) {
        System.out.println("Node " + self.getNodeId() + " transitioning to Follower for term " + term);
        this.state = RaftState.FOLLOWER;
        this.currentTerm = term;
        this.votedFor = null;
        cancelHeartbeatTimer();
        resetElectionTimer();
    }

    public synchronized void becomeCandidate() {
        System.out.println("Node " + self.getNodeId() + " transitioning to Candidate for term " + (currentTerm + 1));
        this.state = RaftState.CANDIDATE;
        this.currentTerm += 1;
        this.votedFor = self.getNodeId();

        leaderElection.startElection();
        resetElectionTimer();
    }

    public synchronized void becomeLeader() {
        System.out.println("Node " + self.getNodeId() + " transitioning to Leader for term " + currentTerm);
        this.state = RaftState.LEADER;
        cancelElectionTimer();
        startHeartbeatTimer();
        sendHeartbeats();
    }

    public synchronized void setVotedFor(String nodeId) {
        System.out.println("Node " + self.getNodeId() + " has voted for: " + nodeId);
        this.votedFor = nodeId;
    }

    public synchronized void handleAppendEntries(AppendEntriesRequest request) {
        System.out.println("Node " + self.getNodeId() + " received AppendEntries for term " + request.getTerm());

        if (request.getTerm() < currentTerm) {
            System.out.println("AppendEntries term " + request.getTerm() + " is stale. Current term is " + currentTerm + ". Ignoring.");
            return;
        }

        if (request.getTerm() > currentTerm) {
            becomeFollower(request.getTerm());
        }

        consensusModule.processAppendEntries(request);
    }

    private synchronized void startElectionTimer() {
        cancelElectionTimer();
        int timeout = randomElectionTimeout();
        electionTimeoutTask = scheduler.schedule(() -> {
            System.out.println("Election timeout for node " + self.getNodeId() + ", starting election");
            if (state != RaftState.LEADER) {
                becomeCandidate();
            }
        }, timeout, TimeUnit.MILLISECONDS);

        System.out.println("Election timer scheduled: " + timeout + "ms for node " + self.getNodeId());
    }

    void resetElectionTimer() {
        startElectionTimer();
    }

    private synchronized void cancelElectionTimer() {
        if (electionTimeoutTask != null) {
            electionTimeoutTask.cancel(false);
            electionTimeoutTask = null;
        }
    }

    private synchronized void startHeartbeatTimer() {
        cancelHeartbeatTimer();
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (state == RaftState.LEADER) {
                sendHeartbeats();
            }
        }, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

        System.out.println("Heartbeat timer scheduled for Leader " + self.getNodeId());
    }

    private synchronized void cancelHeartbeatTimer() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    private void sendHeartbeats() {
        if (state == RaftState.LEADER) {
            System.out.println("Leader " + self.getNodeId() + " sending heartbeats");
            consensusModule.broadcastHeartbeats();
        }
    }

    // Method to create stubs
    private void createGrpcStubs() {
        for (NodeInfo peer : peers) {
            if (!peer.equals(self)) {
                ManagedChannel channel = ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort())
                        .usePlaintext()
                        .build();
                RaftGrpc.RaftStub stub = RaftGrpc.newStub(channel);
                peerStubs.put(peer, stub);
            }
        }
    }

    private int randomElectionTimeout() {
        return ELECTION_TIMEOUT_MIN + (int) (Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN));
    }

    // Getters and Setters
    public NodeInfo getSelf() {
        return self;
    }

    public List<NodeInfo> getPeers() {
        return peers;
    }

    public RaftState getState() {
        return state;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public RaftLog getRaftLog() {
        return raftLog;
    }

    public Map<NodeInfo, RaftGrpc.RaftStub> getPeerStubs() {
        return peerStubs;
    }

    public synchronized void setCurrentTerm(int term) {
        System.out.println("Node " + self.getNodeId() + " setting current term to " + term);
        this.currentTerm = term;
    }

    public synchronized void setRaftState(RaftState state) {
        System.out.println("Node " + self.getNodeId() + " setting state to " + state);
        this.state = state;
    }

    public synchronized int getCommitIndex() {
        return raftLog.getCommitIndex();
    }
}