package com.group6.logsystem.grpc;

import com.group6.logsystem.consensus.RaftNode;
import com.group6.logsystem.consensus.ConsensusModule;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.Arrays;

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

        // Initialize RaftNode
        RaftNode raftNode = new RaftNode(nodeId, Arrays.asList(peerIds));
        raftNode.start();

        // You might later pass peer addresses too — for now, keep it simple
        ConsensusModule consensusModule = new ConsensusModule(raftNode, "localhost:" + (grpcPort + 1));

        LogServiceImpl logService = new LogServiceImpl(raftNode, consensusModule);

        Server server = ServerBuilder
                .forPort(grpcPort)
                .addService(logService)
                .build();

        System.out.println("gRPC Server for " + nodeId + " started on port " + grpcPort);
        server.start();
        server.awaitTermination();
    }
}