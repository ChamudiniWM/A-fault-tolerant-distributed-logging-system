package com.group6.logsystem.grpc;

import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;

public class LogServiceImpl extends LogServiceGrpc.LogServiceImplBase {

    private final List<LogEntry> logEntries = new ArrayList<>();

    @Override
    public void sendLog(LogRequest request, StreamObserver<LogResponse> responseObserver) {
        LogEntry entry = LogEntry.newBuilder()
                .setNodeId(request.getNodeId())
                .setMessage(request.getMessage())
                .setTimestamp(request.getTimestamp())
                .build();

        // For now, just save it in memory
        synchronized (logEntries) {
            logEntries.add(entry);
        }

        LogResponse response = LogResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Log received from node: " + request.getNodeId())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void queryLogs(QueryRequest request, StreamObserver<LogList> responseObserver) {
        List<LogEntry> results = new ArrayList<>();

        synchronized (logEntries) {
            for (LogEntry entry : logEntries) {
                if (entry.getTimestamp() >= request.getStartTime() &&
                        entry.getTimestamp() <= request.getEndTime() &&
                        entry.getNodeId().equals(request.getNodeId())) {
                    results.add(entry);
                }
            }
        }

        LogList logList = LogList.newBuilder()
                .addAllLogs(results)
                .build();

        responseObserver.onNext(logList);
        responseObserver.onCompleted();
    }
}