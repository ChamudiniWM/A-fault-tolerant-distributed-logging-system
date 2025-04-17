package com.group6.logsystem.tests;

import com.group6.logsystem.grpc.LogEntry;
import com.group6.logsystem.timesync.LogicalClock;
import com.group6.logsystem.timesync.LogTimestampCorrector;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class TimeSyncTest {
    @Test
    public void testLogReorderingWithTimestamps() {
        LogicalClock clock = new LogicalClock();
        LogTimestampCorrector corrector = new LogTimestampCorrector();

        // Simulate out-of-order logs
        LogEntry log1 = LogEntry.newBuilder()
                .setNodeId("Node-1")
                .setMessage("First Event (late)")
                .setTimestamp(clock.update(3000))
                .setTerm(1)
                .build();

        LogEntry log2 = LogEntry.newBuilder()
                .setNodeId("Node-2")
                .setMessage("Second Event (early)")
                .setTimestamp(clock.update(1000))
                .setTerm(1)
                .build();

        LogEntry log3 = LogEntry.newBuilder()
                .setNodeId("Node-3")
                .setMessage("Third Event (normal)")
                .setTimestamp(clock.update(2000))
                .setTerm(1)
                .build();

        corrector.bufferLog(log1);
        corrector.bufferLog(log2);
        corrector.bufferLog(log3);

        System.out.println("\n✅ Ordered Output (Expected: 1000 → 2000 → 3000):");
        corrector.flushLogsInOrder();
    }
}