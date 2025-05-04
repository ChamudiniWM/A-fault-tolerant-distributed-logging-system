package raft.grpc;

import client.Client;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import logging.LogEntry;
import raft.core.RaftNode;
import raft.core.RaftRole;
import util.TimeSyncUtil;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClientServiceImpl extends client.ClientServiceGrpc.ClientServiceImplBase {
    private final RaftNode node;

    public ClientServiceImpl(RaftNode node) {
        this.node = node;
    }

    @Override
    public void sendCommand(client.Client.ClientCommandRequest request, StreamObserver<client.Client.ClientCommandResponse> responseObserver) {
        synchronized (node.getLock()) {
            if (node.getRole() != RaftRole.LEADER) {
                client.Client.ClientCommandResponse.Builder response = client.Client.ClientCommandResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Not the leader. Please redirect to the leader.");
                String leaderHost = node.getLeaderHost();
                Integer leaderPort = node.getLeaderPort();
                if (leaderHost != null && !leaderHost.isEmpty() && leaderPort != null && leaderPort != 0) {
                    response.setLeaderHost(leaderHost)
                            .setLeaderPort(leaderPort);
                }
                responseObserver.onNext(response.build());
                responseObserver.onCompleted();
                return;
            }

            try {
                String logId = UUID.randomUUID().toString();
                long timestamp = TimeSyncUtil.getNtpAdjustedTime();
                node.appendClientCommand(request.getCommand(), logId, timestamp);

                client.Client.ClientCommandResponse response = client.Client.ClientCommandResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Command added to log")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch (Exception e) {
                client.Client.ClientCommandResponse response = client.Client.ClientCommandResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Failed to append command: " + e.getMessage())
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        }
    }

    @Override
    public void queryLogs(client.Client.LogQueryRequest request, StreamObserver<client.Client.LogQueryResponse> responseObserver) {
        List<LogEntry> logs = node.getLog();
        List<client.Client.LogEntryData> matching = logs.stream()
                .filter(e -> e.getTimestamp() >= request.getStartTimestamp() && e.getTimestamp() <= request.getEndTimestamp())
                .sorted((a, b) -> request.getAscending() ? Long.compare(a.getTimestamp(), b.getTimestamp())
                        : Long.compare(b.getTimestamp(), a.getTimestamp()))
                .map(e -> client.Client.LogEntryData.newBuilder()
                        .setCommand(e.getCommand())
                        .setLogId(e.getLogId())
                        .setTimestamp(e.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        responseObserver.onNext(client.Client.LogQueryResponse.newBuilder().addAllEntries(matching).build());
        responseObserver.onCompleted();
    }

    @Override
    public void isLeader(client.Client.Empty request, StreamObserver<client.Client.IsLeaderResponse> responseObserver) {
        boolean isLeader = node.getRole() == RaftRole.LEADER;
        client.Client.IsLeaderResponse.Builder builder = client.Client.IsLeaderResponse.newBuilder()
                .setIsLeader(isLeader);

        if (isLeader) {
            builder.setMessage("This node is the leader.");
        } else {
            String leaderHost = node.getLeaderHost();
            Integer leaderPort = node.getLeaderPort();

            if (leaderHost != null && !leaderHost.isEmpty() && leaderPort != null && leaderPort != 0) {
                builder.setLeaderHost(leaderHost)
                        .setLeaderPort(leaderPort)
                        .setMessage("This node is not the leader. Redirecting.");
            } else {
                builder.setMessage("This node is not the leader. Leader info unavailable.");
            }
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getServerTime(Client.Empty request, StreamObserver<client.Client.ServerTimeResponse> responseObserver) {
        try {
            long timeStamp = TimeSyncUtil.getNtpAdjustedTime();
            Client.ServerTimeResponse response = Client.ServerTimeResponse.newBuilder()
                    .setTimestamp(timeStamp)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription("Failed to get server time: " + e.getMessage())));
        }
    }
}