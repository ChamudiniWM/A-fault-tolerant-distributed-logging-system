package com.group6.logsystem.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import com.group6.logsystem.grpc.*;

public class RaftGrpcClient {

    private final ManagedChannel channel;
    private final LogServiceGrpc.LogServiceStub asyncStub;

    public RaftGrpcClient(String host, int port) {
        // Create a channel to connect to the node at the given host and port
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()  // Use plaintext for simplicity, change as necessary
                .build();
        this.asyncStub = LogServiceGrpc.newStub(channel);
    }

    // Method to send RequestVote RPC
    public void sendRequestVote(RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
        asyncStub.requestVote(request, responseObserver);
    }

    // Method to send AppendEntries RPC
    public void sendAppendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        asyncStub.appendEntries(request, responseObserver);
    }

    // Shutdown the channel when done
    public void shutdown() {
        channel.shutdown();
    }
}