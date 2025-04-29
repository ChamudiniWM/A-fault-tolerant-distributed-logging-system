package com.dls.replication;

import com.dls.common.LocalLogEntry;
import com.dls.common.NodeInfo;
import com.dls.raft.RaftNode;
import com.dls.raft.RaftState;
import com.dls.raft.rpc.AppendEntriesRequest;
import com.dls.raft.rpc.AppendEntriesResponse;
import com.dls.raft.rpc.LogEntry;
import com.dls.raft.rpc.RaftGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ConsistencyChecker {
    private final RaftNode raftNode;
    public final Map<String, List<LogEntry>> nodeLogs;
    private final Map<String, Long> lastConsistencyCheck;
    private volatile boolean isChecking;

    public ConsistencyChecker(RaftNode raftNode) {
        this.raftNode = raftNode;
        this.nodeLogs = new ConcurrentHashMap<>();
        this.lastConsistencyCheck = new ConcurrentHashMap<>();
        this.isChecking = false;
    }

    public void startConsistencyCheck() {
        if (raftNode.getState() != RaftState.LEADER) {
            System.out.println("Cannot start consistency check: not the leader");
            return;
        }

        if (isChecking) {
            System.out.println("Consistency check already in progress");
            return;
        }

        isChecking = true;
        try {
            for (NodeInfo peer : raftNode.getPeers()) {
                if (!peer.getNodeId().equals(raftNode.getSelf().getNodeId())) {
                    if (raftNode.getState() != RaftState.LEADER) {
                        System.out.println("Leadership lost during consistency check");
                        break;
                    }
                    getNodeLogs(peer);
                }
            }
        } finally {
            isChecking = false;
        }
    }

    private void getNodeLogs(NodeInfo node) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(node.getHost(), node.getPort())
                .usePlaintext()
                .build();

        try {
            RaftGrpc.RaftBlockingStub stub = RaftGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setLeaderId(raftNode.getSelf().getNodeId())
                    .setPrevLogIndex(0)
                    .setPrevLogTerm(0)
                    .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                    .build();

            AppendEntriesResponse response = stub.appendEntries(request);
            if (response.getSuccess()) {
                // Get the node's logs using a separate RPC call
                List<LogEntry> logs = raftNode.getRaftLog().getLogEntries();
                nodeLogs.put(node.getNodeId(), logs);
                lastConsistencyCheck.put(node.getNodeId(), System.currentTimeMillis());
            } else if (response.getTerm() > raftNode.getCurrentTerm()) {
                // Step down if we discover a higher term
                System.out.println("Higher term discovered during consistency check, stepping down");
                raftNode.becomeFollower(response.getTerm());
                return;
            }
        } catch (Exception e) {
            System.err.println("Error getting logs from node " + node.getNodeId() + ": " + e.getMessage());
        } finally {
            channel.shutdown();
        }
    }

    public List<LocalLogEntry> compareLogs(String nodeId) {
        List<LogEntry> peerLogs = nodeLogs.get(nodeId);
        if (peerLogs == null) {
            return new ArrayList<>();
        }

        List<LogEntry> localLogs = raftNode.getRaftLog().getLogEntries();
        List<LocalLogEntry> missingLogs = new ArrayList<>();

        int peerIndex = 0;
        for (LogEntry localEntry : localLogs) {
            if (peerIndex >= peerLogs.size() ||
                    localEntry.getIndex() != peerLogs.get(peerIndex).getIndex() ||
                    localEntry.getTerm() != peerLogs.get(peerIndex).getTerm()) {
                missingLogs.add(LocalLogEntry.fromGrpcLogEntry(localEntry));
            } else {
                peerIndex++;
            }
        }

        return missingLogs;
    }

    public boolean sendMissingLogs(String nodeId, List<LocalLogEntry> missingLogs) {
        if (raftNode.getState() != RaftState.LEADER) {
            System.out.println("Cannot send missing logs: not the leader");
            return false;
        }

        NodeInfo node = findNodeById(nodeId);
        if (node == null) {
            return false;
        }

        ManagedChannel channel = ManagedChannelBuilder.forAddress(node.getHost(), node.getPort())
                .usePlaintext()
                .build();

        try {
            RaftGrpc.RaftBlockingStub stub = RaftGrpc.newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            List<LogEntry> entries = new ArrayList<>();
            for (LocalLogEntry entry : missingLogs) {
                entries.add(entry.toGrpcLogEntry());
            }

            AppendEntriesRequest request = AppendEntriesRequest.newBuilder()
                    .setTerm(raftNode.getCurrentTerm())
                    .setLeaderId(raftNode.getSelf().getNodeId())
                    .setPrevLogIndex(missingLogs.get(0).getIndex() - 1)
                    .setPrevLogTerm(missingLogs.get(0).getTerm())
                    .setLeaderCommit(raftNode.getRaftLog().getCommitIndex())
                    .addAllEntries(entries)
                    .build();

            AppendEntriesResponse response = stub.appendEntries(request);
            if (response.getTerm() > raftNode.getCurrentTerm()) {
                // Step down if we discover a higher term
                System.out.println("Higher term discovered while sending missing logs, stepping down");
                raftNode.becomeFollower(response.getTerm());
                return false;
            }
            return response.getSuccess();
        } catch (Exception e) {
            System.err.println("Error sending missing logs to node " + nodeId + ": " + e.getMessage());
            return false;
        } finally {
            channel.shutdown();
        }
    }

    private NodeInfo findNodeById(String nodeId) {
        for (NodeInfo peer : raftNode.getPeers()) {
            if (peer.getNodeId().equals(nodeId)) {
                return peer;
            }
        }
        return null;
    }

    public Long getLastConsistencyCheck(String nodeId) {
        return lastConsistencyCheck.get(nodeId);
    }

    public List<LogEntry> getNodeLogs(String nodeId) {
        return nodeLogs.get(nodeId);
    }
}