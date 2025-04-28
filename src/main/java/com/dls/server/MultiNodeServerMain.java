package com.dls.server;

import com.dls.common.NodeInfo;
import com.dls.raft.ConsensusModule;
import com.dls.raft.LeaderElection;
import com.dls.raft.RaftNode;
import com.dls.raft.rpc.RaftServiceGrpcImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MultiNodeServerMain {

    public static void main(String[] args) throws InterruptedException {
        // Define node information (FIX: nodeId first, then host, then port)
        NodeInfo node1Info = new NodeInfo("node1", "localhost", 50051);
        NodeInfo node2Info = new NodeInfo("node2", "localhost", 50052);
        NodeInfo node3Info = new NodeInfo("node3", "localhost", 50053);

        // Create the list of nodes
        List<NodeInfo> nodeInfos = Arrays.asList(node1Info, node2Info, node3Info);

        System.out.println("Starting nodes...");

        // Start a node for each nodeInfo in a separate thread
        for (NodeInfo nodeInfo : nodeInfos) {
            System.out.println("Starting thread for node: " + nodeInfo.getNodeId() + " on port " + nodeInfo.getPort());
            new Thread(() -> startNode(nodeInfo, nodeInfos)).start();
        }

        // Main thread waits for nodes to finish
        synchronized (MultiNodeServerMain.class) {
            System.out.println("Main thread waiting for nodes to finish...");
            MultiNodeServerMain.class.wait();
        }
    }

    private static void startNode(NodeInfo selfNodeInfo, List<NodeInfo> allNodeInfos) {
        try {
            // Debugging: Log node initialization
            System.out.println("Initializing RaftNode for " + selfNodeInfo.getNodeId() + " with peers: " + allNodeInfos);

            // Initialize the RaftNode with self node info and the list of all nodes
            RaftNode raftNode = new RaftNode(selfNodeInfo, allNodeInfos);

            // Debugging: Log node startup
            System.out.println("Creating gRPC server for node " + selfNodeInfo.getNodeId() + " on port " + selfNodeInfo.getPort());

            // Create the gRPC server for this node
            Server server = ServerBuilder.forPort(selfNodeInfo.getPort())
                    .addService(new RaftServiceGrpcImpl(
                            new ConsensusModule(raftNode),
                            new LeaderElection(raftNode)
                    ))  // Pass both objects correctly
                    .build();


            System.out.println("Node " + selfNodeInfo.getNodeId() + " starting on port " + selfNodeInfo.getPort());

            // Start the server
            server.start();
            System.out.println("Node " + selfNodeInfo.getNodeId() + " started successfully!");

            // Debugging: Log shutdown hook
            System.out.println("Adding shutdown hook for node " + selfNodeInfo.getNodeId());

            // Add a shutdown hook to gracefully stop the server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.err.println("Shutting down node " + selfNodeInfo.getNodeId());
                server.shutdown();
            }));

            // Wait for the server to terminate (blocking)
            server.awaitTermination();
            System.out.println("Node " + selfNodeInfo.getNodeId() + " server terminated.");

        } catch (IOException | InterruptedException e) {
            // Debugging: Log any exceptions that occur during startup
            System.err.println("Error starting node " + selfNodeInfo.getNodeId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
