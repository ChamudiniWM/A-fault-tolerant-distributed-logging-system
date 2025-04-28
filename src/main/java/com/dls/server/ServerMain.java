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

public class ServerMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.err.println("Port number is required as a command-line argument.");
            System.exit(1);
        }

        // Read the port from the command-line arguments
        int port = Integer.parseInt(args[0]);

        // Create a RaftNode instance (you may need to initialize it based on your configuration)
        NodeInfo selfNodeInfo = new NodeInfo("localhost", "node1", port);  // Use the port from arguments

        NodeInfo peer1 = new NodeInfo("localhost", "node2", 50052);
        NodeInfo peer2 = new NodeInfo("localhost", "node3", 50053);
        NodeInfo peer3 = new NodeInfo("localhost", "node4", 50054);

        List<NodeInfo> peers = Arrays.asList(peer1, peer2, peer3);

        // Now, create the RaftNode with these two arguments
        RaftNode raftNode = new RaftNode(selfNodeInfo, peers);

        // Create a gRPC server and add the RaftService
        Server server = ServerBuilder.forPort(selfNodeInfo.getPort())
                .addService(new RaftServiceGrpcImpl(
                        new ConsensusModule(raftNode),
                        new LeaderElection(raftNode)
                ))  // Pass both objects correctly
                .build();


        System.out.println("Server is starting on port " + port);

        // Start the server
        server.start();
        System.out.println("Server started successfully on port " + port);

        // Add a shutdown hook to gracefully stop the server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down the server...");
            server.shutdown();
            System.out.println("Server shut down.");
        }));

        // Wait for the server to terminate (blocking)
        server.awaitTermination();
    }
}
