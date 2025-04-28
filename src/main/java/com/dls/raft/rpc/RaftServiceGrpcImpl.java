package com.dls.raft.rpc;

import io.grpc.stub.StreamObserver;
import com.dls.raft.RaftNode;
import com.dls.raft.ConsensusModule;
import com.dls.raft.LeaderElection;

public class RaftServiceGrpcImpl extends RaftGrpc.RaftImplBase {

    private final ConsensusModule consensusModule;
    private final LeaderElection leaderElection;

    public RaftServiceGrpcImpl(ConsensusModule consensusModule, LeaderElection leaderElection) {
        this.consensusModule = consensusModule;
        this.leaderElection = leaderElection;
    }


    @Override
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        AppendEntriesResponse response = consensusModule.processAppendEntries(request);

        // ✨ Reset election timer if AppendEntries (heartbeat) is successfully received
        if (response.getSuccess()) {
            leaderElection.resetElectionTimer();
            System.out.println("Resetting election timer after heartbeat from leader " + request.getLeaderId());
        } else {
            System.out.println("Heartbeat/AppendEntries from " + request.getLeaderId() + " rejected.");
        }


        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void requestVote(RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
        // Process the vote request
        RequestVoteResponse response = leaderElection.processVoteRequest(request);


        // Send the response back to the candidate
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
