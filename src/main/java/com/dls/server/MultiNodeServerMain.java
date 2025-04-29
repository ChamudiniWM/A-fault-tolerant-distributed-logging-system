package com.dls.server;

import com.dls.common.NodeInfo;
import com.dls.config.ConfigLoader;
import com.dls.raft.ConsensusModule;
import com.dls.raft.LeaderElection;
import com.dls.raft.RaftNode;
import com.dls.raft.rpc.RaftServiceGrpcImpl;
import com.dls.raft.rpc.LogMessage;
import com.dls.raft.rpc.LoggingServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.List;

public class MultiNodeServerMain {

    public static void main(String[] args) throws InterruptedException {
        // Load node configurations
        List<NodeInfo> nodeInfos = ConfigLoader.loadClusterConfig("cluster_config.json");

        System.out.println("Starting nodes...");

        // Start each node in a new thread
        for (NodeInfo nodeInfo : nodeInfos) {
            System.out.println("Starting thread for node: " + nodeInfo.getNodeId() + " on port " + nodeInfo.getPort());
            new Thread(() -> startNode(nodeInfo, nodeInfos)).start();
        }

        // Keep main thread alive
        synchronized (MultiNodeServerMain.class) {
            System.out.println("Main thread waiting for nodes to finish...");
            MultiNodeServerMain.class.wait();
        }
    }

    private static void startNode(NodeInfo selfNodeInfo, List<NodeInfo> allNodeInfos) {
        try {
            System.out.println("Initializing RaftNode for " + selfNodeInfo.getNodeId());

            RaftNode raftNode = new RaftNode(selfNodeInfo, allNodeInfos);

            Server server = ServerBuilder.forPort(selfNodeInfo.getPort())
                    .addService(new RaftServiceGrpcImpl(
                            new ConsensusModule(raftNode),
                            new LeaderElection(raftNode)
                    ))
                    .build();

            System.out.println("Node " + selfNodeInfo.getNodeId() + " starting on port " + selfNodeInfo.getPort());
            server.start();
            System.out.println("Node " + selfNodeInfo.getNodeId() + " started successfully!");

            // Logging stub setup
            ManagedChannel loggingChannel = ManagedChannelBuilder.forAddress("127.0.0.1", 50056)
                    .usePlaintext()
                    .build();
            LoggingServiceGrpc.LoggingServiceBlockingStub logStub = LoggingServiceGrpc.newBlockingStub(loggingChannel);

            // Try logging to LoggingServer with retries
            boolean logged = false;
            for (int i = 0; i < 5 && !logged; i++) {
                try {
                    logStub.log(LogMessage.newBuilder()
                            .setNodeId(selfNodeInfo.getNodeId())
                            .setMessage("✅ Node " + selfNodeInfo.getNodeId() + " started successfully and connected to LoggingServer")
                            .build());
                    logged = true;
                } catch (Exception e) {
                    System.err.println("⚠️ Retry " + (i + 1) + ": Failed to log from " + selfNodeInfo.getNodeId() + " - " + e.getMessage());
                    Thread.sleep(1000); // wait 1 second before retry
                }
            }

            if (!logged) {
                System.err.println("❌ Logging failed for node " + selfNodeInfo.getNodeId() + " after 5 attempts.");
            }

            // Shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.err.println("Shutting down node " + selfNodeInfo.getNodeId());
                server.shutdown();
                loggingChannel.shutdown();
            }));

            server.awaitTermination();
            System.out.println("Node " + selfNodeInfo.getNodeId() + " server terminated.");

        } catch (IOException | InterruptedException e) {
            System.err.println("Error starting node " + selfNodeInfo.getNodeId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
