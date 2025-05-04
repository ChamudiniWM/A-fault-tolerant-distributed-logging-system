package raft.core;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import logging.*;
import raft.model.LogEntryPOJO;
import raft.model.RaftState;
import raft.persistence.RaftPersistence;
import util.TimeSyncUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RaftNode {
    private final int port;
    private final String nodeId;
    private final List<String> peers;
    private final RaftPersistence persistence;
    private RaftRole role = RaftRole.FOLLOWER;
    private int currentTerm = 0;
    private String votedFor = null;
    private final Random random = new Random();
    private final Object lock = new Object();
    private int votesReceived = 0;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService replicationExecutor = Executors.newFixedThreadPool(4); // For async replication
    private ScheduledFuture<?> electionTimeoutTask;
    private List<LogEntry> log = new ArrayList<>();
    private int commitIndex = 0;
    private int lastApplied = 0;
    private final Map<String, Integer> nextIndex = new HashMap<>();
    private final Map<String, Integer> matchIndex = new HashMap<>();
    private final Map<String, String> idToLog = new ConcurrentHashMap<>();
    private final NavigableMap<Long, String> timestampToId = new ConcurrentSkipListMap<>();
    private final Set<String> seenLogIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final PriorityQueue<LogEntry> reorderingBuffer = new PriorityQueue<>(
            Comparator.comparingLong(LogEntry::getTimestamp)
    );
    private String leaderHost;
    private int leaderPort;
    private static final int MAX_BATCH_SIZE = 100; // Limit entries per AppendEntries RPC

    public RaftNode(int port, List<String> peers, RaftPersistence persistence) {
        this.port = port;
        this.nodeId = "localhost:" + port;
        this.peers = peers;
        this.persistence = persistence;
        loadState();
    }

    public void start() {
        resetElectionTimeout();
        System.out.println("Election Timeout Initialized\n");
    }

    public void stop() {
        saveState();
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (replicationExecutor != null) {
            replicationExecutor.shutdown();
        }
    }

    private void loadState() {
        try {
            List<LogEntryPOJO> logPOJO = persistence.loadLog();
            log = logPOJO.stream().map(LogEntryPOJO::toProto).collect(Collectors.toList());
            RaftState state = persistence.loadState();
            if (state != null) {
                currentTerm = state.getCurrentTerm();
                votedFor = state.getVotedFor();
                commitIndex = state.getCommitIndex();
                lastApplied = state.getLastApplied();
                seenLogIds.addAll(state.getSeenLogIds());
            }
            System.out.println("\n=== State Loaded ===");
            System.out.println("Loaded state: logSize=" + log.size() + ", currentTerm=" + currentTerm + ", commitIndex=" + commitIndex);
        } catch (Exception e) {
            System.err.println("Failed to load state: " + e.getMessage());
            log = new ArrayList<>();
            seenLogIds.clear();
        }
    }

    public void saveState() {
        try {
            List<LogEntryPOJO> logPOJO = log.stream().map(LogEntryPOJO::fromProto).collect(Collectors.toList());
            RaftState state = new RaftState(currentTerm, votedFor, commitIndex, lastApplied, new ArrayList<>(seenLogIds));
            persistence.saveLog(logPOJO);
            persistence.saveState(state);
            System.out.println("\n=== State Saved ===");
            System.out.println("Saved state: logSize=" + log.size() + ", currentTerm=" + currentTerm + ", commitIndex=" + commitIndex);
        } catch (Exception e) {
            System.err.println("Failed to save state: " + e.getMessage());
        }
    }

    private void resetElectionTimeout() {
        if (electionTimeoutTask != null && !electionTimeoutTask.isCancelled()) {
            electionTimeoutTask.cancel(true);
        }
        int timeout = random.nextInt(2000) + 3000;
        electionTimeoutTask = scheduler.schedule(this::startElection, timeout, TimeUnit.MILLISECONDS);
    }

    private void cancelElectionTimeout() {
        if (electionTimeoutTask != null && !electionTimeoutTask.isCancelled()) {
            electionTimeoutTask.cancel(true);
        }
    }

    private void startElection() {
        synchronized (lock) {
            role = RaftRole.CANDIDATE;
            currentTerm++;
            votedFor = nodeId;
            votesReceived = 1;

            System.out.println("\n=== Election Started ===");
            System.out.println("Node: " + nodeId);
            System.out.println("Term: " + currentTerm);
            System.out.println("Role: " + role + "\n");

            int lastLogIndex = log.size() - 1;
            int lastLogTerm = (lastLogIndex >= 0) ? log.get(lastLogIndex).getTerm() : 0;

            for (String peer : peers) {
                new Thread(() -> {
                    try {
                        String[] parts = peer.split(":");
                        ManagedChannel channel = ManagedChannelBuilder.forAddress(parts[0], Integer.parseInt(parts[1]))
                                .usePlaintext()
                                .build();
                        LogServiceGrpc.LogServiceBlockingStub stub = LogServiceGrpc.newBlockingStub(channel);

                        VoteRequest request = VoteRequest.newBuilder()
                                .setTerm(currentTerm)
                                .setCandidateId(nodeId)
                                .setLastLogIndex(lastLogIndex)
                                .setLastLogTerm(lastLogTerm)
                                .build();

                        VoteResponse response = stub.requestVote(request);

                        synchronized (lock) {
                            if (response.getVoteGranted()) {
                                votesReceived++;
                                System.out.println("\n=== Vote Received ===");
                                System.out.println("Node: " + nodeId);
                                System.out.println("Vote from: " + peer);
                                System.out.println("Total Votes: " + votesReceived);
                                System.out.println("Term: " + currentTerm + "\n");

                                if (votesReceived > (peers.size() + 1) / 2 && role == RaftRole.CANDIDATE) {
                                    role = RaftRole.LEADER;
                                    leaderPort = this.port;
                                    leaderHost = "localhost";
                                    System.out.println("\n=== Leadership Achieved ===");
                                    System.out.println("Node: " + nodeId);
                                    System.out.println("Role: " + role);
                                    System.out.println("Term: " + currentTerm + "\n");
                                    cancelElectionTimeout();
                                    for (String p : peers) {
                                        nextIndex.put(p, log.size());
                                        matchIndex.put(p, -1);
                                    }

                                    startHeartbeatThread();
                                    notifyLeaderChange();
                                }
                            } else if (response.getTerm() > currentTerm) {
                                currentTerm = response.getTerm();
                                role = RaftRole.FOLLOWER;
                                votedFor = null;
                                resetElectionTimeout();
                                System.out.println("\n=== Role Change ===");
                                System.out.println("Node: " + nodeId);
                                System.out.println("New Role: " + role);
                                System.out.println("New Term: " + currentTerm + "\n");
                            }
                        }

                        channel.shutdownNow();
                    } catch (Exception e) {
                        System.err.println("\n=== Election Error ===");
                        System.err.println("Node: " + nodeId);
                        System.err.println("Failed to contact peer: " + peer);
                        System.err.println("Error: " + e.getMessage() + "\n");
                    }
                }).start();
            }
        }
    }

    public void startHeartbeatThread() {
        scheduler.scheduleAtFixedRate(() -> {
            if (role != RaftRole.LEADER) {
                return;
            }

            for (String peer : peers) {
                replicationExecutor.submit(() -> replicateToPeer(peer));
            }

            // Update commitIndex based on majority matchIndex
            List<Integer> matchIndexes = new ArrayList<>(matchIndex.values());
            matchIndexes.add(log.size() - 1);
            Collections.sort(matchIndexes);

            int majorityMatchIndex = matchIndexes.get(matchIndexes.size() / 2);
            System.out.println("\n=== Quorum Status ===");
            System.out.println("Leader: " + nodeId);
            System.out.println("Match Indexes: " + matchIndexes);
            System.out.println("Majority Match Index: " + majorityMatchIndex + "\n");

            if (majorityMatchIndex > commitIndex && log.size() > majorityMatchIndex) {
                commitIndex = majorityMatchIndex;
                System.out.println("\n=== Commit Index Updated ===");
                System.out.println("Leader: " + nodeId);
                System.out.println("New Commit Index: " + commitIndex);
                System.out.println("Quorum Confirmed\n");
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void replicateToPeer(String peer) {
        try {
            String[] parts = peer.split(":");
            ManagedChannel channel = ManagedChannelBuilder.forAddress(parts[0], Integer.parseInt(parts[1]))
                    .usePlaintext()
                    .build();
            LogServiceGrpc.LogServiceBlockingStub stub = LogServiceGrpc.newBlockingStub(channel);

            if (!nextIndex.containsKey(peer)) {
                try {
                    GetLogInfoResponse logInfo = stub.getLogInfo(GetLogInfoRequest.newBuilder().build());
                    int lastLogIndex = logInfo.getLastLogIndex();
                    nextIndex.put(peer, lastLogIndex + 1);
                    matchIndex.put(peer, lastLogIndex);
                    System.out.println("\n=== Follower Log Info ===");
                    System.out.println("Leader: " + nodeId);
                    System.out.println("Peer: " + peer);
                    System.out.println("Last Log Index: " + lastLogIndex);
                    System.out.println("Initialized nextIndex: " + (lastLogIndex + 1) + "\n");
                } catch (Exception e) {
                    System.err.println("Failed to get LogInfo from peer: " + peer + ": " + e.getMessage());
                    nextIndex.put(peer, log.size());
                    matchIndex.put(peer, -1);
                }
            }

            int nextIdx = nextIndex.getOrDefault(peer, log.size());
            int prevLogIndex = nextIdx - 1;
            int prevLogTerm = 0;
            if (prevLogIndex >= 0 && log.size() > prevLogIndex) {
                prevLogTerm = log.get(prevLogIndex).getTerm();
            }

            // Batch entries, limit to MAX_BATCH_SIZE
            int endIdx = Math.min(nextIdx + MAX_BATCH_SIZE, log.size());
            List<LogEntry> entriesToSend = nextIdx < log.size() ? log.subList(nextIdx, endIdx) : Collections.emptyList();

            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .setTerm(currentTerm)
                    .setLeaderId(nodeId)
                    .setPrevLogIndex(prevLogIndex)
                    .setPrevLogTerm(prevLogTerm)
                    .addAllEntries(entriesToSend)
                    .setLeaderCommit(commitIndex)
                    .build();

            AppendEntriesResponse response = stub.appendEntries(request);
            System.out.println("\n=== Heartbeat Sent ===");
            System.out.println("Leader: " + nodeId);
            System.out.println("Sent to: " + peer);
            System.out.println("Term: " + currentTerm + "\n");

            synchronized (lock) {
                if (response.getSuccess()) {
                    if (!entriesToSend.isEmpty()) {
                        int newMatchIndex = prevLogIndex + entriesToSend.size();
                        int currentMatchIndex = matchIndex.getOrDefault(peer, -1);

                        if (newMatchIndex > currentMatchIndex) {
                            matchIndex.put(peer, newMatchIndex);
                            nextIndex.put(peer, newMatchIndex + 1);
                            System.out.println("\n=== Acknowledgment Received ===");
                            System.out.println("Leader: " + nodeId);
                            System.out.println("From: " + peer);
                            System.out.println("Index: " + newMatchIndex);
                            System.out.println("Term: " + currentTerm + "\n");
                        }
                    } else {
                        matchIndex.putIfAbsent(peer, prevLogIndex);
                        nextIndex.putIfAbsent(peer, log.size());
                        System.out.println("\n=== Heartbeat Acknowledged ===");
                        System.out.println("Leader: " + nodeId);
                        System.out.println("From: " + peer);
                        System.out.println("Term: " + currentTerm + "\n");
                    }
                } else if (response.getTerm() > currentTerm) {
                    currentTerm = response.getTerm();
                    role = RaftRole.FOLLOWER;
                    votedFor = null;
                    resetElectionTimeout();
                    System.out.println("\n=== Role Change ===");
                    System.out.println("Node: " + nodeId);
                    System.out.println("New Role: " + role);
                    System.out.println("New Term: " + currentTerm + "\n");
                } else {
                    // Binary search to find correct nextIndex
                    int currentNext = nextIndex.getOrDefault(peer, log.size());
                    int low = 0;
                    int high = currentNext - 1;
                    while (low <= high) {
                        int mid = (low + high) / 2;
                        int midTerm = mid >= 0 && mid < log.size() ? log.get(mid).getTerm() : 0;
                        if (midTerm == prevLogTerm) {
                            low = mid + 1;
                        } else {
                            high = mid - 1;
                        }
                    }
                    nextIndex.put(peer, low);
                    System.out.println("\n=== AppendEntries Failed ===");
                    System.out.println("Leader: " + nodeId);
                    System.out.println("Peer: " + peer);
                    System.out.println("New nextIndex: " + low + "\n");
                }
            }

            channel.shutdown();
        } catch (Exception e) {
            System.err.println("\n=== Heartbeat Error ===");
            System.err.println("Leader: " + nodeId);
            System.err.println("Failed to contact peer: " + peer);
            System.err.println("Error: " + e.getMessage() + "\n");
            synchronized (lock) {
                nextIndex.remove(peer);
                matchIndex.remove(peer);
            }
        }
    }

    public void requestVote(VoteRequest request, StreamObserver<VoteResponse> responseObserver) {
        synchronized (lock) {
            boolean voteGranted = false;
            if (request.getTerm() > currentTerm) {
                currentTerm = request.getTerm();
                votedFor = null;
                role = RaftRole.FOLLOWER;
            }
            if ((votedFor == null || votedFor.equals(request.getCandidateId())) && request.getTerm() >= currentTerm) {
                voteGranted = true;
                votedFor = request.getCandidateId();
                resetElectionTimeout();
                System.out.println("\n=== Vote Granted ===");
                System.out.println("Node: " + nodeId);
                System.out.println("Voted for: " + request.getCandidateId());
                System.out.println("Term: " + currentTerm + "\n");
            }
            VoteResponse response = VoteResponse.newBuilder()
                    .setTerm(currentTerm)
                    .setVoteGranted(voteGranted)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        synchronized (lock) {
            boolean success = false;

            if (request.getTerm() >= currentTerm) {
                if (request.getTerm() > currentTerm) {
                    currentTerm = request.getTerm();
                    votedFor = null;
                    saveState();
                }
                role = RaftRole.FOLLOWER;
                resetElectionTimeout();

                int prevLogIndex = request.getPrevLogIndex();
                int prevLogTerm = request.getPrevLogTerm();

                boolean logMatches = false;
                if (prevLogIndex == -1) {
                    logMatches = true;
                } else if (log.size() > prevLogIndex) {
                    if (log.get(prevLogIndex).getTerm() == prevLogTerm) {
                        logMatches = true;
                    }
                }

                if (logMatches) {
                    List<LogEntry> entries = request.getEntriesList();
                    boolean entriesAppended = appendNewEntries(prevLogIndex, entries);

                    if (request.getLeaderCommit() > commitIndex) {
                        commitIndex = Math.min(request.getLeaderCommit(), log.size() - 1);
                        saveState();
                        System.out.println("\n=== Follower Commit Update ===");
                        System.out.println("Node: " + nodeId);
                        System.out.println("Commit Index: " + commitIndex);
                        System.out.println("Last Applied: " + lastApplied + "\n");
                    }

                    success = true;
                    System.out.println("\n=== Entries Processed ===");
                    System.out.println("Node: " + nodeId);
                    System.out.println("From Leader: " + request.getLeaderId());
                    System.out.println("Type: " + (entries.isEmpty() ? "Heartbeat" : "Log Entries") + "\n");

                    while (lastApplied < commitIndex) {
                        lastApplied++;
                        LogEntry entryToApply = log.get(lastApplied);
                        reorderingBuffer.add(entryToApply);
                    }
                    flushReorderingBuffer();

                    if (entriesAppended) {
                        System.out.println("\n=== New Entries Appended ===");
                        System.out.println("Follower: " + nodeId);
                        System.out.println("Entries:");
                        for (LogEntry entry : entries) {
                            System.out.println("  - Command: " + entry.getCommand());
                            System.out.println("    ID: " + entry.getLogId());
                            reorderingBuffer.add(entry);
                            flushReorderingBuffer();
                        }
                        System.out.println();
                    } else {
                        System.out.println("\n=== No Entries Appended ===");
                        System.out.println("Node: " + nodeId);
                        System.out.println("Reason: Heartbeat Only\n");
                    }
                } else {
                    System.out.println("\n=== Log Inconsistency ===");
                    System.out.println("Node: " + nodeId);
                    System.out.println("Leader: " + request.getLeaderId());
                    System.out.println("Reason: Log mismatch at index " + prevLogIndex + "\n");
                }
            }

            AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                    .setTerm(currentTerm)
                    .setSuccess(success)
                    .build();
            System.out.println("\n=== Acknowledgment Sent ===");
            System.out.println("Follower: " + nodeId);
            System.out.println("To Leader: " + request.getLeaderId());
            System.out.println("Success: " + success + "\n");

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    private void flushReorderingBuffer() {
        long now = System.currentTimeMillis();
        long delayThreshold = 100;

        System.out.println("\n=== Reordering Buffer Flush ===");
        System.out.println("Node: " + nodeId);
        System.out.println("Current Time: " + now);

        while (!reorderingBuffer.isEmpty()) {
            LogEntry entry = reorderingBuffer.peek();
            if (now - entry.getTimestamp() >= delayThreshold) {
                reorderingBuffer.poll();
                if (seenLogIds.add(entry.getLogId())) {
                    applyToStateMachine(entry);
                }
            } else {
                break;
            }
        }

        System.out.println("Remaining Entries in Buffer:");
        for (LogEntry e : reorderingBuffer) {
            System.out.println("  - ID: " + e.getLogId());
            System.out.println("    Timestamp: " + e.getTimestamp());
        }
        System.out.println();
    }

    private boolean appendNewEntries(int prevLogIndex, List<LogEntry> entries) {
        int index = prevLogIndex + 1;
        boolean entriesAppended = false;

        for (int i = 0; i < entries.size(); i++) {
            LogEntry incomingEntry = entries.get(i);

            if (log.size() > index) {
                if (log.get(index).getTerm() != incomingEntry.getTerm()) {
                    log = new ArrayList<>(log.subList(0, index));
                    log.add(incomingEntry);
                    entriesAppended = true;
                }
            } else {
                log.add(incomingEntry);
                entriesAppended = true;
            }

            index++;
        }
        if (entriesAppended) {
            saveState();
        }

        return entriesAppended;
    }

    private void applyToStateMachine(LogEntry logEntry) {
        String message = logEntry.getCommand();
        String logId = logEntry.getLogId();
        long timestamp = logEntry.getTimestamp();

        idToLog.put(logId, message);
        timestampToId.put(timestamp, logId);

        System.out.println("\n=== State Machine Update ===");
        System.out.println("Node: " + nodeId);
        System.out.println("Log ID: " + logId);
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Message: " + message + "\n");
        saveState();
    }

    public void notifyLeaderChange() {
        for (String peer : peers) {
            try {
                String[] parts = peer.split(":");
                ManagedChannel channel = ManagedChannelBuilder.forAddress(parts[0], Integer.parseInt(parts[1])).usePlaintext().build();
                LogServiceGrpc.LogServiceBlockingStub stub = LogServiceGrpc.newBlockingStub(channel);
                LeaderUpdateRequest request = LeaderUpdateRequest.newBuilder()
                        .setLeaderHost("localhost")
                        .setLeaderPort(this.port)
                        .build();
                stub.notifyLeaderUpdate(request);
                System.out.println("\n=== Leader Update Notification Sent ===");
                channel.shutdown();
            } catch (Exception e) {
                System.err.println("\n=== Leader Update Notification Failed ===" + e.getMessage());
            }
        }
    }

    public void appendClientCommand(String command, String logId, long timestamp) {
        synchronized (lock) {
            if (role != RaftRole.LEADER) {
                throw new IllegalStateException("Not the leader");
            }
            LogEntry entry = LogEntry.newBuilder()
                    .setCommand(command)
                    .setTerm(currentTerm)
                    .setIndex(log.size())
                    .setTimestamp(timestamp)
                    .setLogId(logId)
                    .build();
            log.add(entry);
            saveState();
            System.out.println("Leader " + nodeId + " appended command to log: " + command);
        }
    }

    public RaftRole getRole() {
        return role;
    }

    public Object getLock() {
        return lock;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public List<LogEntry> getLog() {
        return log;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public int getLastApplied() {
        return lastApplied;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getLeaderHost() {
        return leaderHost != null ? leaderHost : "localhost";
    }

    public Integer getLeaderPort() {
        return leaderPort;
    }

    public void setLeaderHost(String leaderHost) {
        this.leaderHost = leaderHost;
    }

    public void setLeaderPort(int leaderPort) {
        this.leaderPort = leaderPort;
    }
}