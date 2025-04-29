package com.dls.replication;

import com.dls.common.LocalLogEntry;
import com.dls.timesync.TimestampCorrector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LogRetriever {

    private final TimestampCorrector timestampCorrector;

    public LogRetriever(TimestampCorrector timestampCorrector) {
        this.timestampCorrector = timestampCorrector;
    }

    /**
     * Correct timestamps of logs and reorder them.
     *
     * @param rawEntries List of raw received log entries.
     * @return Corrected and sorted list of log entries.
     */
    public List<LocalLogEntry> correctAndOrderLogs(List<LocalLogEntry> rawEntries) {
        List<LocalLogEntry> correctedEntries = new ArrayList<>();

        for (LocalLogEntry entry : rawEntries) {
            long correctedTimestamp = timestampCorrector.correct(entry.getTimestamp());

            LocalLogEntry correctedEntry = new LocalLogEntry(
                    entry.getIndex(),
                    entry.getTerm(),
                    entry.getCommand(),
                    correctedTimestamp,
                    entry.getId()
            );

            correctedEntry.setData(entry.getData());
            correctedEntry.setMetadata(entry.getMetadata());

            correctedEntries.add(correctedEntry);
        }

        correctedEntries.sort(Comparator.comparingLong(LocalLogEntry::getTimestamp));
        return correctedEntries;
    }
}