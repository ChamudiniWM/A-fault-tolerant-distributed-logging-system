package com.group6.logsystem.node;

import com.group6.logsystem.grpc.LogEntry;
import com.group6.logsystem.grpc.LogRequest;
import com.group6.logsystem.grpc.LogResponse;
import com.group6.logsystem.grpc.LogServiceGrpc;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private String nodeId;
    private boolean isLeader;
    private List<LogEntry> logs;
    private LogServiceGrpc.LogServiceBlockingStub logServiceStub;

    public Node(String nodeId) {
        this.nodeId = nodeId;
        this.logs = new ArrayList<>();
        // Initialize gRPC client stub for communication (can be passed or created here)
    }

    // Example method to send a log entry to a remote node
    public void sendLog(LogEntry logEntry) {
        LogRequest request = LogRequest.newBuilder()
                .setNodeId(nodeId)
                .setMessage(logEntry.getMessage())
                .setTimestamp(logEntry.getTimestamp())
                .build();

        LogResponse response = logServiceStub.sendLog(request);
        System.out.println(response.getMessage());
    }

    // Getter and Setter methods for nodeId, isLeader, etc.
}