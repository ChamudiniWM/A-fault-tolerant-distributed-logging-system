package com.dls.raft.rpc;

import io.grpc.stub.StreamObserver;
import com.dls.raft.ConsensusModule;
import com.dls.raft.LeaderElection;
import com.dls.raft.rpc.LogEntry; // Import your LogEntry DTO
import com.google.protobuf.Empty;

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
        RequestVoteResponse response = leaderElection.processVoteRequest(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void appendLog(LogEntry request, StreamObserver<Empty> responseObserver) {
        // Handle a new log entry being appended (if needed)
        System.out.println("Received a new log entry: " + request.getCommand());

        // You might want to add it to the Raft log here via consensusModule
        consensusModule.handleNewLogEntry(request);

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}