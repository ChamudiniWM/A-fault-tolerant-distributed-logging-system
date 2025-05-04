package raft.grpc;

import io.grpc.stub.StreamObserver;
import logging.*;
import raft.core.RaftNode;

public class LogServiceImpl extends LogServiceGrpc.LogServiceImplBase {
    private final RaftNode raftNode;

    public LogServiceImpl(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        String message = request.getMessage();
        System.out.println("\n=== Ping Received ===");
        System.out.println("Message: " + message + "\n");

        PingResponse response = PingResponse.newBuilder()
                .setMessage("Ping response: " + message)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getLogInfo(GetLogInfoRequest request, StreamObserver<GetLogInfoResponse> responseObserver) {
        int lastLogIndex = raftNode.getLog().size() - 1;
        int lastLogTerm = (lastLogIndex >= 0) ? raftNode.getLog().get(lastLogIndex).getTerm() : 0;
        GetLogInfoResponse response = GetLogInfoResponse.newBuilder()
                .setLastLogIndex(lastLogIndex)
                .setLastLogTerm(lastLogTerm)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void requestVote(VoteRequest request, StreamObserver<VoteResponse> responseObserver) {
        raftNode.requestVote(request, responseObserver);
    }

    @Override
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        raftNode.appendEntries(request, responseObserver);
    }

    @Override
    public void notifyLeaderUpdate(LeaderUpdateRequest request, StreamObserver<LeaderUpdateResponse> responseObserver) {
        System.out.println("\n=== Leader Update Received ===");
        System.out.println("New Leader Host: " + request.getLeaderHost());
        System.out.println("New Leader Port: " + request.getLeaderPort());
        raftNode.setLeaderHost(request.getLeaderHost());
        raftNode.setLeaderPort(request.getLeaderPort());
        LeaderUpdateResponse response = LeaderUpdateResponse.newBuilder()
                .setSuccess(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}