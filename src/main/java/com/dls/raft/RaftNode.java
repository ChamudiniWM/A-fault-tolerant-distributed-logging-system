package com.dls.raft;

import com.dls.common.LocalLogEntry;
import com.dls.common.NodeInfo;
import com.dls.common.RejectionTracker;
import com.dls.faulttolerance.FailureDetector;
import com.dls.faulttolerance.RedundancyManager;
import com.dls.raft.rpc.*;

import java.util.List;
import java.util.concurrent.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;

import java.util.HashMap;
import java.util.Map;

public class RaftNode {

    private final NodeInfo self;
    private final List<NodeInfo> peers;
    private final boolean enableLogging;
    private final ManagedChannel loggingChannel;
    private final LoggingServiceGrpc.LoggingServiceBlockingStub logStub;
    private volatile RaftState state;
    private volatile int currentTerm;
    protected volatile String votedFor;

    private final RaftLog raftLog;
    private final LeaderElection leaderElection;
    private final ConsensusModule consensusModule;
    private final RedundancyManager redundancyManager;
    private final FailureDetector failureDetector;
    final Map<String, Integer> nextIndex = new HashMap<>();  // Maps peerId to nextIndex
    private final Map<String, Integer> matchIndex = new HashMap<>(); // Maps peerId to matchIndex


    private ScheduledFuture<?> electionTimeoutTask;
    private ScheduledFuture<?> heartbeatTask;

    private static final int HEARTBEAT_INTERVAL = 150; // ms
    private static final int ELECTION_TIMEOUT_MIN = 450; // ms
    private static final int ELECTION_TIMEOUT_MAX = 700; // ms

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Field to cache gRPC stubs
    private final Map<NodeInfo, RaftGrpc.RaftStub> peerStubs = new ConcurrentHashMap<>();
    private Server server;

    public RaftNode(NodeInfo self, List<NodeInfo> peers) {
        this.self = self;
        this.peers = peers;
        this.state = RaftState.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = null;
        this.enableLogging = false;
        this.redundancyManager = new RedundancyManager(this);
        this.failureDetector = new FailureDetector(this);
        this.raftLog = new RaftLog(self.getNodeId());
        this.leaderElection = new LeaderElection(this);
        this.consensusModule = new ConsensusModule(this);

        // Initialize logging channel if enabled
        if (enableLogging) {
            this.loggingChannel = ManagedChannelBuilder.forAddress("localhost", 5000)
                    .usePlaintext()
                    .build();
            this.logStub = LoggingServiceGrpc.newBlockingStub(loggingChannel);
        } else {
            this.loggingChannel = null;
            this.logStub = null;
        }

        // Initialize peer stubs
        for (NodeInfo peer : peers) {
            if (!peer.getNodeId().equals(self.getNodeId())) {
                ManagedChannel channel = ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort())
                        .usePlaintext()
                        .build();
                peerStubs.put(peer, RaftGrpc.newStub(channel));
            }
        }

        if (enableLogging) {
            System.out.println("RaftNode initialized for node: " + self.getNodeId() + " in state: " + state + " with term: " + currentTerm);
            RejectionTracker.startLogging();

        }
        startElectionTimer();




            createGrpcStubs(); // Create gRPC stubs at startup

        for (NodeInfo peer : peers) {
            if (!peer.getNodeId().equals(self.getNodeId())) {
                nextIndex.put(peer.getNodeId(), raftLog.getLastLogIndex() + 1);  // Set to the index after the last log entry
                matchIndex.put(peer.getNodeId(), -1);  // Initially, no logs match (set to -1)
            }
        }


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

    public synchronized void appendEntryToLog(LocalLogEntry entry) {
        // Append to local log
        raftLog.appendEntry(entry, getCurrentTerm());


        // Debug
        System.out.println("Leader " + self.getNodeId() + " appended entry to log: " + entry);

        // Replicate to followers
        if (state == RaftState.LEADER) {
            consensusModule.broadcastAppendEntries();  // This should send log entries to followers
        }
    }

    // Method to advance matchIndex for a given peer
    public synchronized void advanceMatchIndex(String peerId, int index) {
        matchIndex.put(peerId, index);  // Set matchIndex for the peer to the new index
        System.out.println("Advanced matchIndex for " + peerId + " to " + index);
    }

