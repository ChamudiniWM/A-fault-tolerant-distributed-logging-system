package com.group6.logsystem.consensus;

import com.group6.logsystem.grpc.*;
import io.grpc.stub.StreamObserver;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.consensus.LeaderElectionService;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.consensus.LeaderElectionService;
import com.group6.logsystem.models.InternalLogEntry;  // Assuming this is the class for log entries

public class RaftService extends LogServiceGrpc.LogServiceImplBase {

    private final RaftNode raftNode;
    private final LeaderElectionService leaderElectionService;

    // Constructor to initialize RaftNode and LeaderElectionService
    public RaftService(RaftNode raftNode, LeaderElectionService leaderElectionService) {
        this.raftNode = raftNode;
        this.leaderElectionService = leaderElectionService;
    }

    @Override
    public void requestVote(RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
        // Handle the RequestVote RPC
        System.out.println("Received vote request from " + request.getCandidateId() + " in term " + request.getTerm());

        // Forward vote request to the LeaderElectionService
        leaderElectionService.receiveVoteRequest(request.getCandidateId(), request.getTerm());

        // Build the response
        RequestVoteResponse response = RequestVoteResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())  // Return the current term from the RaftNode
                .setVoteGranted(true)  // Assume vote is granted (implement your vote decision logic)
                .build();

        // Send the response back to the client
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        // Handle the AppendEntries RPC
        System.out.println("Received append entries from " + request.getLeaderId() + " in term " + request.getTerm());

        // Forward the entries to RaftNode for log appending
        for (LogEntry entry : request.getEntriesList()) {
            int nextIndex = raftNode.getNextLogIndex(); // <-- You should implement this in RaftNode
            InternalLogEntry internalLogEntry = new InternalLogEntry(
                    entry.getLogId(),  // Assuming logId is part of the LogEntry
                    nextIndex,
                    entry.getNodeId(),
                    entry.getMessage(),
                    entry.getTimestamp(),
                    entry.getTerm()
            );
            raftNode.appendLogEntry(internalLogEntry);
        }


        // Build the response
        AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())   // Current term to check if leader is outdated
                .setSuccess(true)                     // Indicate if entries were successfully appended
                .build();

        // Send the response back to the leader
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}