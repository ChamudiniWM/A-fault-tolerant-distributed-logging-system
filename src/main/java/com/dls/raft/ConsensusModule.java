package com.dls.raft;

import com.dls.common.LocalLogEntry;
import com.dls.common.PerformanceLogger;
import com.dls.common.RejectionTracker;
import com.dls.loadtest.PerformanceMetrics;
import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;
import com.dls.common.NodeInfo;
import com.dls.raft.rpc.LogEntry;
import com.dls.raft.rpc.RaftGrpc;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConsensusModule {

    private final RaftNode raftNode;

    public ConsensusModule(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    public void broadcastHeartbeats() {
        List<NodeInfo> peers = raftNode.getPeers();
        Map<NodeInfo, RaftGrpc.RaftStub> stubs = raftNode.getPeerStubs();  // Cached async stubs

        for (NodeInfo peer : peers) {
            if (peer.getNodeId().equals(raftNode.getSelf().getNodeId())) continue;

            // 🧠 Batch recent log entries (example: last 5)
            List<LogEntry> recentEntries = raftNode.getRaftLog()
                    .getEntriesSince(raftNode.getRaftLog().getLastLogIndex() - 4); // last 5 entries

            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .setLeaderId(raftNode.getSelf().getNodeId())
                    .setTerm(raftNode.getCurrentTerm())
                    .setPrevLogIndex(raftNode.getRaftLog().getLastLogIndex())
                    .setPrevLogTerm(raftNode.getRaftLog().getLastLogTerm())
                    .addAllEntries(recentEntries)  // Send batched entries
                    .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                    .build();

            // Use async stub with a fire-and-forget observer
            stubs.get(peer).appendEntries(request, new StreamObserver<>() {
                @Override
                public void onNext(AppendEntriesResponse response) {
                    if (!response.getSuccess()) {
                        System.out.println("AppendEntries rejected by " + peer.getNodeId());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("AppendEntries RPC to " + peer.getNodeId() + " failed: " + t.getMessage());
                }

                @Override
                public void onCompleted() {}
            });
        }
    }

    public void handleNewLogEntry(LogEntry entry) {
        // Convert gRPC LogEntry to LocalLogEntry
        LocalLogEntry localEntry = LocalLogEntry.fromGrpcLogEntry(entry);

        // Append it to the local Raft log
        raftNode.getRaftLog().appendEntry(localEntry);

        System.out.println("New log entry appended to Raft log: " + localEntry.getCommand());
    }


    public AppendEntriesResponse processAppendEntries(AppendEntriesRequest request) {
        long startTime = System.nanoTime();
        boolean success = false;

        if (request.getTerm() < raftNode.getCurrentTerm()) {
            success = false;
            RejectionTracker.increment();
        } else {
            if (request.getTerm() > raftNode.getCurrentTerm()) {
                raftNode.becomeFollower(request.getTerm());
            }

            raftNode.resetElectionTimer();

            if (!request.getEntriesList().isEmpty()) {
                if (raftNode.getRaftLog().matchLog(request.getPrevLogIndex(), request.getPrevLogTerm())) {
                    success = true;

                    raftNode.getRaftLog().appendEntries(
                            LocalLogEntry.fromGrpcLogEntryList(request.getEntriesList())
                    );

                    raftNode.getRaftLog().setCommitIndex(
                            Math.min(request.getLeaderCommit(), raftNode.getRaftLog().getLastLogIndex())
                    );

                    long endTime = System.nanoTime();
                    long latencyMicros = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
                    PerformanceLogger.logLatency(latencyMicros);
                    System.out.println("[Performance] AppendEntries processed. Latency: " + latencyMicros + " µs");
                }
            } else {
                RejectionTracker.increment();
                success = false;
            }
        }

        return AppendEntriesResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setSuccess(success)
                .build();
    }
}