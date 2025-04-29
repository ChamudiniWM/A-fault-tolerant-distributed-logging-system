package com.group6.logsystem.tests;

import com.group6.logsystem.grpc.LogEntry;
import com.group6.logsystem.timesync.LogicalClock;
import com.group6.logsystem.timesync.LogTimestampCorrector;
import org.junit.jupiter.api.Test;

public class TimeSyncTestDuplicateTimestamps {

    @Test
    public void testReorderingWithDuplicateTimestamps() {
        LogicalClock clock = new LogicalClock();
        LogTimestampCorrector corrector = new LogTimestampCorrector();

        long timestamp = clock.tick();

        LogEntry log1 = LogEntry.newBuilder()
                .setNodeId("Node-A")
                .setMessage("First event with duplicate timestamp")
                .setTimestamp(timestamp)
                .setTerm(1)
                .build();

        LogEntry log2 = LogEntry.newBuilder()
                .setNodeId("Node-B")
                .setMessage("Second event with duplicate timestamp")
                .setTimestamp(timestamp)
                .setTerm(1)
                .build();

        corrector.bufferLog(log2);
        corrector.bufferLog(log1);

        System.out.println("\n Output with duplicate timestamps:");
        corrector.flushLogsInOrder();
    }
}
