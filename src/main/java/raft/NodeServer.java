package raft;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import raft.core.RaftNode;
import raft.grpc.ClientServiceImpl;
import raft.grpc.LogServiceImpl;
import raft.persistence.JsonRaftPersistence;
import raft.persistence.RaftPersistence;
import util.TimeSyncUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NodeServer {
    private final int port;
    private final Server server;
    private final RaftNode raftNode;
    private final RaftPersistence persistence;

    public NodeServer(int port, List<String> peers) {
        this.port = port;
        this.persistence = new JsonRaftPersistence("raft_log_" + port + ".json", "raft_state_" + port + ".json");
        this.raftNode = new RaftNode(port, peers, persistence);
        this.server = ServerBuilder.forPort(port)
                .addService(new LogServiceImpl(raftNode))
                .addService(new ClientServiceImpl(raftNode))
                .build();
    }

    public void start() throws IOException {
        TimeSyncUtil.syncNtpOffset();
        server.start();
        raftNode.start();

        System.out.println("\n=== Server Status ===");
        System.out.println("Server started on port: " + port);
        System.out.println("Node ID: localhost:" + port);
        System.out.println("Initial Role: " + raftNode.getRole() + "\n");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Server Shutdown ===");
            System.out.println("Shutting down server...\n");
            stop();
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
        raftNode.stop();
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051;  // Default port
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0] + ". Using default port 50051.");
            }
        }


        List<String> peers = switch (port) {
            case 50051 -> List.of("localhost:50052", "localhost:50053");
            case 50052 -> List.of("localhost:50051", "localhost:50053");
            case 50053 -> List.of("localhost:50051", "localhost:50052");
            default -> new ArrayList<>();
        };

        NodeServer server = new NodeServer(port, peers);
        server.start();
        server.blockUntilShutdown();
    }
}