package com.group6.logsystem.tests;

import com.group6.logsystem.timesync.LogicalClock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TimeSyncTestLogicalClockReceive {

    @Test
    public void testLogicalClockReceive() {
        LogicalClock clock = new LogicalClock();
        long initialTime = clock.getTime();

        // Simulate receiving a log with a higher timestamp
        long receivedTimestamp = initialTime + 5000;
        long updatedTime = clock.receive(receivedTimestamp);

        assertTrue(updatedTime > initialTime, "Clock should advance when receiving a higher timestamp.");
    }
}
