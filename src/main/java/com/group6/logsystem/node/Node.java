package com.group6.logsystem.node;

import com.group6.logsystem.consensus.ConsensusModule;
import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.grpc.LogEntry;
import com.group6.logsystem.grpc.LogRequest;
import com.group6.logsystem.grpc.LogResponse;
import com.group6.logsystem.grpc.LogServiceGrpc;
import com.group6.logsystem.models.InternalLogEntry;
import com.group6.logsystem.util.ConfigLoader;


//Import Time Sync utilities
import com.group6.logsystem.timesync.LogicalClock;
import com.group6.logsystem.timesync.LogTimestampCorrector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Arrays;

public class Node {

    private final String nodeId;
    private boolean isLeader;
    private final List<InternalLogEntry> logs;
    private final LogServiceGrpc.LogServiceBlockingStub logServiceStub;
    private final ConsensusModule consensusModule;

    // Time Synchronization Modules
    private final LogicalClock logicalClock = new LogicalClock();
    private final LogTimestampCorrector timestampCorrector = new LogTimestampCorrector();


    // Load configuration and set up peer list and peer address
    private static List<String> peersList;
    private static String peerAddress;

    static {
        try {
            Properties config = ConfigLoader.loadConfig("config.properties");
            String peersListString = config.getProperty("peers");
            peerAddress = config.getProperty("peerAddress");

            // Convert comma-separated string into List
            peersList = Arrays.asList(peersListString.split(","));
        } catch (Exception e) { // General catch, for any exception during loading
            e.printStackTrace();
            peersList = new ArrayList<>();
            peerAddress = "localhost:50052"; // default value if configuration fails
        }
    }

    public Node(String nodeId, LogServiceGrpc.LogServiceBlockingStub logServiceStub) {
        this.nodeId = nodeId;
        this.isLeader = false; // Default to follower
        this.logs = new ArrayList<>();
        this.logServiceStub = logServiceStub;

        // Initialize RaftNode and ConsensusModule
        RaftNode raftNode = new RaftNode(nodeId, peersList);
        this.consensusModule = new ConsensusModule(raftNode, peerAddress);
    }

    // Start the node (startup hook)
    public void start() {
        System.out.println("Starting node: " + nodeId);
        consensusModule.start();
    }

    // Shutdown the node (shutdown hook)
    public void shutdown() {
        System.out.println("Shutting down node: " + nodeId);
        consensusModule.shutdown();
    }

    // Simulate a node failover
    public void failover() {
        System.out.println("Node " + nodeId + " is failing over.");
        isLeader = false;
        consensusModule.handleFailover();
    }

    // Set node as leader
    public void setLeaderStatus(boolean isLeader) {
        this.isLeader = isLeader;
        consensusModule.updateLeaderStatus(isLeader);
    }

    // Send a log entry to a remote node
    public void sendLog(LogEntry logEntry) {
        if (!isLeader) {
            System.out.println("Only leaders can send log entries!");
            return;
        }

        // Apply logical clock timestamp
        long updatedTimestamp = logicalClock.tick();

        LogRequest request = LogRequest.newBuilder()
                .setNodeId(nodeId)
                .setMessage(logEntry.getMessage())
                .setTimestamp(logEntry.getTimestamp())
                .setTerm(logEntry.getTerm())
                .build();

        try {
            LogResponse response = logServiceStub.sendLog(request);
            System.out.println("Log entry sent: " + response.getMessage());
        } catch (Exception e) {
            System.out.println("Failed to send log entry: " + e.getMessage());
        }
    }

    // Append a log entry locally
    public void appendLog(LogEntry logEntry) {
        

        // Update logical clock based on received timestamp
        long updatedTimestamp = logicalClock.receive(logEntry.getTimestamp());

        // Adjust log entry timestamp if necessary
        logEntry.setTimestamp(updatedTimestamp);

        // Buffer and reorder logs
        timestampCorrector.bufferLog(logEntry);

        // Optional: Flush the logs in timestamp order
        timestampCorrector.flushLogsInOrder(); // This ensures out-of-order logs are handled

        
        logs.add(logEntry);
        System.out.println("Log entry appended to node " + nodeId + ": " + logEntry.getMessage());
    }

    // Getters
    public String getNodeId() {
        return nodeId;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public ConsensusModule getConsensusModule() {
        return consensusModule;
    }
}

