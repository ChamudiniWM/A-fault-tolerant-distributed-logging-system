package com.dls.server;

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
    private final TimestampCorrector timestampCorrector = new TimestampCorrector(new ClockSkewHandler()); // Or inject
    private final LogRetriever logRetriever = new LogRetriever(timestampCorrector);

    public LoggingServer(int port) {
        this.port = port;
        this.server = ServerBuilder.forPort(port)
                .addService(new LoggingServiceImpl())
                .addService(new LoggingServiceLoggerImpl())
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("🟢 LoggingServer started on port " + port);

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
            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    // Expose a way to get corrected and sorted logs
    public List<LocalLogEntry> getOrderedLogs() {
        return logRetriever.correctAndOrderLogs(receivedLogs);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50056; // or load from ServerConfig
        LoggingServer server = new LoggingServer(port);
        server.start();
        server.blockUntilShutdown();
    }
}