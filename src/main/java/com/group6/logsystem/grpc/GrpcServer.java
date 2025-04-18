package com.group6.logsystem.grpc;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.consensus.ConsensusModule;
import com.group6.logsystem.consensus.LeaderElectionService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GrpcServer {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java GrpcServer <nodeId> <comma-separated-peerIds>");
            System.exit(1);
        }

        // Get node ID and peers from arguments
        String nodeId = args[0]; // e.g., "node1"
        String[] peerIds = args[1].split(","); // e.g., "node2,node3"

        // Dynamic gRPC port (e.g., 50051 for node1, 50052 for node2, etc.)
        int nodeNum = Integer.parseInt(nodeId.replaceAll("[^0-9]", ""));
        int grpcPort = 50050 + nodeNum;

        // Create a list of peer node IDs
        List<String> peerList = Arrays.asList(peerIds);

        // Step 1: Create a temporary RaftNode without the LeaderElectionService
        RaftNode localNode = new RaftNode(nodeId, peerList, null);

        // Step 2: Create a list of RaftNode objects for peers (with dummy LeaderElectionService or null)
        List<RaftNode> peerRaftNodes = new ArrayList<>();
        for (String peerId : peerList) {
            peerRaftNodes.add(new RaftNode(peerId, peerList, null));
        }

        // Step 3: Create the LeaderElectionService
        LeaderElectionService leaderElectionService = new LeaderElectionService(localNode, peerRaftNodes);

        // Step 4: Recreate the local RaftNode with the actual LeaderElectionService
        localNode = new RaftNode(nodeId, peerList, leaderElectionService);

        // Step 5: Start the local RaftNode (contains delay before election begins)
        localNode.start();

        // Step 6: Initialize ConsensusModule
        ConsensusModule consensusModule = new ConsensusModule(localNode, "localhost:" + (grpcPort + 1));

        // Step 7: Create the LogService
        LogServiceImpl logService = new LogServiceImpl(localNode, consensusModule);

        // Step 8: Start the gRPC server
        Server server = ServerBuilder
                .forPort(grpcPort)
                .addService(logService)
                .build();

        System.out.println("gRPC Server for " + nodeId + " started on port " + grpcPort);
        server.start();
        server.awaitTermination();
    }
}