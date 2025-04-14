package com.group6.logsystem.consensus;

import com.group6.logsystem.models.InternalLogEntry;
import com.group6.logsystem.util.LogEntryConverter;
import com.group6.logsystem.grpc.LogServiceGrpc;
import com.group6.logsystem.grpc.LogEntry;
import com.group6.logsystem.grpc.LogRequest;
import com.group6.logsystem.grpc.LogResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConsensusModule {

    private final RaftNode raftNode;
    private final LogServiceGrpc.LogServiceStub logServiceStub;

    public ConsensusModule(RaftNode raftNode, String peerAddress) {
        this.raftNode = raftNode;
        ManagedChannel channel = ManagedChannelBuilder.forTarget(peerAddress).usePlaintext().build();
        this.logServiceStub = LogServiceGrpc.newStub(channel);
    }

    public void start() {
        raftNode.start();
        if (raftNode.getRole() == RaftNode.Role.LEADER) {
            initiateLogReplication();
        }
    }

    private void initiateLogReplication() {
        if (raftNode.getRole() != RaftNode.Role.LEADER) {
            return;
        }

        List<InternalLogEntry> logEntries = raftNode.getCommittedEntries();
        for (InternalLogEntry logEntry : logEntries) {
            replicateLogToPeers(logEntry);
        }
    }

    private void replicateLogToPeers(InternalLogEntry logEntry) {
        LogEntry protoLogEntry = LogEntryConverter.toProto(logEntry);

        for (String peer : raftNode.getPeers()) {
            LogRequest request = LogRequest.newBuilder()
                    .setNodeId(raftNode.getNodeId())
                    .setMessage(logEntry.getMessage())
                    .setTimestamp(logEntry.getTimestamp())
                    .build();

            logServiceStub.sendLog(request, new StreamObserver<LogResponse>() {
                @Override
                public void onNext(LogResponse response) {
                    if (response.getSuccess()) {
                        System.out.println("Log entry replicated to " + peer);
                    } else {
                        System.err.println("Failed to replicate log entry to " + peer);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Error while replicating log entry to " + peer + ": " + throwable.getMessage());
                }

                @Override
                public void onCompleted() {
                    // Handle completion (optional)
                }
            });
        }
    }

    public void receiveLog(LogRequest request) {
        if (raftNode.getRole() == RaftNode.Role.FOLLOWER) {
            int nextIndex = raftNode.getNextLogIndex(); // or use log.size() or similar
            InternalLogEntry entry = new InternalLogEntry(
                    nextIndex,
                    request.getNodeId(),
                    request.getMessage(),
                    request.getTimestamp(),
                    0
            );
            raftNode.appendLogEntry(entry);
            System.out.println("Log entry received and appended by " + raftNode.getNodeId());
        } else {
            System.err.println("Only FOLLOWER nodes can receive logs.");
        }
    }


    public void onElectionTimeout() {
        if (raftNode.getRole() != RaftNode.Role.LEADER) {
            raftNode.becomeCandidate();
            initiateVoteRequest();
        }
    }

    private void initiateVoteRequest() {
        if (raftNode.getRole() != RaftNode.Role.CANDIDATE) {
            return;
        }

        for (String peer : raftNode.getPeers()) {
            LogRequest request = LogRequest.newBuilder()
                    .setNodeId(raftNode.getNodeId())
                    .setMessage("Vote Request for Term " + raftNode.getCurrentTerm())
                    .build();

            logServiceStub.sendLog(request, new StreamObserver<LogResponse>() {
                @Override
                public void onNext(LogResponse response) {
                    if (response.getSuccess()) {
                        System.out.println("Vote granted by " + peer);
                    } else {
                        System.err.println("Vote denied by " + peer);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("Error while sending vote request: " + throwable.getMessage());
                }

                @Override
                public void onCompleted() {
                    // Handle completion (optional)
                }
            });
        }
    }

    private final Set<String> receivedVotes = new HashSet<>();

    public void acknowledgeVote(String candidateId, int term) {
        if (raftNode.getRole() == RaftNode.Role.CANDIDATE && term == raftNode.getCurrentTerm()) {
            if (receivedVotes.add(candidateId)) {
                System.out.println("Vote granted to " + candidateId + " for term " + term);
            }

            int totalVotes = receivedVotes.size();
            int majority = (raftNode.getPeers().size() + 1) / 2 + 1;

            if (totalVotes >= majority) {
                raftNode.becomeLeader();
            }
        }
    }



    public void shutdown() {
        raftNode.shutdown();
        System.out.println("Consensus Module Shutdown.");
    }

    public String getNodeStatus() {
        return "Node ID: " + raftNode.getNodeId() + ", Role: " + raftNode.getRole() + ", Current Term: " + raftNode.getCurrentTerm();
    }

    public void stop() {
        shutdown();
    }

    public void handleFailover() {
        System.out.println("Handling failover: forcing node to follower.");
        raftNode.becomeFollower(raftNode.getCurrentTerm());
    }

    public void updateLeaderStatus(boolean isLeader) {
        if (isLeader) {
            raftNode.becomeLeader(); // Transition to leader
        } else {
            raftNode.becomeFollower(raftNode.getCurrentTerm()); // Transition to follower with the current term
        }
    }

    // New method to handle client log request
    public void handleClientLogRequest(InternalLogEntry entry) {
        if (raftNode.getRole() != RaftNode.Role.LEADER) {
            System.out.println("Rejected log entry: " + raftNode.getNodeId() + " is not the leader.");
            return;
        }

        // Append the log entry locally (Raft's leader first appends the log)
        raftNode.appendLogEntry(entry);
        System.out.println("Log entry appended by leader: " + entry);

        // Replicate the log to followers (log replication)
        initiateLogReplication();
    }
}