package com.group6.logsystem.grpc;

import com.group6.logsystem.consensus.ConsensusModule;
import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.models.InternalLogEntry;
import io.grpc.stub.StreamObserver;

public class LogServiceImpl extends LogServiceGrpc.LogServiceImplBase {

    private final LogServiceImplAdapter adapter;
    private final RaftNode raftNode;
    private final ConsensusModule consensusModule;

    // Constructor to initialize RaftNode, ConsensusModule, and LogServiceImplAdapter
    public LogServiceImpl(RaftNode raftNode, ConsensusModule consensusModule) {
        this.raftNode = raftNode;
        this.consensusModule = consensusModule;
        this.adapter = new LogServiceImplAdapter(raftNode, consensusModule);
    }

    @Override
    public void requestVote(RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
        // Check if the candidate's term is less than the current term
        if (request.getTerm() < raftNode.getCurrentTerm()) {
            // If the candidate's term is less than the current term, reject the vote
            RequestVoteResponse response = RequestVoteResponse.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setVoteGranted(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        // If the candidate's term is higher, update the current term
        if (request.getTerm() > raftNode.getCurrentTerm()) {
            raftNode.setCurrentTerm(request.getTerm());
            raftNode.setVotedFor(null); // Reset vote if the term is updated
        }

        // Grant the vote if the node hasn't voted or if it has voted for the same candidate
        boolean voteGranted = false;
        if (raftNode.getVotedFor() == null || raftNode.getVotedFor().equals(request.getCandidateId())) {
            // Check if the candidate's log is up-to-date (last log term and index)
            if (raftNode.getCommittedEntries().isEmpty()) {
                // Handle empty log (no entries to compare with)
                raftNode.setVotedFor(request.getCandidateId());
                voteGranted = true;
            } else {
                InternalLogEntry lastLog = raftNode.getCommittedEntries()
                        .get(raftNode.getCommittedEntries().size() - 1);

                if (lastLog.getTerm() <= request.getLastLogTerm() && lastLog.getIndex() <= request.getLastLogIndex()) {
                    // Grant vote and update the votedFor field
                    raftNode.setVotedFor(request.getCandidateId());
                    voteGranted = true;
                }
            }
        }

        // Prepare the response
        RequestVoteResponse response = RequestVoteResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setVoteGranted(voteGranted)
                .build();

        // Send response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        // If the leader's term is less than the current term, reject the append
        if (request.getTerm() < raftNode.getCurrentTerm()) {
            AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setSuccess(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        // If the leader's term is higher, update the current term
        if (request.getTerm() > raftNode.getCurrentTerm()) {
            raftNode.setCurrentTerm(request.getTerm());
            raftNode.setRole(RaftNode.Role.FOLLOWER);
        }

        // Verify if the logs match
        if (request.getPrevLogIndex() < 0 || request.getPrevLogIndex() >= raftNode.getCommittedEntries().size()) {
            // Handle error: invalid previous log index
            AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setSuccess(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        InternalLogEntry prevLog = raftNode.getCommittedEntries()
                .get(request.getPrevLogIndex());
        if (prevLog == null || prevLog.getTerm() != request.getPrevLogTerm()) {
            // If the previous log doesn't match, respond with failure
            AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setSuccess(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        int nextIndex = raftNode.getLastLogIndex() + 1; // This assumes you have a method to get the last log index

        for (LogEntry entry : request.getEntriesList()) {
            InternalLogEntry internalLogEntry = new InternalLogEntry(
                    nextIndex++,
                    entry.getNodeId(),
                    entry.getMessage(),
                    entry.getTimestamp(),
                    request.getTerm()
            );
            raftNode.getCommittedEntries().add(internalLogEntry);
        }

        // Update the commit index if needed
        if (request.getLeaderCommit() > raftNode.getCommitIndex()) {
            raftNode.setCommitIndex(Math.min(request.getLeaderCommit(), raftNode.getCommittedEntries().size()));
        }

        // Respond with success
        AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setSuccess(true)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}