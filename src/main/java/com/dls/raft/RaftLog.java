package com.dls.raft;

import com.dls.common.LocalLogEntry;
import com.dls.raft.rpc.LogEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaftLog {

    private final List<LogEntry> logEntries;
    private int commitIndex;
    //private final Map<String, Integer> nextIndex = new HashMap<>();  // Maps peerId to nextIndex
   // private final Map<String, Integer> matchIndex = new HashMap<>(); // Maps peerId to matchIndex


    public RaftLog() {
        this.logEntries = new ArrayList<>();
        this.commitIndex = -1; // Start with -1 meaning "no committed entries yet"
    }

    public synchronized int getLastLogIndex() {
        return logEntries.size() - 1;
    }

    public synchronized int getLastLogTerm() {
        if (logEntries.isEmpty()) {
            return 0;
        }
        return logEntries.get(logEntries.size() - 1).getTerm();
    }

    public synchronized boolean matchLog(int prevLogIndex, int prevLogTerm) {
        if (prevLogIndex < 0) {
            return true;  // No previous log means it's a match
        }
        if (prevLogIndex >= logEntries.size()) {
            return false;  // Out of bounds
        }
        return logEntries.get(prevLogIndex).getTerm() == prevLogTerm;
    }

    public synchronized void appendEntries(List<LocalLogEntry> entries, int term) {
        for (LocalLogEntry entry : entries) {
            int newIndex = logEntries.size();
            entry.setIndex(newIndex);
            entry.setTerm(term);
            LogEntry grpcEntry = entry.toGrpcLogEntry();
            logEntries.add(grpcEntry);
        }
    }



    public synchronized void setCommitIndex(int leaderCommit) {
        int newCommitIndex = Math.min(leaderCommit, getLastLogIndex());
        if (newCommitIndex > commitIndex) {
            commitIndex = newCommitIndex;
        }
    }

    public synchronized void appendEntry(LocalLogEntry entry, int term) {
        int newIndex = logEntries.size();  // 0-based index
        entry.setIndex(newIndex);
        entry.setTerm(term);
        LogEntry grpcEntry = entry.toGrpcLogEntry();
        logEntries.add(grpcEntry);
    }




    public synchronized int getCommitIndex() {
        return commitIndex;
    }

    public synchronized int getNextLogIndex() {
        return logEntries.size();
    }


    public synchronized boolean isUpToDate(int lastLogIndex, int lastLogTerm) {
        int myLastTerm = getLastLogTerm();
        if (lastLogTerm != myLastTerm) {
            return lastLogTerm > myLastTerm;
        }
        return lastLogIndex >= getLastLogIndex();
    }

    public synchronized List<LogEntry> getUncommittedEntries() {
        int start = commitIndex + 1;
        if (start < 0) {
            start = 0;
        }
        return new ArrayList<>(logEntries.subList(start, logEntries.size()));
    }

    public synchronized List<LogEntry> getEntriesSince(int fromIndex) {
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (fromIndex >= logEntries.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(logEntries.subList(fromIndex, logEntries.size()));
    }

    public synchronized List<LogEntry> getLogEntries() {
        return new ArrayList<>(logEntries);
    }
}