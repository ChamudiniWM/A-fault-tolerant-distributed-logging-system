package com.dls.loadtest;

import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.RaftGrpc;
import com.dls.raft.rpc.LogEntry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class LoadTestClient {

    private final RaftGrpc.RaftBlockingStub blockingStub;
    private int leaderTerm = 1; // Assume term 1 for now


    public LoadTestClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = RaftGrpc.newBlockingStub(channel);
    }

    public void sendLogEntries(int numEntries, int delayMs) throws InterruptedException {
        for (int i = 0; i < numEntries; i++) {
            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .setTerm(leaderTerm) // you may want to set the correct term
                    .setLeaderId("leader") // assume you know the leader ID for now
                    .addEntries(LogEntry.newBuilder()
                            .setIndex(i)
                            .setTerm(0)
                            .setCommand("log-" + i)
                            .build())
                    .build();
            try {
                blockingStub.appendEntries(request);
            } catch (Exception e) {
                System.err.println("Failed to send log entry " + i + ": " + e.getMessage());
            }

            // Control the sending rate
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LoadTestClient client = new LoadTestClient("localhost", 50051);
        client.sendLogEntries(10000, 1); // Send 10000 entries, 1 ms apart (~1000/sec)
    }
}