package com.group6.logsystem.grpc;

import com.group6.logsystem.models.InternalLogEntry;
import com.group6.logsystem.util.LogEntryConverter;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LogServiceImpl extends LogServiceGrpc.LogServiceImplBase {

    private final List<InternalLogEntry> logEntries = new ArrayList<>();

    @Override
    public void sendLog(LogRequest request, StreamObserver<LogResponse> responseObserver) {
        // Convert proto to internal model
        InternalLogEntry entry = new InternalLogEntry(
                request.getNodeId(),
                request.getMessage(),
                request.getTimestamp()
        );

        // Save internally
        synchronized (logEntries) {
            logEntries.add(entry);
        }

        // Respond to client
        LogResponse response = LogResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Log received from node: " + request.getNodeId())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void queryLogs(QueryRequest request, StreamObserver<LogList> responseObserver) {
        List<InternalLogEntry> results;

        synchronized (logEntries) {
            results = logEntries.stream()
                    .filter(entry -> entry.getTimestamp() >= request.getStartTime()
                            && entry.getTimestamp() <= request.getEndTime()
                            && entry.getNodeId().equals(request.getNodeId()))
                    .toList();
        }

        // Convert internal logs back to proto
        List<com.group6.logsystem.grpc.LogEntry> protoLogs = results.stream()
                .map(LogEntryConverter::toProto)
                .collect(Collectors.toList());

        LogList logList = LogList.newBuilder()
                .addAllLogs(protoLogs)
                .build();

        responseObserver.onNext(logList);
        responseObserver.onCompleted();
    }
}