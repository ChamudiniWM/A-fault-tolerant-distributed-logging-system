package com.group6.logsystem.tests;

import com.group6.logsystem.consensus.ConsensusModule;
import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.grpc.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicationTest {

    private RaftNode raftNode;
    private ConsensusModule consensusModule;
    private LogServiceImpl logService;

    @BeforeEach
    void setUp() {
        raftNode = new RaftNode("node1", List.of("node2", "node3"));
        raftNode.start();
        raftNode.becomeLeader(); // Force node1 to be leader for testing
        consensusModule = new ConsensusModule(raftNode, "localhost:50051");
        logService = new LogServiceImpl(raftNode, consensusModule);
    }

    @Test
    void testDeduplication() {
        String logId = UUID.randomUUID().toString();
        LogRequest request1 = LogRequest.newBuilder()
                .setLogId(logId)
                .setNodeId("node1")
                .setMessage("Test log")
                .setTimestamp(System.currentTimeMillis())
                .setTerm(1)
                .build();

        LogRequest request2 = LogRequest.newBuilder()
                .setLogId(logId) // Same logId as request1
                .setNodeId("node1")
                .setMessage("Test log")
                .setTimestamp(System.currentTimeMillis())
                .setTerm(1)
                .build();

        // First request: Should be appended
        TestStreamObserver<LogResponse> observer1 = new TestStreamObserver<>();
        logService.sendLog(request1, observer1);
        assertTrue(observer1.getResponse().getSuccess());
        // Manually update commitIndex to simulate a commit
        raftNode.setCommitIndex(1);
        assertEquals(1, raftNode.getCommittedEntries().size());
        assertEquals(logId, raftNode.getCommittedEntries().get(0).getLogId());

        // Second request: Should be rejected (duplicate)
        TestStreamObserver<LogResponse> observer2 = new TestStreamObserver<>();
        logService.sendLog(request2, observer2);
        assertTrue(observer2.getResponse().getSuccess());
        assertEquals(1, raftNode.getCommittedEntries().size()); // No new log added
    }
}

class TestStreamObserver<T> implements StreamObserver<T> {
    private T response;

    @Override
    public void onNext(T value) {
        this.response = value;
    }

    @Override
    public void onError(Throwable t) {
        throw new RuntimeException(t);
    }

    @Override
    public void onCompleted() {
    }

    public T getResponse() {
        return response;
    }
}