    // Method to decrement nextIndex for a given peer
    public synchronized void decrementNextIndex(String peerId) {
        nextIndex.put(peerId, nextIndex.get(peerId) - 1);  // Decrease the nextIndex for the peer
        System.out.println("Decremented nextIndex for " + peerId);
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
            } else {
                // If we're no longer leader, cancel the heartbeat timer
                cancelHeartbeatTimer();
            }
        }, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

        if (enableLogging) {
            System.out.println("Heartbeat timer scheduled for Leader " + self.getNodeId());
        }
    }


    private synchronized void cancelHeartbeatTimer() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    public void log(String message) {
        if (enableLogging && logStub != null) {
            try {
                logStub.log(LogMessage.newBuilder()
                        .setNodeId(self.getNodeId())
                        .setMessage(message)
                        .build());
            } catch (Exception e) {
                // Suppress logging errors during testing
                if (!enableLogging) {
                    System.err.println("Failed to send log to LoggingServer: " + e.getMessage());
                }
            }
        }
    }

    public synchronized void stopServer() {
        if (enableLogging) {
            System.out.println("Stopping gRPC server for node " + self.getNodeId());
        }
        if (server != null) {
            try {
                server.shutdown().awaitTermination(3, TimeUnit.SECONDS);
                if (enableLogging) {
                    System.out.println("Server stopped for node " + self.getNodeId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (enableLogging) {
                    System.err.println("Interrupted while shutting down server for node " + self.getNodeId());
                }
            }
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

    public synchronized void shutdown() {
        if (enableLogging) {
            System.out.println("Shutting down node " + self.getNodeId());
        }

        // Shutdown all peer gRPC channels
        peerStubs.keySet().forEach(peer -> {
            ((ManagedChannel) peerStubs.get(peer).getChannel()).shutdown();
        });

        cancelElectionTimer();
        cancelHeartbeatTimer();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
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
        if (enableLogging) {
            System.out.println("Node " + self.getNodeId() + " setting state to " + state);
        }
        this.state = state;
        if (state != RaftState.LEADER) {
            cancelHeartbeatTimer();
        }
    }

    public synchronized int getCommitIndex() {
        return raftLog.getCommitIndex();
    }

    public int getNextIndexFor(String peerId) {
        return nextIndex.getOrDefault(peerId, raftLog.getLastLogIndex() + 1); // Default is after the last log
    }

    public int getTermAtIndex(int index) {
        if (index < 0 || index >= raftLog.getLogEntries().size()) {
            return -1;  // Invalid index
        }
        return raftLog.getLogEntries().get(index).getTerm();
    }

    public RedundancyManager getRedundancyManager() {
        return redundancyManager;
    }

    public FailureDetector getFailureDetector() {
        return failureDetector;
    }

    public synchronized void verifyNodeState() {
        if (enableLogging) {
            System.out.println("\n=== Node State Verification ===");
            System.out.println("Node ID: " + self.getNodeId());
            System.out.println("Current State: " + state);
            System.out.println("Current Term: " + currentTerm);
            System.out.println("Voted For: " + votedFor);
            System.out.println("Election Timer Active: " + (electionTimeoutTask != null && !electionTimeoutTask.isDone()));
            System.out.println("Heartbeat Timer Active: " + (heartbeatTask != null && !heartbeatTask.isDone()));
            System.out.println("=== End Verification ===\n");
        }
    }

    public synchronized void checkLogReplication() {
        if (enableLogging) {
            System.out.println("\n=== Manual Log Replication Check ===");
            System.out.println("Node ID: " + self.getNodeId());
            System.out.println("Current State: " + state);
            System.out.println("Current Term: " + currentTerm);

            // Print log entries in a table format
            System.out.println("\nLog Entries:");
            System.out.println("Index\tTerm\tCommand");
            System.out.println("------------------------");

            List<LogEntry> entries = raftLog.getLogEntries();
            for (int i = 0; i < entries.size(); i++) {
                LogEntry entry = entries.get(i);
                System.out.printf("%d\t%d\t%s%n",
                        i,
                        entry.getTerm(),
                        entry.getCommand() != null ? entry.getCommand() : "NO-OP"
                );
            }

            // Print commit and last applied information
            System.out.println("\nCommit Information:");
            System.out.println("Commit Index: " + raftLog.getCommitIndex());
            System.out.println("Last Log Index: " + raftLog.getLastLogIndex());
            System.out.println("Last Log Term: " + raftLog.getLastLogTerm());

            // Print uncommitted entries
            List<LogEntry> uncommitted = raftLog.getUncommittedEntries();
            System.out.println("\nUncommitted Entries: " + uncommitted.size());
            if (!uncommitted.isEmpty()) {
                System.out.println("First uncommitted index: " + (raftLog.getCommitIndex() + 1));
            }

            System.out.println("=== End Log Check ===\n");
        }
    }


    public void setServer(Server server) {
        this.server = server;
    }

    public Server getServer() {
        return server;
    }

}