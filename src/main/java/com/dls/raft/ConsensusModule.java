package com.dls.raft;

import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;
import com.dls.common.NodeInfo;
import com.dls.raft.rpc.RaftGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.List;

public class ConsensusModule {

    private final RaftNode raftNode;

    public ConsensusModule(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    private void sendHeartbeat(NodeInfo peer, AppendEntriesRequest request) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(peer.getHost(), peer.getPort())
                .usePlaintext()
                .build();

        RaftGrpc.RaftBlockingStub stub = RaftGrpc.newBlockingStub(channel);

        try {
            AppendEntriesResponse response = stub.appendEntries(request);
            // Optionally handle response (for now, heartbeats usually don't care)
        } catch (StatusRuntimeException e) {
            System.err.println("Heartbeat RPC failed to " + peer.getNodeId() + ": " + e.getStatus());
        } finally {
            channel.shutdown();
        }
    }

    public void broadcastHeartbeats() {
        List<NodeInfo> peers = raftNode.getPeers();

        for (NodeInfo peer : peers) {
            if (peer.getNodeId().equals(raftNode.getSelf().getNodeId())) {
                continue; // Don't send heartbeat to self
            }

            AppendEntriesRequest heartbeat = AppendEntriesRequest.newBuilder()
                    .setLeaderId(raftNode.getSelf().getNodeId())
                    .setTerm(raftNode.getCurrentTerm())
                    .setPrevLogIndex(raftNode.getRaftLog().getLastLogIndex())
                    .setPrevLogTerm(raftNode.getRaftLog().getLastLogTerm())
                    .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                    .build();

            sendHeartbeat(peer, heartbeat);
        }
    }

    public AppendEntriesResponse processAppendEntries(AppendEntriesRequest request) {
        boolean success = false;

        if (request.getTerm() < raftNode.getCurrentTerm()) {
            success = false;
        } else {
            if (request.getTerm() > raftNode.getCurrentTerm()) {
                raftNode.becomeFollower(request.getTerm());
            }

            // Reset election timer even if log matching fails
            raftNode.resetElectionTimer();

            if (raftNode.getRaftLog().matchLog(request.getPrevLogIndex(), request.getPrevLogTerm())) {
                success = true;

                // TODO: Apply entries if there are any
                // raftNode.getRaftLog().appendEntries(LocalLogEntry.fromGrpcLogEntryList(request.getEntriesList()));

                raftNode.getRaftLog().setCommitIndex(
                        Math.min(request.getLeaderCommit(), raftNode.getRaftLog().getLastLogIndex())
                );
            }
        }

        return AppendEntriesResponse.newBuilder()
                .setTerm(raftNode.getCurrentTerm())
                .setSuccess(success)
                .build();
    }


}