package com.group6.logsystem.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class RaftGrpcClient {

    private final ManagedChannel channel;
    private final LogServiceGrpc.LogServiceStub asyncStub;

    public RaftGrpcClient(String host, int port) {
        // Create a channel to connect to the node at the given host and port
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()  // Use plaintext for simplicity
                .build();
        this.asyncStub = LogServiceGrpc.newStub(channel);
    }

    // RequestVote RPC call
    public void sendRequestVote(String peerId, RequestVoteRequest request, StreamObserver<RequestVoteResponse> responseObserver) {
        asyncStub.requestVote(request, responseObserver);
    }

    // AppendEntries RPC call
    public void sendAppendEntries(String peerId, AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        asyncStub.appendEntries(request, responseObserver);
    }

    // Shutdown the gRPC channel
    public void shutdown() {
        channel.shutdown();
    }
}
