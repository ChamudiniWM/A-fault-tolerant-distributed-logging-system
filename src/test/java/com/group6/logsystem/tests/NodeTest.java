//package com.group6.logsystem.tests;
//
//import com.group6.logsystem.node.Node;
//import com.group6.logsystem.grpc.LogEntry;
//import com.group6.logsystem.grpc.LogRequest;
//import com.group6.logsystem.grpc.LogResponse;
//import com.group6.logsystem.grpc.LogServiceGrpc;
//import org.junit.Before;
//import org.junit.Test;
//import static org.mockito.Mockito.*;
//
//public class NodeTest {
//
//    private Node node;
//    private LogServiceGrpc.LogServiceBlockingStub mockStub;
//
//    @Before
//    public void setup() {
//        mockStub = mock(LogServiceGrpc.LogServiceBlockingStub.class);
//        node = new Node("node1");
//        node.setLogServiceStub(mockStub);
//    }
//
//    @Test
//    public void testSendLog() {
//        LogEntry logEntry = LogEntry.newBuilder()
//                .setNodeId("node1")
//                .setMessage("Test log message")
//                .setTimestamp(System.currentTimeMillis())
//                .build();
//
//        LogRequest request = LogRequest.newBuilder()
//                .setNodeId("node1")
//                .setMessage("Test log message")
//                .setTimestamp(logEntry.getTimestamp())
//                .build();
//
//        LogResponse mockResponse = LogResponse.newBuilder()
//                .setSuccess(true)
//                .setMessage("Log received")
//                .build();
//
//        when(mockStub.sendLog(request)).thenReturn(mockResponse);
//
//        node.sendLog(logEntry);
//
//        verify(mockStub).sendLog(request); // Verifies that sendLog was called with the correct request
//    }
//}