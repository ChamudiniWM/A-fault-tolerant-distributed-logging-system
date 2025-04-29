package com.dls.raft;

import com.dls.common.NodeInfo;
import com.dls.raft.rpc.RaftGrpc;
import com.dls.raft.rpc.RequestVoteRequest;
import com.dls.raft.rpc.RequestVoteResponse;
import com.dls.raft.RaftNode;
import com.dls.raft.RaftState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import com.dls.raft.rpc.LoggingServiceGrpc;
import com.dls.raft.rpc.LogMessage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderElection {

    private final RaftNode raftNode;
    private final AtomicInteger voteCount;

    public LeaderElection(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.voteCount = new AtomicInteger(0);
    }

    public void startElection() {
        // Before starting election, confirm node is still a Candidate
        if (raftNode.getState() != RaftState.CANDIDATE) {
            System.out.println("Node " + raftNode.getSelf().getNodeId() + " is no longer candidate, cancelling election start.");
            return;
        }

        raftNode.resetElectionTimer();
        System.out.println("Starting election for term " + raftNode.getCurrentTerm());
        voteCount.set(1); // Self-vote
        logToLoggingServer("📢 Starting election for term " + raftNode.getCurrentTerm());


        List<NodeInfo> peers = raftNode.getPeers();
        for (NodeInfo peer : peers) {
            if (peer.getNodeId().equals(raftNode.getSelf().getNodeId())) {
                continue; // Don't send RequestVote to self
            }

            RequestVoteRequest request = RequestVoteRequest.newBuilder()
                    .setCandidateId(raftNode.getSelf().getNodeId())
                    .setTerm(raftNode.getCurrentTerm())
                    .setLastLogIndex(raftNode.getRaftLog().getLastLogIndex())
                    .setLastLogTerm(raftNode.getRaftLog().getLastLogTerm())
                    .build();

            sendRequestVote(peer, request);
        }
    }

    public synchronized void processVoteResponse(RequestVoteResponse response) {
        synchronized (this) {
            // Before processing vote, confirm node is still a Candidate
            if (raftNode.getState() != RaftState.CANDIDATE) {
                System.out.println("Ignoring vote because node is no longer candidate.");
                return;
            }

            if (response.getTerm() > raftNode.getCurrentTerm()) {
                raftNode.becomeFollower(response.getTerm());
                raftNode.resetElectionTimer();
                return;
            }

            if (response.getVoteGranted()) {
                int votes = voteCount.incrementAndGet();
                System.out.println("Node " + raftNode.getSelf().getNodeId() + " received a vote, current votes: " + votes);
                logToLoggingServer("🗳️ Received vote from peer. Total votes: " + votes);


                // Check again if still Candidate before becoming Leader
                if (votes > (raftNode.getPeers().size() + 1) / 2 && raftNode.getState() == RaftState.CANDIDATE) {
                    raftNode.becomeLeader();
                    System.out.println("Node " + raftNode.getSelf().getNodeId() + " became Leader for term " + raftNode.getCurrentTerm());
                    logToLoggingServer("👑 Became Leader for term " + raftNode.getCurrentTerm());
                }
            }
        }
    }

    private void sendRequestVote(NodeInfo peer, RequestVoteRequest request) {
        System.out.println("Attempting to connect to node " + peer.getNodeId() + " at " + peer.getHost() + ":" + peer.getPort());
        System.out.println("Sending RequestVote for term " + request.getTerm() + " to " + peer.getNodeId());

        ManagedChannel channel = ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort())
                .usePlaintext()
                .build();

        RaftGrpc.RaftBlockingStub stub = RaftGrpc.newBlockingStub(channel);

        try {
            RequestVoteResponse response = stub.requestVote(request);
            processVoteResponse(response);
        } catch (StatusRuntimeException e) {
            System.err.println("RPC failed: " + e.getStatus());
        } finally {
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                    channel.shutdown();
                } catch (InterruptedException ignored) {
                }
            }).start();
        }
    }

    private void logToLoggingServer(String message) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50056)
                .usePlaintext()
                .build();

        LoggingServiceGrpc.LoggingServiceBlockingStub stub = LoggingServiceGrpc.newBlockingStub(channel);

        try {
            stub.log(LogMessage.newBuilder()
                    .setNodeId(raftNode.getSelf().getNodeId())
                    .setMessage(message)
                    .build());
        } catch (Exception e) {
            System.err.println("❌ Failed to send log to LoggingServer: " + e.getMessage());
        } finally {
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                    channel.shutdown();
                } catch (InterruptedException ignored) {}
            }).start();
        }
    }


    public RequestVoteResponse processVoteRequest(RequestVoteRequest request) {
        System.out.println("Processing vote request for term " + request.getTerm() + " from " + request.getCandidateId());

        boolean voteGranted = false;

        if (request.getTerm() < raftNode.getCurrentTerm()) {
            System.out.println("Rejecting vote from " + request.getCandidateId() + " for stale term " + request.getTerm());
            logToLoggingServer("❌ Rejected vote from " + request.getCandidateId() + " due to stale term " + request.getTerm());
            voteGranted = false;
        } else {
            // If request term is greater, update our term and become follower
            if (request.getTerm() > raftNode.getCurrentTerm()) {
                raftNode.becomeFollower(request.getTerm());
            }

            if ((raftNode.getVotedFor() == null || raftNode.getVotedFor().equals(request.getCandidateId()))
                    && raftNode.getRaftLog().isUpToDate(request.getLastLogIndex(), request.getLastLogTerm())) {
                System.out.println("Granting vote to " + request.getCandidateId() + " for term " + request.getTerm());
                logToLoggingServer("✅ Granted vote to " + request.getCandidateId() + " for term " + request.getTerm());
                raftNode.setVotedFor(request.getCandidateId());
                raftNode.resetElectionTimer();
                voteGranted = true;
            } else {
                System.out.println("Rejecting vote from " + request.getCandidateId() + " due to log mismatch or already voted for another.");
                logToLoggingServer("❌ Rejected vote from " + request.getCandidateId() + " due to log mismatch or already voted.");
            }
        }

        return RequestVoteResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setVoteGranted(voteGranted)
                .build();
    }

    public void resetElectionTimer() {
        raftNode.resetElectionTimer();
    }

}