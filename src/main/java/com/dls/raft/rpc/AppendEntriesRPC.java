package com.dls.raft.rpc;

import io.grpc.stub.StreamObserver;
import com.dls.raft.RaftNode;
import com.dls.raft.RaftState;
import com.dls.common.LocalLogEntry;
import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;

import java.util.List;
import java.util.stream.Collectors;

public class AppendEntriesRPC {

    private final RaftNode raftNode;

    public AppendEntriesRPC(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    // AppendEntries RPC handler
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        int currentTerm = raftNode.getCurrentTerm();
        int leaderTerm = request.getTerm();

        // If the leader's term is less than our current term, reject the request
        if (leaderTerm < currentTerm) {
            AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                    .setTerm(currentTerm)
                    .setSuccess(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        // Update current term if the leader's term is greater
        if (leaderTerm > currentTerm) {
            raftNode.setCurrentTerm(leaderTerm);
            raftNode.setRaftState(RaftState.FOLLOWER);  // Become a follower if necessary
        }

        // Verify log consistency (Check if the previous log entry matches)
        boolean isLogMatching = raftNode.getRaftLog().matchLog(request.getPrevLogIndex(), request.getPrevLogTerm());
        if (!isLogMatching) {
            AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setSuccess(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        // Convert gRPC LogEntry to LocalLogEntry
        List<LocalLogEntry> commonLogEntries = request.getEntriesList().stream()
                .map(LocalLogEntry::fromGrpcLogEntry)  // Clean conversion using your static method
                .collect(Collectors.toList());

        // Append new entries
        raftNode.getRaftLog().appendEntries(commonLogEntries, raftNode.getCurrentTerm());
        System.out.println("Appended " + request.getEntriesCount() + " new entries");

        // Update the commit index if necessary
        if (request.getLeaderCommit() > raftNode.getCommitIndex()) {
            raftNode.getRaftLog().setCommitIndex(
                    Math.min(request.getLeaderCommit(), raftNode.getRaftLog().getLastLogIndex())
            );
        }

        // Send success response
        AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setSuccess(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}