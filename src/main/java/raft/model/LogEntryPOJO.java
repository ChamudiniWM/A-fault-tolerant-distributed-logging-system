package raft.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import logging.LogEntry;

public class LogEntryPOJO {
    @JsonProperty("term")
    private int term;

    @JsonProperty("index")
    private int index;

    @JsonProperty("command")
    private String command;

    @JsonProperty("logId")
    private String logId;

    @JsonProperty("timestamp")
    private long timestamp;

    // Default constructor for Jackson
    public LogEntryPOJO() {
    }

    // Parameterized constructor
    public LogEntryPOJO(int term, int index, String command, String logId, long timestamp) {
        this.term = term;
        this.index = index;
        this.command = command;
        this.logId = logId;
        this.timestamp = timestamp;
    }

    // Convert from Protobuf LogEntry
    public static LogEntryPOJO fromProto(LogEntry proto) {
        return new LogEntryPOJO(
                proto.getTerm(),
                proto.getIndex(),
                proto.getCommand(),
                proto.getLogId(),
                proto.getTimestamp()
        );
    }

    // Convert to Protobuf LogEntry
    public LogEntry toProto() {
        return LogEntry.newBuilder()
                .setTerm(term)
                .setIndex(index)
                .setCommand(command)
                .setLogId(logId)
                .setTimestamp(timestamp)
                .build();
    }

    // Getters and setters for Jackson
    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}