//package com.group6.logsystem.Consensus;
//
//import com.dls.common.NodeInfo;
//import com.dls.raft.rpc.LogMessage;
//import com.dls.raft.rpc.LoggingServiceGrpc;
//import io.grpc.ManagedChannel;
//import io.grpc.ManagedChannelBuilder;
//
//public class TestNetworkPartitionLeaderToFollower {
//
//    public static void main(String[] args) throws InterruptedException {
//        // Step 1: Launch nodes manually using MultiNodeServerMain
//        // Run MultiNodeServerMain before executing this test
//
//        System.out.println("[Test] Simulating network partition...");
//
//        // Step 2: Block network messages from leader to follower
//        // This is a placeholder – actual simulation depends on your network mock/logic.
//        // For now, we simulate by not sending logs to the follower and waiting to observe.
//
//        Thread.sleep(5000);  // Give time for leader election and heartbeats
//
//        // Step 3: Simulate leader log append
//        ManagedChannel leaderChannel = ManagedChannelBuilder.forAddress("localhost", 50051)
//                .usePlaintext().build();
//        LoggingServiceGrpc.LoggingServiceBlockingStub stub = LoggingServiceGrpc.newBlockingStub(leaderChannel);
//
//        stub.log(LogMessage.newBuilder()
//                .setNodeId("node1")
//                .setMessage("✉️ Leader appending entry under network partition from node2")
//                .build());
//
//        // Step 4: Log that node2 is isolated (network partitioned)
//        ManagedChannel followerChannel = ManagedChannelBuilder.forAddress("localhost", 50052)
//                .usePlaintext().build();
//        LoggingServiceGrpc.LoggingServiceBlockingStub followerStub = LoggingServiceGrpc.newBlockingStub(followerChannel);
//
//        followerStub.log(LogMessage.newBuilder()
//                .setNodeId("node2")
//                .setMessage("🚫 Simulating that node2 is partitioned from leader (not receiving messages)")
//                .build());
//
//        Thread.sleep(10000);  // Wait and observe logs from LoggingServer
//
//        leaderChannel.shutdownNow();
//        followerChannel.shutdownNow();
//
//        System.out.println("[Test] Network partition test complete. Check LoggingServer output.");
//    }
//}
