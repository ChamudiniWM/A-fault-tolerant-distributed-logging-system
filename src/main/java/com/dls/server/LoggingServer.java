package com.dls.server;

import com.dls.common.NodeInfo;
import com.dls.raft.RaftNode;
import com.dls.raft.rpc.LogEntry;
import com.dls.raft.rpc.LogMessage;
import com.dls.raft.rpc.LoggingServiceGrpc;
import com.dls.raft.rpc.RaftGrpc;
import com.dls.replication.LogRetriever;
import com.dls.common.LocalLogEntry;
import com.dls.timesync.TimestampCorrector;
import com.dls.timesync.ClockSkewHandler;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LoggingServer {

    private final int port;
    private final Server server;
    private final List<LocalLogEntry> receivedLogs = new CopyOnWriteArrayList<>();
    private final TimestampCorrector timestampCorrector = new TimestampCorrector(new ClockSkewHandler());
    private final LogRetriever logRetriever = new LogRetriever(timestampCorrector);
    private final RaftNode raftNode;

    public LoggingServer(int port, RaftNode raftNode) {
        this.port = port;
        this.raftNode = raftNode;
        this.server = ServerBuilder.forPort(port)
                .addService(new LoggingServiceImpl())
                .addService(new LoggingServiceLoggerImpl())
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("🟢 LoggingServer started on port " + port);

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10_000);
                    System.out.println("📜 Ordered Logs:");
                    for (LocalLogEntry log : getOrderedLogs()) {
                        System.out.println("🧾 " + log);
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("🛑 Shutting down LoggingServer...");
            LoggingServer.this.stop();
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private class LoggingServiceImpl extends RaftGrpc.RaftImplBase {
        @Override
        public void appendLog(LogEntry request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
            LocalLogEntry entry = LocalLogEntry.fromGrpcLogEntry(request);
            receivedLogs.add(entry);
            System.out.println("📥 Received log: " + entry);
            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    private class LoggingServiceLoggerImpl extends LoggingServiceGrpc.LoggingServiceImplBase {

        @Override
        public void log(LogMessage request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
            System.out.printf("📣 [LOG] Node %s: %s%n", request.getNodeId(), request.getMessage());

            // Create LocalLogEntry with synchronized timestamp
            LocalLogEntry entry = new LocalLogEntry(
                    -1, // placeholder index (not using Raft log index)
                    -1, // placeholder term (not using Raft term)
                    request.getMessage(),
                    com.dls.timesync.TimeSynchronizer.getSynchronizedTime().toEpochMilli(),
                    request.getNodeId()
            );
            entry.setData("ExternalLog");
            entry.setMetadata("ViaLoggingServer");

            // Append to internal log list
            receivedLogs.add(entry);

            // If this server is also a RaftNode (optional), append to Raft log
            if (raftNode != null && raftNode.getRaftLog() != null) {
                try {
                    long newIndex = raftNode.getRaftLog().getLastLogIndex() + 1;
                    long term = raftNode.getCurrentTerm();

                    entry.setIndex((int) newIndex);
                    entry.setTerm((int) term);

                    raftNode.getRaftLog().appendEntry(entry, (int) term);
                    System.out.println("🧾 [Appended to Raft Log] " + entry);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to append to Raft log: " + e.getMessage());
                }
            } else {
                System.out.println("ℹ️ Raft node not available — skipping Raft log append.");
            }

            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }



    }

    public List<LocalLogEntry> getOrderedLogs() {
        return logRetriever.correctAndOrderLogs(receivedLogs);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50056;

        // Define the Logging Server as a Raft node for interaction
        NodeInfo self = new NodeInfo("logging-server", "localhost", port);

        // Use the actual Raft nodes from your MultiNodeServerMain
        List<NodeInfo> peers = List.of(
                new NodeInfo("node1", "localhost", 50051),
                new NodeInfo("node2", "localhost", 50052),
                new NodeInfo("node3", "localhost", 50053)
        );

        RaftNode raftNode = new RaftNode(self, peers);

        LoggingServer server = new LoggingServer(port, raftNode);
        server.start();
        server.blockUntilShutdown();
    }
}