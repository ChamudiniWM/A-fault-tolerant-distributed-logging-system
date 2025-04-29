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
        Map<NodeInfo, RaftGrpc.RaftStub> stubs = raftNode.getPeerStubs();

        for (NodeInfo peer : peers) {
            if (peer.getNodeId().equals(raftNode.getSelf().getNodeId())) continue;

            String peerId = peer.getNodeId();
            int nextIndex = raftNode.getNextIndexFor(peerId);  // Follower's next expected log index
            int prevLogIndex = nextIndex - 1;
            int prevLogTerm = raftNode.getTermAtIndex(prevLogIndex);

            List<LogEntry> entriesToSend = raftNode.getRaftLog().getEntriesSince(nextIndex);  // Entries from nextIndex onward

            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .setLeaderId(raftNode.getSelf().getNodeId())
                    .setTerm(raftNode.getCurrentTerm())
                    .setPrevLogIndex(prevLogIndex)
                    .setPrevLogTerm(prevLogTerm)
                    .addAllEntries(entriesToSend)
                    .setLeaderCommit(raftNode.getCommitIndex())
                    .build();

            stubs.get(peer).appendEntries(request, new StreamObserver<>() {
                @Override
                public void onNext(AppendEntriesResponse response) {
                    if (response.getSuccess()) {
                        System.out.println("AppendEntries succeeded for " + peerId);
                        int newMatchIndex = nextIndex + entriesToSend.size() - 1;
                        raftNode.advanceMatchIndex(peerId, newMatchIndex);
                        raftNode.nextIndex.put(peerId, newMatchIndex + 1);
                    } else {
                        System.out.println("AppendEntries rejected by " + peerId);
                        raftNode.decrementNextIndex(peerId);  // Back off
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("AppendEntries RPC to " + peerId + " failed: " + t.getMessage());
                }

                @Override
                public void onCompleted() {}
            });
        }
    }


    public void handleNewLogEntry(LogEntry entry) {
        LocalLogEntry localEntry = LocalLogEntry.fromGrpcLogEntry(entry);
        raftNode.getRaftLog().appendEntry(localEntry, raftNode.getCurrentTerm());
        System.out.println("New log entry appended to Raft log: " + localEntry.getCommand());
    }

    public void broadcastAppendEntries() {
        List<NodeInfo> peers = raftNode.getPeers();
        Map<NodeInfo, RaftGrpc.RaftStub> stubs = raftNode.getPeerStubs();

        for (NodeInfo peer : peers) {
            if (peer.getNodeId().equals(raftNode.getSelf().getNodeId())) continue;

            int nextIndex = raftNode.getNextIndexFor(peer.getNodeId());
            int prevLogIndex = nextIndex - 1;
            int prevLogTerm = raftNode.getTermAtIndex(prevLogIndex);

            List<LogEntry> entriesToSend = raftNode.getRaftLog().getEntriesSince(prevLogIndex);

            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .setLeaderId(raftNode.getSelf().getNodeId())
                    .setTerm(raftNode.getCurrentTerm())
                    .setPrevLogIndex(prevLogIndex)
                    .setPrevLogTerm(prevLogTerm)
                    .addAllEntries(entriesToSend)
                    .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                    .build();

            stubs.get(peer).appendEntries(request, new StreamObserver<>() {
                @Override
                public void onNext(AppendEntriesResponse response) {
                    if (response.getSuccess()) {
                        // Update matchIndex for the peer
                        raftNode.advanceMatchIndex(peer.getNodeId(), nextIndex + entriesToSend.size() - 1);
                        System.out.println("✅ Log replicated to " + peer.getNodeId());
                    } else {
                        // Decrement nextIndex for the peer
                        raftNode.decrementNextIndex(peer.getNodeId());
                        System.out.println("❌ Log replication failed for " + peer.getNodeId() + ", will retry");
                    }
                }

                @Override
                public void onError(Throwable t) {
                    System.err.println("❗ AppendEntries RPC to " + peer.getNodeId() + " failed: " + t.getMessage());
                }

                @Override
                public void onCompleted() {}
            });
        }
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

            if (request.getEntriesList().isEmpty()) {
                // Heartbeat
                success = raftNode.getRaftLog().matchLog(request.getPrevLogIndex(), request.getPrevLogTerm());
            } else {
                // Real log replication
                if (raftNode.getRaftLog().matchLog(request.getPrevLogIndex(), request.getPrevLogTerm())) {
                    success = true;

                    raftNode.getRaftLog().appendEntries(
                            LocalLogEntry.fromGrpcLogEntryList(request.getEntriesList()),
                            request.getTerm()
                    );

                    raftNode.getRaftLog().setCommitIndex(
                            Math.min(request.getLeaderCommit(), raftNode.getRaftLog().getLastLogIndex())
                    );

                    long endTime = System.nanoTime();
                    long latencyMicros = TimeUnit.NANOSECONDS.toMicros(endTime - startTime);
                    PerformanceLogger.logLatency(latencyMicros);
                    System.out.println("[Performance] AppendEntries processed. Latency: " + latencyMicros + " µs");
                } else {
                    success = false;
                    RejectionTracker.increment();
                }
            }
        }

        return AppendEntriesResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setSuccess(success)
                .build();
    }

}