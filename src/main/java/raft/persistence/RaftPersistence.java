package raft.persistence;

import raft.model.LogEntryPOJO;
import raft.model.RaftState;

import java.util.List;

public interface RaftPersistence {
    void saveLog(List<LogEntryPOJO> log) throws Exception;
    List<LogEntryPOJO> loadLog() throws Exception;
    void saveState(RaftState state) throws Exception;
    RaftState loadState() throws Exception;
}