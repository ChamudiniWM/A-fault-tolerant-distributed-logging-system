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
import java.util.Optional;

public class ServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println("❌ Port number is required as a command-line argument.");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        List<NodeInfo> nodeInfos = ConfigLoader.loadClusterConfig("cluster_config.json");

        // Find self node based on given port
        Optional<NodeInfo> optionalSelf = nodeInfos.stream()
                .filter(n -> n.getPort() == port)
                .findFirst();

        if (optionalSelf.isEmpty()) {
            System.err.println("❌ Error: No node configuration found for port " + port);
            System.exit(1);
            return;
        }

        NodeInfo selfNodeInfo = optionalSelf.get();
        System.out.println("✅ Identified self node as " + selfNodeInfo.getNodeId() + " (port " + selfNodeInfo.getPort() + ")");

        List<NodeInfo> peers = nodeInfos.stream()
                .filter(n -> !n.getNodeId().equals(selfNodeInfo.getNodeId()))
                .toList();

        // Initialize the Raft node
        System.out.println("🔧 Initializing RaftNode for " + selfNodeInfo.getNodeId() + " with peers: " + peers);
        RaftNode raftNode = new RaftNode(selfNodeInfo, peers);

        // Create and start the gRPC server
        System.out.println("🚀 Creating gRPC server for " + selfNodeInfo.getNodeId() + " on port " + selfNodeInfo.getPort());
        Server server = ServerBuilder.forPort(selfNodeInfo.getPort())
                .addService(new RaftServiceGrpcImpl(
                        new ConsensusModule(raftNode),
                        new LeaderElection(raftNode)
                ))
                .build();

        server.start();
        System.out.println("✅ Node " + selfNodeInfo.getNodeId() + " started successfully and is accepting gRPC requests");

        // Connect to LoggingServer (adjust to match actual port, typically 50056)
        ManagedChannel loggingChannel = ManagedChannelBuilder.forAddress("localhost", 50056)
                .usePlaintext()
                .build();
        LoggingServiceGrpc.LoggingServiceBlockingStub logStub = LoggingServiceGrpc.newBlockingStub(loggingChannel);

        try {
            logStub.log(LogMessage.newBuilder()
                    .setNodeId(selfNodeInfo.getNodeId())
                    .setMessage("🟢 Node " + selfNodeInfo.getNodeId() + " is up and running on port " + selfNodeInfo.getPort())
                    .build());
        } catch (Exception e) {
            System.err.println("❌ Failed to send log to LoggingServer: " + e.getMessage());
        }

        // Add shutdown hook for graceful cleanup
        System.out.println("🛠️  Adding shutdown hook for node " + selfNodeInfo.getNodeId());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("🛑 Shutting down node " + selfNodeInfo.getNodeId());
            server.shutdown();
            loggingChannel.shutdown();
            System.out.println("✅ Shutdown complete for node " + selfNodeInfo.getNodeId());
        }));

        // Keep server running
        server.awaitTermination();
        System.out.println("🔚 Node " + selfNodeInfo.getNodeId() + " terminated");
    }
}