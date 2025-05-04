package raft.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import raft.model.LogEntryPOJO;
import raft.model.RaftState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonRaftPersistence implements RaftPersistence {
    private final ObjectMapper mapper = new ObjectMapper();
    private final File logFile;
    private final File stateFile;

    public JsonRaftPersistence(String logFilePath, String stateFilePath) {
        this.logFile = new File(logFilePath);
        this.stateFile = new File(stateFilePath);
    }

    @Override
    public void saveLog(List<LogEntryPOJO> log) throws Exception {
        mapper.writeValue(logFile, log);
    }

    @Override
    public List<LogEntryPOJO> loadLog() throws Exception {
        if (logFile.exists()) {
            return mapper.readValue(logFile, mapper.getTypeFactory().constructCollectionType(List.class, LogEntryPOJO.class));
        }
        return new ArrayList<>();
    }

    @Override
    public void saveState(RaftState state) throws Exception {
        mapper.writeValue(stateFile, state);
    }

    @Override
    public RaftState loadState() throws Exception {
        if (stateFile.exists()) {
            return mapper.readValue(stateFile, RaftState.class);
        }
        return null;
    }
}