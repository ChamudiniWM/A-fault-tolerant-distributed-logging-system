package com.dls.raft.rpc;

import io.grpc.stub.StreamObserver;
import com.dls.raft.RaftNode;
import com.dls.raft.RaftState;
import com.dls.raft.rpc.RequestVoteRequest;
import com.dls.raft.rpc.RequestVoteResponse;

public class RequestVoteRPC {

    private final RaftNode raftNode;

    public RequestVoteRPC(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    // RequestVote RPC handler
    public synchronized void requestVote(RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
        int currentTerm = raftNode.getCurrentTerm();
        int candidateTerm = request.getTerm();

        boolean voteGranted = false;

        if (candidateTerm < currentTerm) {
            // Candidate's term is outdated
            voteGranted = false;
        } else {
            // Candidate's term is >= currentTerm
            if (candidateTerm > currentTerm) {
                // Newer term: update ourselves
                raftNode.setCurrentTerm(candidateTerm);
                raftNode.setRaftState(RaftState.FOLLOWER);
                raftNode.setVotedFor(null); // Important: reset votedFor!
            }

            // Now, check if we can vote
            if (raftNode.getVotedFor() == null || raftNode.getVotedFor().equals(request.getCandidateId())) {
                voteGranted = true;
                raftNode.setVotedFor(request.getCandidateId());
            }
        }

        // Build the response
        RequestVoteResponse response = RequestVoteResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setVoteGranted(voteGranted)
                .build();

        // Send the response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}