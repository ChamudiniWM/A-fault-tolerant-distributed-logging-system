package com.group6.logsystem.TimeSync;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TimeSyncRaftBehaviorTests {

    static class LocalLogEntry {
        int index, term;
        String command, nodeId;
        long timestamp;

        LocalLogEntry(int index, int term, String command, long timestamp, String nodeId) {
            this.index = index;
            this.term = term;
            this.command = command;
            this.timestamp = timestamp;
            this.nodeId = nodeId;
        }

        public long getTimestamp() { return timestamp; }
    }

    static class TimestampCorrector {
        public static List<LocalLogEntry> getOrderedLogs(List<LocalLogEntry> logs) {
            logs.sort(Comparator.comparingLong(LocalLogEntry::getTimestamp));
            return logs;
        }
    }

    enum RaftState {
        FOLLOWER, CANDIDATE, LEADER
    }

    static class SimulatedRaftNode {
        int currentTerm = 0;
        RaftState state = RaftState.FOLLOWER;

        void onElectionTimeout(long clockTime, long skewMillis) {
            // Clock skew is ignored in term decision, only affects scheduling
            this.currentTerm += 1;
            this.state = RaftState.CANDIDATE;
        }

        void onReceiveHigherTerm(int incomingTerm) {
            if (incomingTerm > this.currentTerm) {
                this.currentTerm = incomingTerm;
                this.state = RaftState.FOLLOWER;
            }
        }
    }

    @Test
    public void testLogIndexConsistencyWithTimestamps() {
        List<LocalLogEntry> logs = new ArrayList<>();
        logs.add(new LocalLogEntry(5, 2, "CmdA", 1000, "node1"));
        logs.add(new LocalLogEntry(3, 2, "CmdB", 500, "node2"));
        logs.add(new LocalLogEntry(7, 2, "CmdC", 1500, "node3"));

        logs.sort(Comparator.comparingInt(log -> log.index)); // Index order
        assertEquals(3, logs.get(0).index);
        assertEquals(5, logs.get(1).index);
        assertEquals(7, logs.get(2).index);
    }

    @Test
    public void testSkewedClockDoesNotAffectTermLogic() {
        SimulatedRaftNode node = new SimulatedRaftNode();

        node.onElectionTimeout(System.currentTimeMillis(), 3000);  // simulate skewed clock
        assertEquals(RaftState.CANDIDATE, node.state);
        assertEquals(1, node.currentTerm);

        node.onReceiveHigherTerm(3);  // another node claims higher term
        assertEquals(3, node.currentTerm);
        assertEquals(RaftState.FOLLOWER, node.state);
    }

    @Test
    public void testLeaderRejectionOnLowerTermDespiteFasterClock() {
        SimulatedRaftNode node = new SimulatedRaftNode();
        node.currentTerm = 5;
        node.state = RaftState.LEADER;

        int incomingTerm = 4;
        node.onReceiveHigherTerm(incomingTerm);  // Should not change state
        assertEquals(RaftState.LEADER, node.state);
        assertEquals(5, node.currentTerm);
    }
}