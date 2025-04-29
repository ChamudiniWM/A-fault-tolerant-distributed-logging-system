package com.group6.logsystem.tests;

import com.group6.logsystem.timesync.LogTimestampCorrector;
import org.junit.jupiter.api.Test;

public class TimeSyncTestFlushEmptyBuffer {

    @Test
    public void testFlushEmptyLogBuffer() {
        LogTimestampCorrector corrector = new LogTimestampCorrector();

        // Should not throw any exceptions
        corrector.flushLogsInOrder();

        System.out.println("\n Flushed empty buffer successfully (no logs).");
    }
}
