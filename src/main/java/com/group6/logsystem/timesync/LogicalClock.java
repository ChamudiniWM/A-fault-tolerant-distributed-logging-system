package com.group6.logsystem.timesync;

public class LogicalClock {
    private long clock;

    public LogicalClock() {
        clock = System.currentTimeMillis();
    }

    public synchronized long tick() {
        return ++clock;
    }

    public long receive(long receivedTimestamp) {
        clock = Math.max(clock, receivedTimestamp) + 1;
        return clock;
    }

    public synchronized long update(long receivedTimestamp) {
        clock = Math.max(clock, receivedTimestamp) + 1;
        return clock;
    }

    public synchronized long getTime() {
        return clock;
    }
}
