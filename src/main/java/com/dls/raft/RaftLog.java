package com.dls.raft;

import com.dls.common.LocalLogEntry;
import com.dls.raft.rpc.LogEntry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaftLog {

    private final List<LogEntry> logEntries;
    private int commitIndex;
    private final String nodeId;
    private final Path logFile;
    private static final String LOG_DIR = "raft_logs";
    //private final Map<String, Integer> nextIndex = new HashMap<>();  // Maps peerId to nextIndex
   // private final Map<String, Integer> matchIndex = new HashMap<>(); // Maps peerId to matchIndex


    public RaftLog(String nodeId) {
        this.nodeId = nodeId;
        this.logEntries = new ArrayList<>();
        this.commitIndex = -1; // Start with -1 meaning "no committed entries yet"
        // Create logs directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create logs directory: " + e.getMessage());
        }
        this.logFile = Paths.get(LOG_DIR, "raft_log_" + nodeId + ".dat");
        loadFromDisk();

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
        saveToDisk();
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
        saveToDisk();
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

    private synchronized void loadFromDisk() {
        if (!Files.exists(logFile)) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(logFile.toFile()))) {

            // Read commit index
            commitIndex = ois.readInt();

            // Read log entries
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                LogEntry entry = (LogEntry) ois.readObject();
                logEntries.add(entry);
            }

            System.out.println("Loaded " + size + " log entries from disk for node " + nodeId);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading log from disk: " + e.getMessage());
        }
    }

    private synchronized void saveToDisk() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(logFile.toFile()))) {
            out.writeInt(commitIndex);
            out.writeInt(logEntries.size());
            for (LogEntry entry : logEntries) {
                out.writeObject(entry);
            }
            out.flush();
            System.out.println("Saved " + logEntries.size() + " log entries to disk for node " + nodeId + ". Last commit index: " + commitIndex);
        } catch (IOException e) {
            System.err.println("Failed to save log to disk: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized List<LogEntry> getLogEntries() {
        return new ArrayList<>(logEntries);
    }
}