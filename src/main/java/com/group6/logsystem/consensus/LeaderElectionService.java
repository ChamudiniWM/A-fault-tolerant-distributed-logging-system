package com.group6.logsystem.consensus;

import com.group6.logsystem.grpc.RequestVoteRequest;
import com.group6.logsystem.grpc.RequestVoteResponse;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LeaderElectionService {

    private final RaftNode raftNode;
    private AtomicBoolean electionInProgress = new AtomicBoolean(false);
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    public LeaderElectionService(RaftNode raftNode, List<RaftNode> peerRaftNodes) {
        this.raftNode = raftNode;
        this.electionInProgress = new AtomicBoolean(false);  // Initializes the election flag as false
        this.executorService = Executors.newFixedThreadPool(5); // Creates a thread pool with 5 threads for handling tasks
    }

    // Initiate leader election
    public void startElection() {
        if (!electionInProgress.compareAndSet(false, true)) {
            System.out.println(raftNode.getNodeId() + ": Election already in progress, skipping...");
            return;
        }

        raftNode.setCurrentTerm(raftNode.getCurrentTerm() + 1);
        raftNode.setRole(RaftNode.Role.CANDIDATE);
        raftNode.setVotedFor(raftNode.getNodeId());
        raftNode.receivedVotes.clear();
        raftNode.receivedVotes.add(raftNode.getNodeId());

        int currentTerm = raftNode.getCurrentTerm();
        int majority = (raftNode.getPeers().size() + 1) / 2 + 1;

        System.out.println(raftNode.getNodeId() + " started election for term " + currentTerm);

        for (String peerId : raftNode.getPeers()) {
            final String targetPeerId = peerId;  // capture effectively final variable for use in anonymous class

            RequestVoteRequest requestVoteRequest = RequestVoteRequest.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setCandidateId(raftNode.getNodeId())
                    .setLastLogIndex(raftNode.getLastLogIndex())
                    .setLastLogTerm(raftNode.getLog().isEmpty() ? 0 :
                            raftNode.getLog().get(raftNode.getLog().size() - 1).getTerm())
                    .build();

            raftNode.raftGrpcClient.sendRequestVote(targetPeerId, requestVoteRequest, new StreamObserver<RequestVoteResponse>() {
                @Override
                public void onNext(RequestVoteResponse response) {
                    if (response.getVoteGranted()) {
                        System.out.println(raftNode.getNodeId() + " received vote from " + targetPeerId);
                        raftNode.receivedVotes.add(targetPeerId);

                        int majority = (raftNode.getPeers().size() + 1) / 2 + 1;
                        if (raftNode.receivedVotes.size() >= majority && raftNode.getRole() == RaftNode.Role.CANDIDATE) {
                            raftNode.onRoleTransition(RaftNode.Role.LEADER);
                            electionInProgress.set(false);
                            System.out.println(raftNode.getNodeId() + " is elected as the LEADER.");
                        }
                    } else {
                        System.out.println(raftNode.getNodeId() + " was denied vote by " + targetPeerId);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("Error while requesting vote from " + targetPeerId + ": " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("Completed RequestVote RPC to " + targetPeerId);
                }
            });
        }
    }

    // Handle incoming vote request (called by RaftNodeServer)
    public synchronized void receiveVoteRequest(String candidateId, int term) {
        if (term > raftNode.getCurrentTerm()) {
            raftNode.setCurrentTerm(term);
            raftNode.setVotedFor(candidateId);
            raftNode.onRoleTransition(RaftNode.Role.FOLLOWER);
            System.out.println(raftNode.getNodeId() + " voted for " + candidateId + " (term: " + term + ")");
        }
    }

    // Gracefully shutdown thread pool
    public void shutdown() {
        executorService.shutdown();
    }
}