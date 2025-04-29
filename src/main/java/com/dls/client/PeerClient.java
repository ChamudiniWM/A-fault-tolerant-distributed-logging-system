package com.dls.client;

import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;
import com.dls.raft.rpc.RaftGrpc;
import com.dls.common.NodeInfo;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;

public class PeerClient {

    private final NodeInfo peer;
    private final ManagedChannel channel;
    private final RaftGrpc.RaftBlockingStub blockingStub;

    public PeerClient(NodeInfo peer) {
        this.peer = peer;
        this.channel = ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort())
                .usePlaintext()
                .build();
        this.blockingStub = RaftGrpc.newBlockingStub(channel);
    }

    public AppendEntriesResponse sendAppendEntries(AppendEntriesRequest request) {
        try {
            return blockingStub.appendEntries(request);
        } catch (StatusRuntimeException e) {
            System.err.println("RPC failed to peer " + peer.getNodeId() + ": " + e.getStatus());
            return null;
        }
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Channel shutdown interrupted for peer " + peer.getNodeId());
        }
    }
}