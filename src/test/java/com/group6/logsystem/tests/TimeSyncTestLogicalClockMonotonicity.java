package com.group6.logsystem.tests;

import com.group6.logsystem.timesync.LogicalClock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TimeSyncTestLogicalClockMonotonicity {

    @Test
    public void testLogicalClockMonotonicity() {
        LogicalClock clock = new LogicalClock();

        long t1 = clock.tick();
        long t2 = clock.tick();
        long t3 = clock.tick();

        assertTrue(t1 < t2, "Clock should increase monotonically: t1 < t2");
        assertTrue(t2 < t3, "Clock should increase monotonically: t2 < t3");
    }
}

