package raft.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RaftState {
    @JsonProperty("currentTerm")
    private int currentTerm;

    @JsonProperty("votedFor")
    private String votedFor;

    @JsonProperty("commitIndex")
    private int commitIndex;

    @JsonProperty("lastApplied")
    private int lastApplied;

    @JsonProperty("seenLogIds")
    private List<String> seenLogIds;

    // Default constructor for Jackson
    public RaftState() {
    }

    public RaftState(int currentTerm, String votedFor, int commitIndex, int lastApplied, List<String> seenLogIds) {
        this.currentTerm = currentTerm;
        this.votedFor = votedFor;
        this.commitIndex = commitIndex;
        this.lastApplied = lastApplied;
        this.seenLogIds = seenLogIds;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public void setCurrentTerm(int currentTerm) {
        this.currentTerm = currentTerm;
    }

    public String getVotedFor() {
        return votedFor;
    }

    public void setVotedFor(String votedFor) {
        this.votedFor = votedFor;
    }

    public int getCommitIndex() {
        return commitIndex;
    }

    public void setCommitIndex(int commitIndex) {
        this.commitIndex = commitIndex;
    }

    public int getLastApplied() {
        return lastApplied;
    }

    public void setLastApplied(int lastApplied) {
        this.lastApplied = lastApplied;
    }

    public List<String> getSeenLogIds() {
        return seenLogIds;
    }

    public void setSeenLogIds(List<String> seenLogIds) {
        this.seenLogIds = seenLogIds;
    }
}
